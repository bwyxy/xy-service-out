# 多端登录管控功能测试报告

- 功能：即时通讯聊天系统多端登录管控（设备类型分组隔离 + 同类型单会话互踢）
- 设计文档：[design.md](./design.md)
- 自动化脚本：[scripts/multi-device-login-test.sh](file:///Users/xiaoyi/me-project/xy-service-out/scripts/multi-device-login-test.sh)
- 测试日期：2026-10-15

---

## 1. 结论摘要

| 类别 | 项目 | 结果 |
|------|------|------|
| 静态验证 | 后端编译（xy-common / xy-system / xy-framework / fg-chat） | 通过 |
| 静态验证 | 前端类型检查（`vue-tsc -b --force`，y-chat-sub） | 通过（EXIT=0） |
| 静态验证 | 失效会话拦截链路审查（黑名单 → 401 + 4011） | 通过 |
| 静态验证 | 会话 CRUD 越权入口审查 | 通过（无对外 CRUD 接口） |
| 端到端 | 全场景自动化用例（"§4 场景矩阵"） | **待执行**（见 §5） |

> 端到端未执行的原因：当前本地（`127.0.0.1:9080`）与线上（`https://www.bwyxy.cn/chat-api`）均未部署包含本次改动的后端，
> 自动化脚本需要运行「包含本次改动的新代码」的 fg-chat 实例与可用 Redis 才能得到有效结论；未在旧版本上执行以免产出误导性失败结论。

---

## 2. 测试环境

| 项 | 说明 |
|----|------|
| 后端 | fg-chat（Spring Boot，context-path `/chat-api`），依赖 MySQL + Redis + 悟空 IM |
| 前端 | y-chat-sub（Vue3 + Vite + Tauri 桌面端），网页端 / 桌面端共用 |
| 构建 | Maven（JDK 21）、Node 24 + `vue-tsc` |
| 部署方式 | 本地 `http://localhost:9080` 或线上域名 + `/chat-api` 反向代理 |

---

## 3. 已执行验证明细

### 3.1 后端编译

```
$ mvn -o -pl customer-api/fg-chat,xy-framework,xy-common -am compile -DskipTests
[INFO] xy-common .......................................... SUCCESS
[INFO] xy-system .......................................... SUCCESS
[INFO] xy-framework ....................................... SUCCESS
[INFO] fg-chat ............................................ SUCCESS
[INFO] BUILD SUCCESS
```

覆盖新增/修改文件：`DeviceType`、`SessionStatus`、`LoginSession`、`ILoginSessionService`、
`LoginSessionServiceImpl`、`ForceLogoutNotifier`、`SessionTouchFilter`、`AuthServiceImpl`、
`AuthController`、`LoginDTO`、`JwtTokenProvider`、`MessageType`、`ResultCode`、`JwtAuthenticationFilter`。

### 3.2 前端类型检查

```
$ node ./node_modules/vue-tsc/bin/vue-tsc.js -b --force
EXIT=0
```

### 3.3 静态链路核查

| 核查点 | 结论 |
|--------|------|
| 登录是否会写入会话 | `AuthServiceImpl#login` → `replaceLoginSession` → `LoginSessionServiceImpl#replaceSession`，是 |
| 同类型旧会话是否被标记失效 | `kickSameTypeSessions` 对索引内全部 ACTIVE 会话执行 `endSession(token, KICKED)`，是 |
| 是否推送强制下线 | 被踢列表逐个 `ForceLogoutNotifier#notifyKicked`（220 信令，`system_admin` → uid），是 |
| 失效 token 是否会被拦截 | `JwtAuthenticationFilter` 命中 `blacklist:{token}` 直接返回 HTTP 401 + `code=4011` 并 `return`（不再继续过滤器链），是 |
| 跨类型是否互相影响 | 索引键为 `login:user:{uid}:type:{deviceType}`，互踢仅作用于同类型分组，是 |
| 前端 220 是否会被误判为系统通知 | `message-handler` 中 220 分支位于 `isSystemMessageType(>=100)` 之前，是 |
| 越权入口 | 对外仅 `POST /auth/logout`（需认证），且校验 `session.uid == 当前登录 uid`；无会话列表/删除类接口，是 |
| 最后活跃时间是否维护 | `SessionTouchFilter` 对携带 token 的请求调用 `touch`（5 分钟节流），是 |

---

## 4. 场景矩阵（端到端用例与预期结果）

对应需求 8「全场景测试」。下表为脚本 `scripts/multi-device-login-test.sh` 覆盖的用例与预期；同一账号通过 `deviceId` 模拟多台设备。

| # | 场景 | 步骤 | 预期结果 | 脚本用例 |
|---|------|------|----------|----------|
| 1 | 两台手机 | 手机 H1 登录 → 手机 H2 登录 → H1 携带旧 token 调 `/auth/userinfo` | H2 登录成功；H1 返回 HTTP 401 + `4011`；H2 正常返回 200 | 用例1 |
| 2 | PC + 手机 | PC 登录 → 手机 H2 登录 → 分别请求 | PC 与手机互不影响，两个 token 均可用；同类型新登录不踢跨类型设备 | 用例2 |
| 3 | 两台平板 | 平板 T1 登录 → 平板 T2 登录 | T1 返回 401 + `4011`；T2 可用；PC 会话仍在线 | 用例3 |
| 4 | 旧 token 失效（离线设备回归） | 设备被踢后（模拟离线期间）携带旧 token 请求任意业务接口 | HTTP 401 + `code=4011`，前端跳转登录页 | 用例1/2/3 中的 `check_expired` |
| 5 | 弱网 / 重复提交一致性 | 同类型并发登录 5 次 | 仅 1 个活跃会话（索引 SCARD=1）；最后一次登录的 token 可用，其余失效 | 用例7 + 用例8 |
| 6 | 主动登出 | 调用 `POST /auth/logout` 后再用原 token 请求 | 登出返回 200；原 token 立即返回 401 + `4011` | 用例5 |
| 7 | 越权防护 | 伪造 token / 缺失 token 调 `/auth/logout`、`/auth/userinfo` | 均返回 HTTP 401；不影响既有有效会话 | 用例6 |
| 8 | 会话存储状态（可选） | 设置 `REDIS_CMD` 后断言索引与明细 | `login:user:{uid}:type:3` SCARD=1；被踢类型索引为 0；明细 `status` 为 `ACTIVE`/`KICKED` | 用例8 |

### 4.1 客户端交互验收（需人工确认）

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| C1 | 网页端被顶下线 | 同一账号在浏览器 A 登录 → 浏览器 B（同设备类型）登录 | A 页立即弹出「您的账号已在其他同类型设备上登录，当前设备已被强制退出」，点击「重新登录」回到登录页，且 A 页所有接口不再可用 |
| C2 | 网页端刷新后旧 token | A 页被踢后刷新页面 | 接口返回 401 + `4011`，自动跳转登录页 |
| C3 | 桌面端被顶下线 | 桌面端（PC）与浏览器（PC 类型）同账号登录 | 桌面端弹窗并切回登录窗口 |
| C4 | 手机 + PC 并行 | 手机浏览器登录后 PC 登录 | 两端同时在线，无弹窗 |
| C5 | 主动退出登录 | 点击「退出登录」 | 服务端会话置 `LOGOUT`，前端跳转登录页，返回键/刷新不会恢复登录态 |

---

## 5. 待执行项与执行方式

### 5.1 前置条件

1. 部署包含本次改动的 fg-chat（`mvn -pl customer-api/fg-chat -am clean package -DskipTests` 后启动）；
2. 确认 Redis 连通（会话与黑名单存储）；
3. 准备一个可登录的测试账号（手机号或邮箱）。

### 5.2 执行命令

```bash
# 基础用例（互踢 / 跨类型 / 登出 / 越权 / 并发）
ACCOUNT=<测试账号> PASSWORD=<密码> \
  BASE_URL=http://localhost:9080 \
  ./scripts/multi-device-login-test.sh

# 追加会话存储断言
ACCOUNT=<测试账号> PASSWORD=<密码> \
  BASE_URL=http://localhost:9080 \
  REDIS_CMD="redis-cli -h 127.0.0.1 -p 6379 --no-auth-warning" \
  ./scripts/multi-device-login-test.sh
```

脚本输出 `PASS/FAIL` 汇总，退出码 0 表示全部通过。

### 5.3 执行后需回填

执行完成后，将脚本输出（PASS/FAIL 明细）与 §4.1 的人工验收结论回填至本报告 §1 表格，并将「端到端」状态由「待执行」更新为实测结果。

---

## 6. 风险与回归建议

| 风险 | 影响 | 建议 |
|------|------|------|
| 悟空 IM `resetUserToken(deviceLevel=0)` 抢占连接 | 同类型互踢的实时通知可能依赖推送时序（本次已把推送提前到 `resetUserToken` 之前） | 端到端用例 1 必须验证「旧设备实际收到弹窗」，而非仅验证 token 失效 |
| Redis 不可用 | 登录时 `replaceSession` 直接抛错，登录失败（不做降级） | 部署时确保 Redis 高可用；如需降级需产品确认 |
| 前端 `deviceId` 依赖 localStorage | 清理浏览器数据后会被识别为「新设备」，可能踢掉同设备旧会话 | 属预期行为（与主流 IM 一致），如需强识别可后续引入设备指纹 |
| 会话记录保留至 token 过期（24h） | Redis 中失效会话明细会短期留存 | 属可追溯性设计，如需清理可加定时任务 |
| 登出接口失败不阻断本地退出 | 极端情况下服务端会话仍为 `ACTIVE` | token 24h 自然过期兜底；前端已清本地态，用户无感知 |

---

## 7. 回归范围建议

1. 登录 / 注册 / 退出登录全链路；
2. 桌面端与网页端同账号登录（PC 分组内互踢）；
3. RTC 音视频通话时被顶下线（通话中断是否符合预期）；
4. 消息收发与未读数（`SessionTouchFilter` 新增过滤器对链路无副作用）。