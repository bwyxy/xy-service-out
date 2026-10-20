#!/usr/bin/env bash
# ============================================================================
# RTC 音视频通话 API 自动化测试
#
# 前置条件：
#   1. fg-chat 服务已启动（默认 http://localhost:9080，需包含 RTC 新代码）
#   2. 准备两个可正常登录的测试账号（互为好友非必须，通话不校验好友关系）
#
# 用法：
#   ACCOUNT_A=18600000001 PASSWORD_A=123456 \
#   ACCOUNT_B=18600000002 PASSWORD_B=123456 \
#   ./scripts/rtc-api-test.sh
#
# 可选环境变量：
#   BASE_URL   后端地址，默认 http://localhost:9080
#   GROUP_ID   测试群 ID（纯数字，如 9），设置后启用群通话用例
#   INVITE_UID 群内另一位成员 uid（可选），设置后在群通话用例中追加 invite
#   MYSQL_CMD  完整的 mysql 连接命令（含库名），设置后启用数据库状态断言，
#              例：MYSQL_CMD="mysql -h127.0.0.1 -uroot -p123456 xy_chat"
#
# 测试覆盖：
#   登录 / LiveKit 健康检查 / 呼叫自己 / 接听挂断 / 幂等重挂 / 拒绝 / 取消 /
#   忙线 / 重复呼叫 / 结束后重建房间 / 群通话（可选）/ 数据库状态流转（可选）
# ============================================================================
set -u

BASE_URL="${BASE_URL:-http://localhost:9080}"
ACCOUNT_A="${ACCOUNT_A:?请设置环境变量 ACCOUNT_A（主叫测试账号：手机号或邮箱）}"
PASSWORD_A="${PASSWORD_A:?请设置环境变量 PASSWORD_A}"
ACCOUNT_B="${ACCOUNT_B:?请设置环境变量 ACCOUNT_B（被叫测试账号：手机号或邮箱）}"
PASSWORD_B="${PASSWORD_B:?请设置环境变量 PASSWORD_B}"

PASS=0
FAIL=0

# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

# 从 JSON 中按点路径取值：json_get "$resp" data.roomId
json_get() {
  printf '%s' "$1" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
except Exception:
    print('')
    sys.exit(0)
for p in '$2'.split('.'):
    d = d.get(p) if isinstance(d, dict) else None
    if d is None:
        break
print(d if d is not None else '')
"
}

# JSON 数组长度：json_len "$resp"
json_len() {
  printf '%s' "$1" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
except Exception:
    print(0)
    sys.exit(0)
print(len(d) if isinstance(d, list) else 0)
"
}

# api METHOD PATH TOKEN [JSON_BODY]
api() {
  local method="$1" path="$2" token="$3" body="${4:-}"
  if [ -n "$body" ]; then
    curl -s -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" -H "token: $token" -d "$body"
  else
    curl -s -X "$method" "$BASE_URL$path" -H "token: $token"
  fi
}

# login ACCOUNT PASSWORD → 输出 token（全局变量 TOKEN/UID 由调用方捕获）
login() {
  curl -s -X POST "$BASE_URL/chat/login" \
    -H "Content-Type: application/json" \
    -d "{\"account\":\"$1\",\"password\":\"$2\"}"
}

section() { printf '\n\033[1m=== %s ===\033[0m\n' "$1"; }

check() { # check 描述 期望 实际
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1"
  else
    FAIL=$((FAIL + 1)); printf '  [FAIL] %s（期望: %s, 实际: %s）\n' "$1" "$2" "$3"
  fi
}

check_contains() { # check_contains 描述 子串 实际
  case "$3" in
    *"$2"*)
      PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1" ;;
    *)
      FAIL=$((FAIL + 1)); printf '  [FAIL] %s（期望包含: %s, 实际: %s）\n' "$1" "$2" "$3" ;;
  esac
}

check_not_200() { # check_not_200 描述 resp（业务码应非 200）
  local code
  code="$(json_get "$2" code)"
  if [ "$code" != "200" ]; then
    PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1"
  else
    FAIL=$((FAIL + 1)); printf '  [FAIL] %s（期望业务失败, 实际 code=%s）\n' "$1" "$code"
  fi
}

# ---------------------------------------------------------------------------
# 0. 登录
# ---------------------------------------------------------------------------
section "用例0：双账号登录"

RESP_A="$(login "$ACCOUNT_A" "$PASSWORD_A")"
TOKEN_A="$(json_get "$RESP_A" data.token)"
UID_A="$(json_get "$RESP_A" data.uid)"
check "主叫 A 登录成功" "200" "$(json_get "$RESP_A" code)"
if [ -z "$TOKEN_A" ] || [ "$TOKEN_A" = "None" ]; then
  echo "主叫 A 登录失败: $RESP_A"; exit 1
fi

RESP_B="$(login "$ACCOUNT_B" "$PASSWORD_B")"
TOKEN_B="$(json_get "$RESP_B" data.token)"
UID_B="$(json_get "$RESP_B" data.uid)"
check "被叫 B 登录成功" "200" "$(json_get "$RESP_B" code)"
if [ -z "$TOKEN_B" ] || [ "$TOKEN_B" = "None" ]; then
  echo "被叫 B 登录失败: $RESP_B"; exit 1
