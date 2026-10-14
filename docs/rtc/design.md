# 1v1 音视频通话系统设计方案

## 1. 系统概述

基于现有数据库表结构（`t_rtc_room` / `t_rtc_participant`）与悟空 IM 消息通道，实现 1v1 实时音视频通话。媒体流走 WebRTC P2P（DTLS-SRTP 端到端加密），不经服务端转发；房间状态与呼叫控制由 fg-chat 后端管理；实时信令复用悟空 IM 个人频道传输，不引入额外 WebSocket。

### 1.1 三平面架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端（y-chat-sub）                     │
│  CallOverlay / IncomingCallDialog / call store / webrtc.ts   │
└──────────────┬──────────────────┬───────────────────────────┘
               │ REST（控制面）     │ 悟空IM WSS（信令面）
               ▼                  ▼
┌──────────────────────┐   ┌──────────────────────┐
│  fg-chat（9080）      │   │  悟空 IM             │
│  RtcController       │   │  个人频道消息转发      │
│  RtcServiceImpl      │──▶│  payload=Base64(...)  │
│  状态机/锁/超时任务    │   │  (Feign WuKongClient)│
└─────────┬────────────┘   └──────────────────────┘
          │ MyBatis-Plus
          ▼
┌──────────────────────┐        ┌──────────────────────┐
│  MySQL               │        │  P2P 媒体面（直连）    │
│  t_rtc_room          │        │  Audio/Video SRTP    │
│  t_rtc_participant   │        │  ICE/STUN/TURN 穿透   │
└──────────────────────┘        └──────────────────────┘
```

| 平面 | 通道 | 职责 |
|------|------|------|
| 控制面 | REST `/rtc/*` | 房间状态机（呼叫/接听/拒绝/挂断/取消）、忙线检测、ICE 配置下发 |
| 信令面 | 悟空 IM 个人频道 | offer/answer/candidate 实时交换、来电邀请、结束通知、通话记录落库展示 |
| 媒体面 | WebRTC P2P | 音视频流传输（DTLS-SRTP 加密），无需 SFU |

### 1.2 关键决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 媒体拓扑 | P2P（无 SFU） | 1v1 场景下直连时延最低、成本最低；表结构中的 `sfu_node` 字段为未来多方会议预留 |
| 信令通道 | 复用悟空 IM | 免新增连接，天然在线感知；MessageHeader 可控持久化/红点 |
| 接听后 offer 方 | 被叫 B | 被叫点接听时本地媒体已就绪，避免主叫在回铃阶段提前采集 |
| 通话记录 | wire 210 持久化消息 | 复用 IM 消息链路，微信风格灰色居中提示，双端各落一条 |

## 2. 数据模型

沿用既有表，字段语义如下：

### t_rtc_room（房间）

| 字段 | 说明 |
|------|------|
| room_id | UUID（去横线），主键 |
| room_type | 1-语音 2-视频（3-会议预留） |
| host_uid | 主叫 |
| channel_id | 被叫 uid |
| status | **0-呼叫中 → 1-通话中 → 2-已结束** |
| start_time / end_time | 接听时刻 / 结束时刻 |

### t_rtc_participant（参与者）

| 字段 | 说明 |
|------|------|
| room_id + u_id | 唯一键，每房间固定两条 |
| role | 0-普通（被叫） 1-主持人（主叫） |
| status | **0-呼叫中 → 1-已加入**；结束：2-已离开 / 3-拒绝 |
| join_time / leave_time | 接听 / 挂断时刻 |

### 状态机

```
房间：  CALLING(0) ──accept──▶ ACTIVE(1) ──hangup──▶ ENDED(2)
           │ │
           │ ├──reject/cancel/timeout──▶ ENDED(2)
           └──（幂等：已结束房间的一切操作直接返回成功）

参与者：主叫 P_CALLING(0) ──────────────▶ P_LEFT(2)
        被叫 P_CALLING(0) ──accept─▶ P_JOINED(1) ──hangup─▶ P_LEFT(2)
                          └─reject─▶ P_REJECTED(3)
```

## 3. 信令协议（悟空 IM 个人频道）

消息 payload 为 `Base64({ type, content })`，`content` 为 JSON 字符串。wire 类型在 `MessageType` 枚举中定义：

| 类型 | 含义 | 方向 | content 字段 |
|------|------|------|--------------|
| 200 | 来电邀请 | 后端→被叫 | `roomId, mediaType, caller{uid,nickname,avatar}` |
| 201 | 主叫取消 | 后端→被叫 | `roomId, mediaType` |
| 202 | 被叫接听 | 后端→主叫 | `roomId, mediaType` |
| 203 | 被叫拒绝 | 后端→主叫 | `roomId, mediaType` |
| 204 | 挂断 | 后端→对端 | `roomId, mediaType` |
| 205 | 忙线 | 后端→主叫 | `roomId, mediaType` |
| 206 | SDP Offer | 被叫→主叫（P2P 转发） | `roomId, sdp` |
| 207 | SDP Answer | 主叫→被叫 | `roomId, sdp` |
| 208 | ICE Candidate | 双向 | `roomId, candidate{...}` |
| 210 | 通话记录 | 后端→双端 | `roomId, mediaType, text, duration, status` |

**MessageHeader 约定**：

- 200-208（实时信令）：`noPersist=1, redDot=0` —— 不落聊天记录、无红点
- 210（通话记录）：`noPersist=0, redDot=1` —— 落聊天记录、亮红点

**前端拦截**：`message-handler.ts` 在消息分发最前置判断 `isRtcSignalType(type)`（200-208），直接路由给 RTC 信令服务，不进入聊天/系统消息流；210 走聊天消息流但不弹系统通知。类型号 200+ 与前端既有业务类型 1-103 完全错开。

## 4. API 接口规范

统一前缀 `http://<host>:9080`，鉴权头 `token: <JWT>`，响应体 `{ code, msg, data }`，业务成功 `code=200`，业务失败 HTTP 仍为 200 但 `code≠200`。

### 4.1 发起通话

```
POST /rtc/call
{ "calleeUid": 10000001, "mediaType": 1 }   // mediaType: 1-语音 2-视频

200 → { "code": 200, "data": { "roomId": "a1b2c3...", "mediaType": 1 } }
失败 → { "code": 500, "msg": "对方忙线，请稍后再试" | "不能呼叫自己" | "你正处于通话中，请先结束当前通话" }
```

并发控制：以 `rtc:call:{calleeUid}` 为 Redis 锁键（3 秒等待 / 10 秒持有），防止并发呼叫同一被叫产生重复房间。

### 4.2 接听

```
POST /rtc/accept   { "roomId": "..." }
```

仅被叫本人、房间处于呼叫中时生效；否则幂等返回成功。

### 4.3 拒绝 / 挂断 / 取消

```
POST /rtc/reject   { "roomId": "..." }   // 被叫拒绝（振铃阶段）
POST /rtc/hangup   { "roomId": "..." }   // 任一方挂断（通话阶段）
POST /rtc/cancel   { "roomId": "..." }   // 主叫取消（振铃阶段）
```

三者均幂等（房间已结束直接返回成功），均向对端推送对应信令并向双端落 210 记录。

### 4.4 ICE 服务器配置

```
GET /rtc/ice-servers
→ { "code": 200, "data": [ { "urls": "stun:stun.l.google.com:19302" },
                           { "urls": "turn:turn.example.com:3478", "username": "...", "credential": "..." } ] }
```

配置来源 `xy.rtc.ice-servers`，为空时兜底 Google STUN。

### 4.5 错误码

| code | 场景 |
|------|------|
| 200 | 成功（含幂等成功） |
| 401 | 未登录 / token 失效 |
| 500 | 业务失败（忙线、呼叫自己、超时锁竞争等，msg 为用户可读文案） |

## 5. 前端交互流程

### 5.1 呼叫时序（正常接通）

```
主叫 A                                     被叫 B
  │ 语音/视频按钮 → startCall()               │
  │──GET /rtc/ice-servers──▶ 后端            │
  │──POST /rtc/call──────▶ 后端 ──200 邀请──▶│ IncomingCallDialog 弹出 + 响铃
  │ 回铃音 + 90s 超时定时器                   │
  │                                         │ 点击接听 → POST /rtc/accept
  │◀──────────────202 接听（后端推送）────────│
  │ 停回铃 → connecting                      │ 停响铃 → connecting
  │ 初始化本地媒体（getUserMedia）            │ 初始化本地媒体
  │                                         │◀═ 206 Offer（B 为 offer 方）═
  │◀═══════ 207 Answer ════════════════════│
  │◀═══════ 208 Candidate ═══════▶ 互发 ═══▶│
  │        WebRTC P2P 媒体通道建立           │
  │ onConnected → active + 计时 + 质量监测   │
  │                                         │
  │ 任一方点击红色挂断 → POST /rtc/hangup     │
  │◀──── 204 挂断信令 + 双端 210 通话记录 ────│
  │ ended（显示"通话已结束"）3s 后复位 idle    │
```

### 5.2 异常分支

| 场景 | 行为 |
|------|------|
| 被叫点拒绝 | B 调 `/rtc/reject`；A 收 203 → ended（"对方已拒绝"） |
| 主叫振铃中取消 | A 调 `/rtc/cancel`；B 收 201 → 关闭弹窗 |
| 被叫 90s 未接听 | 前端 armCallTimeout 超时 → A 自动 cancel；后端 `RtcTimeoutTask`（10s 周期）兜底将超时房间置结束并推信令，防状态残留 |
| 被叫忙线 | 后端 call 时检测活跃房间，直接返回"对方忙线"，A 端 toast 提示 |
| ICE 连接失败 | offer 方 `restartIce()` 自动重试一次，仍失败 → onFailed → 自动挂断 |
| 弱网（丢包>15% 或 RTT>500ms） | 视频通话自动 `replaceTrack(null)` 降级为纯语音并提示，网络恢复可手动点"恢复视频" |
| candidate 早于 remoteDescription 到达 | 缓存 `pendingCandidates`，remoteDescription 就绪后 flush |

### 5.3 界面组件

- **CallOverlay**（`src/components/CallOverlay`）：全屏通话浮层。远端视频全屏 + 本地 PIP（点击互换）；纯语音/对端关视频时显示头像舞台；顶栏显示昵称、mm:ss 计时、网络质量角标（良好/一般/较差）；底部静音/摄像头/挂断控制。
- **IncomingCallDialog**（`src/components/IncomingCallDialog`）：来电弹窗，头像 + 昵称 + 红色拒绝 / 绿色接听，遮罩不可点击关闭。
- **通话记录**：MessageBox 中 wire 210 渲染为微信风格灰色居中胶囊（电话图标 + "视频通话 · 通话时长 03:25 / 未接听 / 已拒绝"）。
- **入口**：聊天窗口 header 语音/视频两个图标；好友详情页"语音聊天/视频聊天"按钮。

### 5.4 前端状态机（store/call.ts）

```
idle → outgoing（主叫回铃）──202──▶ connecting ──媒体连通──▶ active
idle → incoming（被叫响铃）──接听──▶ connecting（B 发 offer）
任一状态 → ended（3s 后回 idle）
```

铃声由 WebAudio 合成（无外部资源）：来电 440+480Hz 双音（响 2s 停 4s）、回铃 450Hz（响 1s 停 3s）。

## 6. 后端实现逻辑

### 6.1 核心类

| 类 | 职责 |
|----|------|
| `RtcController` | 6 个 REST 端点，`SecurityUtils.getUId()` 取当前用户 |
| `RtcServiceImpl` | 状态机核心：锁、房间/参与者状态流转、信令推送、210 记录生成 |
| `RtcTimeoutTask` | `@Scheduled(fixedDelay=10s)` 调 `expireStaleRooms()`：呼叫中超 90s 的房间自动结束，按接听与否生成"未接听/通话时长 xx"记录 |
| `RtcProperties` | `xy.rtc.*` 配置绑定（iceServers、callTimeoutSeconds） |

### 6.2 关键机制

- **忙线判定**：`hasActiveRoom(uid)` 查该用户存在于 `status IN (0,1)` 房间的任一参与者记录。call 时先查被叫（对方忙线）再查主叫（自己正在通话中）。
- **幂等**：accept/reject/hangup/cancel 均先判房间是否存在且状态匹配，不匹配直接返回；重复挂断、对已结束房间接听均无害。
- **分布式锁**：`DistributedLockHelper.tryLock`（setIfAbsent 自旋），呼叫按被叫维度、房间操作按 roomId 维度加锁，锁竞争超时返回"操作过于频繁"。
- **通话记录文案**：`buildRecordText()` 生成"视频通话 · 通话时长 03:25"；未接听（接听前结束）记"未接听"；时长 <3s 记 failed。挂断时长 = `Duration.between(startTime, now)`。
- **信令推送**：`pushRtcSignal(toUid, fromUid, type, content)` 经 Feign `WuKongClient` 发个人频道消息，按类型设置 noPersist/redDot。

## 7. 网络适应性与质量优化

| 机制 | 实现 |
|------|------|
| NAT 穿透 | ICE + STUN（默认 `stun:stun.l.google.com:19302`），生产可加自建 coturn TURN（见 deployment.md） |
| 断线重连 | `onconnectionstatechange=failed` 时 offer 方 `restartIce()` 重试一次 |
| 弱网降级 | `getStats()` 5s 周期监测 remote-inbound-rtp（RTT/丢包）与 outbound-rtp（码率差分），丢包>15% 或 RTT>500ms 判定 poor，自动关视频省带宽；>5%/200ms 为 medium |
| 采集优化 | 音频开回声消除/降噪/自动增益；视频 1280x720 ideal，设备能力不足自动降档 |
| 端到端延迟 | P2P 直连，无服务端转发跳数 |

## 8. 安全性

- **媒体加密**：WebRTC 强制 DTLS-SRTP，密钥协商在浏览器内完成，服务端不可见明文。
- **信令鉴权**：REST 全部走 JWT（`token` 头）；悟空 IM 连接有独立 IM token；信令经服务端校验 roomId 归属后才推送。
- **房间隔离**：candidate/offer 信令处理前校验 `roomId` 与当前会话一致，防串扰与伪造。
- **超时清理**：定时任务兜底清理残留房间，防止状态悬挂被利用。
