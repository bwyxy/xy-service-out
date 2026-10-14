# 1v1 音视频通话系统实现计划

## Context

聊天应用（y-chat-sub + fg-chat 后端）需要支持用户间 1v1 实时视频/语音通话。数据库已建好 `t_rtc_room`/`t_rtc_participant` 表（[chat.sql:141-169](file:///Users/xiaoyi/me-project/xy-service-out/sql/chat.sql)），实体类 `RtcRoom`/`RtcParticipant` 已存在但无任何业务代码；前端 ChatWindow 头部和好友资料卡有"视频/语音通话"占位图标（无逻辑）。

用户已确认：**仅 1v1（P2P WebRTC，无 SFU）** + **微信风格通话记录**（聊天窗口灰色居中提示"通话时长 03:25/未接听/已拒绝"）。交付物含系统设计文档、API 规范、测试报告、部署文档。

## 架构总览

```
控制面（REST，后端维护房间状态机）
  POST /rtc/call|accept|reject|hangup|cancel   GET /rtc/ice-servers
        │ 写 t_rtc_room + t_rtc_participant
        ▼
信令通道（悟空IM 个人频道，payload=Base64({type,content})，复用现有 /message/send 或 WuKongClient）
  200 invite 201 cancel 202 accept 203 reject 204 hangup 205 busy   ← 后端经 WuKongClient 推送
  206 offer 207 answer 208 candidate                              ← 前端经现有 POST /message/send（noPersist=1 不入历史）
  210 rtcRecord 通话记录                                           ← 后端持久化推送，双端聊天窗口渲染
        ▼
媒体面（WebRTC P2P，DTLS-SRTP 强制加密，ICE/STUN/TURN 穿透）
```

wire 类型用 200+ 避开前端已占用的 1-103（前端 `isSystemMessageType = type >= 100`，必须在 message-handler **最前置**拦截 RTC 类型，否则会误弹系统通知）。

## 后端实现（customer-api/fg-chat，包 com.chat.fg）

遵循现有模式：`@RestController + @RequestMapping + @RequiredArgsConstructor`、`R.ok/R.error`、`SecurityUtils.getUId()`、MyBatis-Plus `ServiceImpl`。

| 文件（新建） | 职责 |
| --- | --- |
| `mapper/RtcRoomMapper.java`、`mapper/RtcParticipantMapper.java` | extends BaseMapper；参与者加 `selectActiveByUid`（Wrapper 查 status IN (0,1)） |
| `service/IRtcService.java` + `service/impl/RtcServiceImpl.java` | 核心状态机（见下） |
| `controller/RtcController.java` | `/rtc` 六个端点 |
| `pojo/dto/CallDTO.java`（calleeUid, mediaType 1语音/2视频）、`pojo/dto/RoomDTO.java`、`pojo/vo/CallVO.java`、`pojo/vo/IceServerVO.java` | DTO/VO |
| `task/RtcTimeoutTask.java` | `@Scheduled(fixedDelay=10s)`：status=0 且创建超 90s 的房间置结束，双端推 201 + 210"未接听" |

**状态机要点**（全部在 `DistributedLockHelper.tryLock("rtc:call:{uid}", 3, 10, ...)` 内做 DB 校验，锁只包 DB 段不包 RPC）：
- `call`：被叫有活跃房间 → 推 205 忙线给主叫并返回；主叫有活跃房间 → R.error；否则建房间（roomId=UUID，status=0，hostUid=主叫，channelId=被叫 uid）+ 两条 participant，向被叫推 200（content JSON：roomId/主叫昵称头像/mediaType）
- `accept`：校验 status=0 → status=1、startTime=now、participant status=1，向主叫推 202
- `reject`：participant status=3、房间结束，主叫收 204，双端 210"已拒绝"
- `hangup`：房间结束、向对方推 204，双端 210（时长=now-startTime，<3s 记"未接通"）
- `cancel`：仅主叫且 status=0，被叫收 201 停铃，双端 210"已取消"
- 私有 `pushRtcSignal(toUid, type, contentObj)`：仿 [MessageServiceImpl.sendMessage](file:///Users/xiaoyi/me-project/xy-service-out/customer-api/fg-chat/src/main/java/com/chat/fg/service/impl/MessageServiceImpl.java#L81-L125) 构造 Base64 payload + MessageHeader（200-208 信令 noPersist=1/redDot=0；210 记录 noPersist=0/redDot=1），fromUid=对端 uid
- 全部接口幂等：房间已结束时重复调用返回 R.ok 不报错（以 DB 状态为准）

**改动现有文件**：
- [MessageType.java](file:///Users/xiaoyi/me-project/xy-service-out/xy-common/src/main/java/com/xy/common/enums/MessageType.java)：追加 200-210 枚举值（旧 100-106 未被引用但保留不动）
- [application.yml](file:///Users/xiaoyi/me-project/xy-service-out/customer-api/fg-chat/src/main/resources/application.yml)：`xy.rtc.ice-servers`（默认 `stun:stun.l.google.com:19302`，支持 TURN 可配）+ `xy.rtc.call-timeout-seconds: 90`

## 前端实现（y-micro-app/apps/y-chat-sub/src）

| 文件 | 职责 |
| --- | --- |
| `services/im/constants.ts` | 追加 RTC_* = 200..210 + `isRtcSignalType()` |
| `services/im/message-handler.ts` | handleIncomingMessage 最前置：200-208 → rtcHandlers（不弹 ElNotification 不进气泡）；210 → chatHandlers 进消息列表 |
| `api/rtc.ts` | call/accept/reject/hangup/cancel/getIceServers（复用 [utils/request.ts](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/utils/request.ts)） |
| `services/rtc/signaling.ts` | sendRtcSignal(peerUid, type, data) 走现有 sendMessage；订阅 message-handler rtc 事件 |
| `services/rtc/webrtc.ts` | RTCPeerConnection 封装：getUserMedia(audio[,video])、offer/answer/setRemote、onicecandidate→208、ontrack→绑流、连接状态监听；`startStatsMonitor()` 每 5s getStats（RTT/丢包率/jitter/bitrate）→ 质量分级；iceConnectionState failed → restartIce() 一次，再失败 → 挂断 |
| `store/call.ts`（pinia） | 状态机 `idle→outgoing→incoming→connecting→active→ended`；字段 roomId/peer/mediaType/localStream/remoteStream/audioMuted/videoMuted/duration/networkLevel；action：startCall/onIncoming/accept/reject/hangup/cancel/onSignal/toggleMute/toggleVideo |
| `components/CallOverlay/index.vue` | 全屏浮层（layout 挂载）：远端视频全屏 + 本地 PIP、状态文案（正在等待对方接受/正在响铃/通话中 mm:ss）、控制栏（静音/摄像头/挂断）、网络质量角标；纯语音时显示头像 |
| `components/IncomingCallDialog/index.vue` | 来电弹窗（仿 [NotificationCenter](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/layout/Header/NotificationCenter.vue) 的 el-dialog 模式）：头像+昵称+类型、接听/拒绝；铃声用 **WebAudio 合成**（oscillator 双音铃音，无外部资源依赖） |
| [MessageBox.vue](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/components/MessageBox/MessageBox.vue) | `type===210` 渲染灰色居中提示行 |
| [ChatWindow.vue](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src/views/chat/components/ChatWindow/ChatWindow.vue#L134) + `views/friend/index.vue` | 占位图标/按钮接线 → `callStore.startCall(uid, mediaType)` |

**主时序**：A 点击通话 → GET ice-servers → POST /rtc/call（B 弹 IncomingCallDialog 响铃，A 显示 outgoing）→ B 接听 POST /rtc/accept（A 收 202）→ **B** getUserMedia 后经 /message/send 发 206 offer → A setRemote + getUserMedia 回 207 answer → 互发 208 candidate → ICE/DTLS 通 → active 计时 → 任一方 hangup → 双方 UI 结束 + 210 落聊天记录。

**异常分支**：拒接(204+210)、忙线(205，call 直接返回)、主叫取消(201)、90s 超时(定时任务)、WebRTC 断连(ICE failed→restartIce 一次→失败走 hangup)。

**弱网降级**：丢包>15% 或 RTT>500ms → `sender.replaceTrack(null)` 自动关视频转纯语音 + UI 提示，恢复后可手动重开。

## macOS Tauri 摄像头/麦克风权限

新建 `src-tauri/Info.plist`（Tauri CLI 自动与生成值合并，官方机制）：
```xml
<key>NSCameraUsageDescription</key><string>用于视频通话</string>
<key>NSMicrophoneUsageDescription</key><string>用于语音通话</string>
```
需重启 `tauri dev` 生效。

## 文档交付（用户明确要求，新建 docs/rtc/）

1. `design.md` — 系统设计方案 + API 接口规范 + 前端交互流程 + 后端实现逻辑
2. `deployment.md` — 部署文档（含 coturn TURN 服务器 Docker 自建配置、ICE servers 配置说明、macOS 权限、Nginx/HTTPS 要求）
3. `test-report.md` — 测试报告（功能/性能/兼容性）

## 实施顺序

1. 后端：枚举 → Mapper/Service/Controller/DTO → yml → 超时任务 → 编译 + 重启 fg-chat
2. 前端基础层：constants → message-handler 拦截 → api/rtc.ts → signaling.ts
3. WebRTC 层：webrtc.ts
4. 状态机：store/call.ts
5. UI：CallOverlay → IncomingCallDialog → MessageBox 210 → 入口接线
6. Tauri Info.plist + dev 重启
7. 测试（下节）→ 写三份文档

## 验证

- **自动化**：curl 脚本（双测试账号登录 → call → accept → reject → busy → hangup → 断言 t_rtc_room/t_rtc_participant 状态流转与 210 消息推送）；`vue-tsc -b` + `pnpm build`；本地 MySQL（docker 3307）与 fg-chat（9080）已确认在运行
- **手动**（双端各开一个窗口/一个 Tauri 桌面端）：发起/响铃/接听/通话/挂断全流程、拒接、忙线、超时取消、静音/摄像头开关、弱网降级（Network Link Conditioner 20% 丢包）、macOS 权限弹窗、通话记录展示
- 手动测试结果回填 `test-report.md`

## 风险与对策

- 悟空IM noPersist 信令仅在线可达（离线丢失）→ 90s 超时任务兜底，210 记录持久化
- 200+ 类型漏拦截会误弹系统通知 → message-handler 最前置拦截 + 单测覆盖类型分流
- WKWebView getUserMedia 需 macOS 10.15+ 且需 Info.plist 声明 → 已纳入步骤 6
- DB 与前端 store 双状态不一致 → 接口幂等、信令 content 携带 roomId 供校验