fi

echo "  UID_A=$UID_A  UID_B=$UID_B"

# ---------------------------------------------------------------------------
# 1. 通话服务健康检查（媒体面 LiveKit 连通性）
# ---------------------------------------------------------------------------
section "用例1：通话服务健康检查"

RESP="$(api GET /rtc/health "$TOKEN_A")"
check "GET /rtc/health 返回 200" "200" "$(json_get "$RESP" code)"
check "LiveKit 连通性为 up" "up" "$(json_get "$RESP" data.livekit)"

# ---------------------------------------------------------------------------
# 2. 参数与状态校验：呼叫自己
# ---------------------------------------------------------------------------
section "用例2：呼叫自己（参数校验）"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_A, \"mediaType\": 1}")"
check_not_200 "呼叫自己应被拒绝" "$RESP"
check_contains "错误提示为[不能呼叫自己]" "不能呼叫自己" "$(json_get "$RESP" msg)"

# ---------------------------------------------------------------------------
# 3. 正常通话流程：呼叫 → 接听 → 挂断 → 幂等
# ---------------------------------------------------------------------------
section "用例3：正常通话流程（接听→挂断→幂等）"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 2}")"
check "A 呼叫 B（视频）成功" "200" "$(json_get "$RESP" code)"
ROOM1="$(json_get "$RESP" data.roomId)"
check "返回 roomId 非空" "yes" "$([ -n "$ROOM1" ] && [ "$ROOM1" != "None" ] && echo yes || echo no)"
check "返回 livekitUrl 非空" "yes" "$([ -n "$(json_get "$RESP" data.livekitUrl)" ] && echo yes || echo no)"
check "返回接入 token 非空" "yes" "$([ -n "$(json_get "$RESP" data.token)" ] && echo yes || echo no)"
echo "  roomId=$ROOM1"

RESP="$(api POST /rtc/accept "$TOKEN_B" "{\"roomId\": \"$ROOM1\"}")"
check "B 接听成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/hangup "$TOKEN_B" "{\"roomId\": \"$ROOM1\"}")"
check "B 挂断成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/hangup "$TOKEN_A" "{\"roomId\": \"$ROOM1\"}")"
check "A 对已结束房间重复挂断（幂等）" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/accept "$TOKEN_B" "{\"roomId\": \"$ROOM1\"}")"
check "对已结束房间重复接听（幂等无错）" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/accept "$TOKEN_B" "{\"roomId\": \"not-exist-room-id\"}")"
check "接听不存在的房间（幂等无错）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 4. 拒绝流程
# ---------------------------------------------------------------------------
section "用例4：拒绝流程"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 1}")"
ROOM2="$(json_get "$RESP" data.roomId)"
check "A 呼叫 B（语音）成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/reject "$TOKEN_B" "{\"roomId\": \"$ROOM2\"}")"
check "B 拒绝成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/reject "$TOKEN_B" "{\"roomId\": \"$ROOM2\"}")"
check "B 重复拒绝（幂等）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 5. 主叫取消流程
# ---------------------------------------------------------------------------
section "用例5：主叫取消流程"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 1}")"
ROOM3="$(json_get "$RESP" data.roomId)"
check "A 呼叫 B 成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/cancel "$TOKEN_A" "{\"roomId\": \"$ROOM3\"}")"
check "A 取消呼叫成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/accept "$TOKEN_B" "{\"roomId\": \"$ROOM3\"}")"
check "取消后 B 再接听（幂等，房间已结束）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 6. 忙线：B 振铃中，B 反呼 A（A 处于活跃房间）
# ---------------------------------------------------------------------------
section "用例6：忙线检测"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 2}")"
ROOM4="$(json_get "$RESP" data.roomId)"
check "A 呼叫 B（保持振铃）成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/call "$TOKEN_B" "{\"calleeUid\": $UID_A, \"mediaType\": 1}")"
check_not_200 "B 反呼 A 应报忙线" "$RESP"
check_contains "错误提示包含[忙线]" "忙线" "$(json_get "$RESP" msg)"

RESP="$(api POST /rtc/cancel "$TOKEN_A" "{\"roomId\": \"$ROOM4\"}")"
check "A 取消呼叫（清理现场）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 7. 重复呼叫：振铃中 A 再次呼叫 B
# ---------------------------------------------------------------------------
section "用例7：重复呼叫"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 1}")"
ROOM5="$(json_get "$RESP" data.roomId)"
check "A 呼叫 B 成功" "200" "$(json_get "$RESP" code)"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 1}")"
check_not_200 "A 振铃中重复呼叫应被拒绝" "$RESP"
check_contains "错误提示包含[忙线/通话中]" "忙线" "$(json_get "$RESP" msg)$(json_get "$RESP" msg)"

RESP="$(api POST /rtc/cancel "$TOKEN_A" "{\"roomId\": \"$ROOM5\"}")"
check "A 取消呼叫（清理现场）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 8. 结束后重新呼叫（房间可重建）
# ---------------------------------------------------------------------------
section "用例8：结束后重新呼叫"

