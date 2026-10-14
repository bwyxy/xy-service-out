#!/usr/bin/env bash
# ============================================================================
# 多端登录管控 API 自动化测试
#
# 前置条件：
#   1. fg-chat 服务已启动且包含「多端登录管控」新代码（默认 http://localhost:9080）
#   2. 服务依赖的 Redis 可用
#   3. 准备一个可正常登录的测试账号（同一账号模拟多台设备登录）
#
# 用法：
#   ACCOUNT=18600000001 PASSWORD=123456 ./scripts/multi-device-login-test.sh
#
# 可选环境变量：
#   BASE_URL   后端地址，默认 http://localhost:9080
#   REDIS_CMD  完整的 redis-cli 命令，设置后启用会话存储状态断言，
#              例：REDIS_CMD="redis-cli -h 127.0.0.1 -p 6379 -a 密码 --no-auth-warning"
#
# 测试覆盖：
#   同类型手机互踢 / PC 与手机并行 / 同类型平板互踢 / 旧 token 返回 4011 /
#   主动登出立即失效 / 同类型重复登录仅保留最新会话 / 伪造 token 越权防护 /
#   并发登录一致性 / 会话存储状态（可选）
# ============================================================================
set -u

BASE_URL="${BASE_URL:-http://localhost:9080}"
ACCOUNT="${ACCOUNT:?请设置环境变量 ACCOUNT（测试账号：手机号或邮箱）}"
PASSWORD="${PASSWORD:?请设置环境变量 PASSWORD}"

DEVICE_PC=1
DEVICE_PAD=2
DEVICE_PHONE=3

PASS=0
FAIL=0
TMP_BODY="$(mktemp -t xy-mdl-body.XXXXXX)"
trap 'rm -f "$TMP_BODY"' EXIT

# ---------------------------------------------------------------------------
# 工具函数
# ---------------------------------------------------------------------------

# 从 JSON 中按点路径取值：json_get "$resp" data.token
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

# login ACCOUNT PASSWORD DEVICE_TYPE DEVICE_ID
login() {
  curl -s -X POST "$BASE_URL/chat/login" \
    -H "Content-Type: application/json" \
    -d "{\"account\":\"$1\",\"password\":\"$2\",\"deviceType\":$3,\"deviceId\":\"$4\"}"
}

# api METHOD PATH TOKEN [JSON_BODY]
# 结果写入全局变量 HTTP_STATUS / HTTP_BODY
api() {
  local method="$1" path="$2" token="$3" body="${4:-}"
  if [ -n "$body" ]; then
    HTTP_STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path" \
      -H "Content-Type: application/json" -H "token: $token" -d "$body")"
  else
    HTTP_STATUS="$(curl -s -o "$TMP_BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path" \
      -H "token: $token")"
  fi
  HTTP_BODY="$(cat "$TMP_BODY")"
}

section() { printf '\n\033[1m=== %s ===\033[0m\n' "$1"; }

check() { # check 描述 期望 实际
  if [ "$2" = "$3" ]; then
    PASS=$((PASS + 1)); printf '  [PASS] %s\n' "$1"
  else
    FAIL=$((FAIL + 1)); printf '  [FAIL] %s（期望: %s, 实际: %s）\n' "$1" "$2" "$3"
  fi
}

# 断言会话有效：业务码 200
check_alive() { # check_alive 描述 TOKEN
  api POST /auth/userinfo "$2"
  check "$1" "200" "$(json_get "$HTTP_BODY" code)"
}

# 断言会话已失效：HTTP 401 且业务码 4011
check_expired() { # check_expired 描述 TOKEN
  api POST /auth/userinfo "$2"
  check "$1（HTTP 401）" "401" "$HTTP_STATUS"
  check "$1（业务码 4011）" "4011" "$(json_get "$HTTP_BODY" code)"
}

login_token() { # login_token DEVICE_TYPE DEVICE_ID → 输出 token
  local resp
  resp="$(login "$ACCOUNT" "$PASSWORD" "$1" "$2")"
  json_get "$resp" data.token
}

# ---------------------------------------------------------------------------
# 0. 基线：登录手机设备 H1
# ---------------------------------------------------------------------------
section "用例0：基线登录（手机 H1）"

RESP="$(login "$ACCOUNT" "$PASSWORD" "$DEVICE_PHONE" "phone-h1")"
TOKEN_H1="$(json_get "$RESP" data.token)"
UID="$(json_get "$RESP" data.uid)"
check "手机 H1 登录成功" "200" "$(json_get "$RESP" code)"
if [ -z "$TOKEN_H1" ] || [ "$TOKEN_H1" = "None" ]; then
  echo "登录失败: $RESP"; exit 1
fi
echo "  uid=$UID"
check_alive "H1 会话可用" "$TOKEN_H1"

# ---------------------------------------------------------------------------
# 1. 两台手机：同类型互踢
# ---------------------------------------------------------------------------
section "用例1：两台手机（同类型强制互踢）"

TOKEN_H2="$(login_token "$DEVICE_PHONE" "phone-h2")"
check "手机 H2 登录成功" "yes" "$([ -n "$TOKEN_H2" ] && [ "$TOKEN_H2" != "None" ] && echo yes || echo no)"
check "H2 旧 token 与新 token 不同（jti 唯一）" "yes" \
  "$([ "$TOKEN_H1" != "$TOKEN_H2" ] && echo yes || echo no)"
check_expired "H1 被同类型设备顶下线" "$TOKEN_H1"
check_alive "H2 新会话可用" "$TOKEN_H2"

# ---------------------------------------------------------------------------
# 2. PC 与手机：跨类型互不冲突
# ---------------------------------------------------------------------------
section "用例2：PC 与手机并行（跨类型隔离）"

TOKEN_PC="$(login_token "$DEVICE_PC" "pc-01")"
check "PC 登录成功" "yes" "$([ -n "$TOKEN_PC" ] && [ "$TOKEN_PC" != "None" ] && echo yes || echo no)"
check_alive "PC 会话可用" "$TOKEN_PC"
check_alive "PC 登录后手机 H2 会话不受影响" "$TOKEN_H2"

TOKEN_H3="$(login_token "$DEVICE_PHONE" "phone-h3")"
check "手机 H3 登录成功" "yes" "$([ -n "$TOKEN_H3" ] && [ "$TOKEN_H3" != "None" ] && echo yes || echo no)"
check_expired "H3 登录后 H2 被踢" "$TOKEN_H2"
check_alive "PC 会话仍不受手机登录影响" "$TOKEN_PC"

# ---------------------------------------------------------------------------
# 3. 两台平板：同类型互踢
# ---------------------------------------------------------------------------
section "用例3：两台平板（同类型强制互踢）"

TOKEN_T1="$(login_token "$DEVICE_PAD" "pad-01")"
TOKEN_T2="$(login_token "$DEVICE_PAD" "pad-02")"
check "平板 T1/T2 均登录成功" "yes" \
  "$([ -n "$TOKEN_T1" ] && [ "$TOKEN_T1" != "None" ] && [ -n "$TOKEN_T2" ] && [ "$TOKEN_T2" != "None" ] && echo yes || echo no)"
check_expired "T1 被同类型平板顶下线" "$TOKEN_T1"
check_alive "T2 新会话可用" "$TOKEN_T2"
check_alive "PC 会话仍在线（三类型互不干扰）" "$TOKEN_PC"

# ---------------------------------------------------------------------------
# 4. 同类型重复登录：仅保留最新会话
# ---------------------------------------------------------------------------
section "用例4：同类型重复登录"

TOKEN_H4="$(login_token "$DEVICE_PHONE" "phone-h4")"
check_expired "H3 被顶下线" "$TOKEN_H3"
check_alive "H4 会话可用" "$TOKEN_H4"

TOKEN_H5="$(login_token "$DEVICE_PHONE" "phone-h5")"
check_expired "H4 被顶下线" "$TOKEN_H4"
check_alive "H5 会话可用" "$TOKEN_H5"

# ---------------------------------------------------------------------------
# 5. 主动登出：token 立即失效
# ---------------------------------------------------------------------------
section "用例5：主动登出"