RESP="$(api POST /rtc/call "$TOKEN_A" "{\"calleeUid\": $UID_B, \"mediaType\": 2}")"
ROOM6="$(json_get "$RESP" data.roomId)"
check "通话结束后 A 可再次呼叫 B" "200" "$(json_get "$RESP" code)"
check "生成新 roomId" "yes" "$([ -n "$ROOM6" ] && [ "$ROOM6" != "$ROOM1" ] && echo yes || echo no)"

RESP="$(api POST /rtc/hangup "$TOKEN_B" "{\"roomId\": \"$ROOM6\"}")"
check "B 挂断（清理现场）" "200" "$(json_get "$RESP" code)"

# ---------------------------------------------------------------------------
# 9. 群通话流程（可选，设置 GROUP_ID 时启用；A 须为群成员）
# ---------------------------------------------------------------------------
GROUP_ROOM=""
if [ -n "${GROUP_ID:-}" ]; then
  section "用例9：群通话流程"

  RESP="$(api POST /rtc/call "$TOKEN_A" "{\"channelId\": \"g_$GROUP_ID\", \"channelType\": 2, \"inviteeUids\": [$UID_B], \"mediaType\": 1}")"
  check "A 发起群通话成功" "200" "$(json_get "$RESP" code)"
  check "返回 channelType=2" "2" "$(json_get "$RESP" data.channelType)"
  check "群通话 token 非空" "yes" "$([ -n "$(json_get "$RESP" data.token)" ] && echo yes || echo no)"
  GROUP_ROOM="$(json_get "$RESP" data.roomId)"
  echo "  roomId=$GROUP_ROOM"

  if [ -n "${INVITE_UID:-}" ]; then
    RESP="$(api POST /rtc/invite "$TOKEN_A" "{\"roomId\": \"$GROUP_ROOM\", \"inviteeUids\": [$INVITE_UID]}")"
    check "群通话追加邀请成员成功" "200" "$(json_get "$RESP" code)"
  fi

  RESP="$(api POST /rtc/end "$TOKEN_B" "{\"roomId\": \"$GROUP_ROOM\"}")"
  check_not_200 "非主持人/管理员结束整场通话应失败" "$RESP"

  RESP="$(api POST /rtc/end "$TOKEN_A" "{\"roomId\": \"$GROUP_ROOM\"}")"
  check "主持人结束群通话成功" "200" "$(json_get "$RESP" code)"
else
  echo ""
  echo "提示：设置 GROUP_ID 环境变量可启用群通话用例，例如："
  echo "  GROUP_ID=9 INVITE_UID=<群内其他成员uid> ./scripts/rtc-api-test.sh"
fi

# ---------------------------------------------------------------------------
# 10. 数据库状态断言（可选，设置 MYSQL_CMD 时启用）
# ---------------------------------------------------------------------------
if [ -n "${MYSQL_CMD:-}" ]; then
  section "用例10：数据库状态流转断言"

  echo "  最近房间记录（status: 0-呼叫中 1-通话中 2-已结束；channel_type: 1-单聊 2-群聊）："
  $MYSQL_CMD -e "SELECT room_id, host_uid, channel_type, status,
    IFNULL(DATE_FORMAT(start_time, '%H:%i:%s'), '-') AS start_time,
    IFNULL(DATE_FORMAT(end_time, '%H:%i:%s'), '-') AS end_time
    FROM t_rtc_room ORDER BY create_time DESC LIMIT 8;"

  echo ""
  echo "  最近参与者记录（status: 0-呼叫中 1-已加入 2-已离开 3-拒绝）："
  $MYSQL_CMD -e "SELECT p.room_id, p.u_id, p.status, p.role
    FROM t_rtc_participant p JOIN t_rtc_room r ON p.room_id = r.room_id
    ORDER BY r.create_time DESC, p.u_id LIMIT 16;"

  ENDED_COUNT="$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM t_rtc_room WHERE room_id IN ('$ROOM1','$ROOM2','$ROOM3','$ROOM6') AND status = 2;")"
  check "上述测试房间均已置为已结束(status=2)" "4" "$ENDED_COUNT"

  if [ -n "$GROUP_ROOM" ]; then
    GROUP_ENDED="$($MYSQL_CMD -N -e "SELECT COUNT(*) FROM t_rtc_room WHERE room_id = '$GROUP_ROOM' AND status = 2 AND channel_type = 2;")"
    check "群通话房间已结束且 channel_type=2" "1" "$GROUP_ENDED"
  fi
else
  echo ""
  echo "提示：设置 MYSQL_CMD 环境变量可启用数据库状态断言，例如："
  echo "  MYSQL_CMD=\"mysql -h127.0.0.1 -uroot -p<密码> <chat库名>\" ./scripts/rtc-api-test.sh"
fi

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------
printf '\n============================================\n'
if [ "$FAIL" -eq 0 ]; then
  printf '测试全部通过：PASS=%d FAIL=%d\n' "$PASS" "$FAIL"
  exit 0
else
  printf '存在失败用例：PASS=%d FAIL=%d\n' "$PASS" "$FAIL"
  exit 1
fi