api POST /auth/logout "$TOKEN_H5"
check "登出接口返回 200" "200" "$(json_get "$HTTP_BODY" code)"
check_expired "登出后原 token 立即失效" "$TOKEN_H5"

# ---------------------------------------------------------------------------
# 6. 越权与异常防护
# ---------------------------------------------------------------------------
section "用例6：越权与异常防护"

api POST /auth/logout "fake-token-for-test"
check "伪造 token 登出被拒绝（HTTP 401）" "401" "$HTTP_STATUS"

api POST /auth/logout ""
check "缺失 token 登出被拒绝（HTTP 401）" "401" "$HTTP_STATUS"

api POST /auth/userinfo "fake-token-for-test"
check "伪造 token 访问业务接口被拒绝（HTTP 401）" "401" "$HTTP_STATUS"

check_alive "异常请求未影响既有有效会话（T2）" "$TOKEN_T2"
check_alive "异常请求未影响既有有效会话（PC）" "$TOKEN_PC"

# ---------------------------------------------------------------------------
# 7. 并发登录一致性（弱网/重复点击代理场景）
# ---------------------------------------------------------------------------
section "用例7：同类型并发登录一致性"

CONCURRENCY=5
CONC_BODIES="$(mktemp -t xy-mdl-conc.XXXXXX)"
for i in $(seq 1 "$CONCURRENCY"); do
  ( login "$ACCOUNT" "$PASSWORD" "$DEVICE_PHONE" "phone-conc-$i" >> "$CONC_BODIES" ; echo >> "$CONC_BODIES" ) &
done
wait

TOKENS="$(python3 - "$CONC_BODIES" <<'PY'
import json, sys
tokens = []
for line in open(sys.argv[1], encoding='utf-8'):
    line = line.strip()
    if not line:
        continue
    try:
        d = json.loads(line)
    except Exception:
        continue
    t = (d.get('data') or {}).get('token')
    if t:
        tokens.append(t)
print(len(tokens))
for t in tokens:
    print(t)
PY
)"
CONC_COUNT="$(printf '%s' "$TOKENS" | head -1)"
printf '  并发登录成功次数: %s / %s\n' "$CONC_COUNT" "$CONCURRENCY"

if [ "$CONC_COUNT" -ge 2 ]; then
  LAST_TOKEN="$(printf '%s' "$TOKENS" | tail -1)"
  OTHER_TOKEN="$(printf '%s' "$TOKENS" | sed -n '2p')"
  check_expired "并发登录中较早会话已失效" "$OTHER_TOKEN"
  check_alive "并发登录后最新会话可用" "$LAST_TOKEN"
else
  printf '  跳过：并发登录成功次数不足 2，无法校验互踢\n'
fi
rm -f "$CONC_BODIES"

# ---------------------------------------------------------------------------
# 8. 会话存储状态断言（可选，设置 REDIS_CMD 时启用）
# ---------------------------------------------------------------------------
if [ -n "${REDIS_CMD:-}" ]; then
  section "用例8：会话存储状态断言"

  PHONE_SET_SIZE="$($REDIS_CMD SCARD "login:user:$UID:type:$DEVICE_PHONE")"
  PC_SET_SIZE="$($REDIS_CMD SCARD "login:user:$UID:type:$DEVICE_PC")"
  PAD_SET_SIZE="$($REDIS_CMD SCARD "login:user:$UID:type:$DEVICE_PAD")"
  check "手机类型活跃会话索引仅 1 条" "1" "$PHONE_SET_SIZE"
  check "PC 类型活跃会话索引仅 1 条" "1" "$PC_SET_SIZE"
  check "平板类型活跃会话索引为空（已被顶下线移出）" "0" "$PAD_SET_SIZE"

  echo ""
  echo "  PC 会话明细："
  PC_TOKEN="$($REDIS_CMD SMEMBERS "login:user:$UID:type:$DEVICE_PC" | head -1)"
  $REDIS_CMD GET "login:session:$PC_TOKEN"
else
  echo ""
  echo "提示：设置 REDIS_CMD 环境变量可启用会话存储状态断言，例如："
  echo "  REDIS_CMD=\"redis-cli -h 127.0.0.1 -p 6379\" ./scripts/multi-device-login-test.sh"
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