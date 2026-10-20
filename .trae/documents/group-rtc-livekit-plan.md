# 群聊音视频通话（自建 LiveKit SFU）实施方案

## 一、Context（为什么做这件事）

当前项目的音视频通话只有 **1v1**，媒体面是自研 P2P：

- 媒体层 `services/rtc/webrtc.ts` 的 `RtcConnection` 自己管 `RTCPeerConnection`、ICE 重启、弱网降级（418 行）；
- 信令层 `MessageType` 的 206/207/208 携带 SDP/ICE，经悟空 IM 个人频道点对点传输（`services/rtc/signaling.ts`）；
- 后端 `RtcServiceImpl` 是**二元模型**：`RtcRoom.channelId` 被复用存被叫 uid，`peerOf()` 用「是 host 就是另一端」取对端，`hangup()` 直接结束整房；
- 群聊连入口都没有（`ChatWindow.vue` header 的通话下拉被 `v-if="channelType === 1"` 限制）。

需求是**群聊多人音视频（上限 9 人）**。P2P 全互联在 9 人视频下上行带宽不可接受，且 NAT 穿透失败率随人数上升，因此改为 **自建 LiveKit（SFU）**。

已确认的三项决策：

1. **1v1 一并迁移到 LiveKit**，媒体层只保留一套（LiveKit SDK 自带信令、SDP/ICE、simulcast/dynacast 与连接质量），同时它**内置 TURN**，生产可免去自建 coturn。
2. **呼叫控制信令继续复用悟空 IM 个人频道**（200 邀请 / 201 取消 / 202 接听 / 203 拒绝 / 204 挂断离开 / 205 忙线），媒体面不再需要任何自研信令。
3. **群通话人数上限 9**（写入 LiveKit 房间 `maxParticipants` 与前端选人上限）。

预期结果：群聊头部可发起/邀请多人音视频通话，宫格界面显示各成员画面与静音/说话状态，群主可结束通话，通话结束后群内出现一条记录；1v1 行为与现有观感保持一致。

---

## 二、目标架构

| 平面 | 载体 | 职责 |
|---|---|---|
| 控制面 | REST `/rtc/*` | 建房、权限校验、参与者状态迁移、签发 LiveKit token、通话记录 |
| 呼叫信令面 | 悟空 IM 个人频道（`channelType=1`） | 200-205 呼叫控制（不传媒体数据） |
| 媒体面 | LiveKit SFU，**房间名 = `t_rtc_room.room_id`** | 轨道收发、simulcast/dynacast、静音状态同步、连接质量、重连 |
| 状态面 | MySQL `t_rtc_room` / `t_rtc_participant` | 意图与审计记录、忙线判定 |

关键取舍：

- **房间名与 `room_id` 1:1**，不复用 `g_{groupId}` 作房间名，避免同一群连续两通电话串房；同群并发通话由「该群是否已有未结束房间」拦截。
- **服务端显式 `CreateRoom`**（而非依赖 `auto_create`），因为必须下发 9 人上限与超时策略，且建房失败可快速失败、不写库。
- **LiveKit 调用放在分布式锁之外**：`DistributedLockHelper` 的 lease 只有 10s（`RtcServiceImpl` L74/L118），锁内只做 DB 状态迁移与幂等判定。
- **在线真相以 LiveKit 为准，DB 只做审计与超时兜底**。
- `identity = String(uid)`（同 identity 新连接会顶掉旧连接，前端提示「已在其他设备加入通话」）；`name` 取昵称（群内优先群昵称）；`metadata` 存 `{uid,nickname,avatar}` 供前端兜底展示；token TTL 4 小时（覆盖重连，LiveKit JS SDK 无公开的 token 刷新事件）。
- `t_rtc_room.sfu_node` 存 LiveKit URL，作为未来多节点的路由锚点。

---

## 三、数据模型变更

`sql/chat.sql` 追加（**仅新增可空默认列，回滚只需回退代码**）：

```sql
ALTER TABLE t_rtc_room
    ADD COLUMN `channel_type` tinyint NOT NULL DEFAULT 1 COMMENT '频道类型 1-单聊 2-群聊' AFTER `channel_id`,
    ADD INDEX idx_channel (channel_id, channel_type);
```

`xy-common/src/main/java/com/xy/common/entity/chat/RtcRoom.java` 新增 `private Integer channelType;`。

字段最终语义：

| 字段 | 语义 |
|---|---|
| `room_id` | 房间 ID = LiveKit roomName |
| `room_type` | 1-语音 2-视频（群聊同样只用 1/2） |
| `host_uid` | 通话发起人（= 主持人） |
| `channel_id` | 1v1：对端 uid；群聊：`g_{groupId}` |
| `channel_type` | 1-单聊 2-群聊 |
| `max_participants` | 1v1=2；群聊=9 |
| `status` | 0-呼叫中（joined<2）/ 1-通话中（joined≥2）/ 2-已结束 |
| `start_time` / `end_time` | 首次 joined≥2 时刻 / 结束时刻 |

`t_rtc_participant` **不加字段**（`status` 的 0/1/2/3/4 已覆盖呼叫中/已加入/已离开/拒绝/忙线）。注意 `uk_room_user(room_id,u_id)` 唯一索引：**重复邀请/重新加入必须 UPDATE，不能 INSERT**。

---

## 四、后端接口契约（`RtcController`）

| 端点 | 请求 | 响应 data |
|---|---|---|
| `POST /rtc/call` | `CallDTO` | `CallVO`（含 `livekitUrl`/`token`/`participants`） |
| `POST /rtc/accept` | `RoomDTO{roomId}` | `AcceptVO`（含 token，幂等可重复调用） |
| `POST /rtc/reject` | `RoomDTO` | `null` |
| `POST /rtc/hangup` | `RoomDTO` | `null`（群聊=离开，1v1=挂断结束） |
| `POST /rtc/cancel` | `RoomDTO` | `null`（仅 CALLING 阶段、仅发起人） |
| `POST /rtc/invite` | `InviteDTO` | `InviteVO` |
| `POST /rtc/end` | `RoomDTO` | `null`（发起人或群主/管理员） |
| `POST /rtc/token` | `RoomDTO` | `TokenVO`（重连刷新） |
| `GET /rtc/room/{roomId}` | — | `RoomDetailVO` |
| `GET /rtc/health` | — | `{livekit:"up"\|"down"}` |
| ~~`GET /rtc/ice-servers`~~ | — | 删除（P5） |

DTO/VO：

```java
// CallDTO：1v1 走 calleeUid；群聊走 channelId + channelType=2 + inviteeUids
private Long calleeUid;
private String channelId;        // 群聊："g_9"，后端归一化为数字 groupId
private Integer channelType;     // 默认 1
private List<Long> inviteeUids;  // 不含自己，≤ maxParticipants-1
@NotNull private Integer mediaType;

// 新增 InviteDTO { @NotBlank String roomId; @NotEmpty List<Long> inviteeUids; }
// 新增 RtcMemberVO { Long uid; String nickname; String avatar; String groupNickname;
//                    Integer role; Integer status; LocalDateTime joinTime; }
// CallVO/AcceptVO/TokenVO 共同字段 { roomId, mediaType, channelType, channelId,
//                    livekitUrl, token, maxParticipants, participants[] }
```

---

## 五、后端改造清单

| 文件 | 动作 |
|---|---|
| `customer-api/fg-chat/pom.xml` | 新增 `io.livekit:livekit-server:0.16.0` |
| 根 `pom.xml` | 覆写 `<kotlin.version>2.3.0</kotlin.version>`（见风险 R1） |
| `.../config/RtcProperties.java` | 删除 `iceServers`；新增 `livekit{url,apiUrl,apiKey,apiSecret,tokenTtlSeconds,emptyTimeoutSeconds,departureTimeoutSeconds}`、`maxParticipants=9`、`maxCallMinutes=180` |
| `.../service/LivekitAdminService.java` + `impl/LivekitAdminServiceImpl.java` | **新增**：`ensureRoom(roomName,maxParticipants)` / `issueToken(roomName,uid,name,metadata,isRoomAdmin)` / `listParticipantIdentities(roomName)` / `closeRoom(roomName)` / `isUp()`；`RoomServiceClient` 单例常驻 |
| `.../service/impl/RtcServiceImpl.java` | 二元 → 多参与者模型改造（见下） |
| `.../service/IRtcService.java` | 新增 `invite/end/token/roomDetail/isLivekitUp`；`accept` 由 `void` 改返回 `AcceptVO`；删除 `getIceServers` |
| `.../controller/RtcController.java` | 新增 invite/end/token/room/health；删除 ice-servers |
| `.../task/RtcTimeoutTask.java` | 周期不变，`expireStaleRooms()` 内部改两阶段 |
| `.../pojo/dto|vo/*` | 按第四节新增/扩展 |
| `xy-common/.../entity/chat/RtcRoom.java` | 新增 `channelType` |

### `RtcServiceImpl` 逐方法改造要点

- **`call`**：`channelType==1` 走原逻辑（忙线检测 + 2 人）；`==2` 新增 `callGroup`：校验群存在且未解散、未全员禁言、调用者与所有 `inviteeUids` 均为群成员（`IGroupMemberService.getGroupMemberByUid`）、去重排除自己、人数不超限，并拦截「该群已有未结束房间」。顺序：生成 roomId → `ensureRoom` → 插 room + host participant(`status=1, role=1`) + N 条 invitee participant(`status=0`) → 逐个推 200。
- **`accept`**：改为返回 `AcceptVO`；参与者不存在则按「重新加入」UPDATE（兼容唯一索引）；**仅当 `joinedCount>=2` 时把房间 0→1 并写 `start_time`**；token 现场签发，可重复调用用于重连。
- **`reject`**：1v1 保持（房间结束 + `rejected` 记录）；群聊仅本人 `status=3`，房间继续，推 203，**不落记录**。
- **`hangup`**：1v1 保持现状（结束整房 + `completed/failed` 记录）；群聊 = 仅本人 `status=2`，剩余 JOINED>0 则房间继续并推 204，归零则结束整房 + 群记录。
- **`cancel`**：1v1 保持；群聊仅 `host_uid==uid` 且 `status=0` 时结束并广播 201。
- **`invite`（新）**：调用者须在房间内且 `status in (0,1)`，且为发起人或群 `owner/admin`；按唯一索引 UPDATE 或 INSERT；总人数 ≤ 9；逐个推 200。
- **`end`（新）**：仅发起人或群 `owner/admin`；1v1 不允许（用 hangup）。
- **幂等去重（关键）**：把 `endRoom()`（L373-378）改为返回布尔值的 `endRoomIfActive()`（`update ... .ne(status, ROOM_ENDED)` 后判断受影响行数），**只有「赢得迁移」的那一次调用才推送通话记录**，天然避免双方同时挂断导致重复落库。

### 群通话记录

- 沿用 `MessageType.rtcRecord(210)`，**前端 `MessageBox.vue` L70-71/L148-153 已按灰色居中胶囊渲染，无需改渲染**。
- 通过 `wk/ChatMessage.java` 的 `sendGroupMessage(groupId, null, MessageType.rtcRecord, text)` 发到群频道 `g_{groupId}`；它**不经过 `MessageServiceImpl.sendMessage`，因此不受群禁言校验影响**（权限须由我们显式校验）。
- 文案：`视频通话 · 3人 · 通话时长 05:12` / `语音通话 · 已取消` / `语音通话 · 未接听`；人数取 `join_time IS NOT NULL` 的 distinct uid 数。
- 触发者唯一化：最后一人离开 / 发起人结束 / 超时无人加入 / CALLING 阶段取消，均以 `endRoomIfActive()` 返回 true 为唯一触发条件。

### `expireStaleRooms()` 两阶段

- **阶段 A**（保留原语义）：`status=0` 且超时 → 结束 + 全员 LEFT + 向未离开者推 201 + 记录。为兼容「发起中又追加邀请」，超时基准取 `create_time` 与最近一条 participant `create_time` 的较晚者（用 `QueryWrapper.select("MAX(create_time)")` 即可，无需自定义 XML）。**不能用「存在 JOINED 参与者」作为跳过条件**（群房间必然有一条 host participant）。
- **阶段 B**（新增）：`status=1` 且 `start_time` 超过 `maxCallMinutes`（默认 180）→ 查 `listParticipantIdentities`：为空则结束并落记录；非空则仅修正 DB 中不在 LiveKit 的参与者为 LEFT。LiveKit 不可达时跳过并 WARN，避免误杀。

---

## 六、前端改造清单

| 文件 | 动作 |
|---|---|
| `src/services/rtc/livekit.ts` | **新增** `LiveKitSession` 类（下详） |
| `src/store/call.ts` | 重写为多参与者模型 |
| `src/components/CallOverlay/index.vue` | 宫格 + 邀请/结束/离开 + 音频 sink |
| `src/components/IncomingCallDialog/index.vue` | 群邀请文案 |
| `src/views/chat/components/ChatWindow/GroupCallMemberPicker.vue` | **新增**成员选择器 |
| `src/views/chat/components/ChatWindow/ChatWindow.vue` | 放开群聊入口 + `handleGroupAudioCall/VideoCall` |
| `src/views/friend/index.vue` | 适配 `startCall` 新签名（行为不变） |
| `src/api/rtc.ts` | 新增 `rtcInvite/rtcEnd/rtcToken/getRoomDetail/getRtcHealth`；删除 `getIceServers`（P5） |
| `src/services/rtc/signaling.ts` | 删除 `send`（P5）；`RtcSignalData` 扩展 `channelType/channelId/groupName/hostUid/inviteeCount/hangupUid`，删除 `sdp/candidate` |
| `src/services/im/constants.ts` | 新增 `isCallControlType()`（200-205）；**保留** `isRtcSignalType` 的 200-208 拦截范围（兼容旧版客户端，P5 再评估收窄） |
| `src/views/chat/components/ChatWindow/…` | 群通话记录会话名兜底（可选）：`store/chat.ts` `syncConversationFromMessage`（L520）在 `channelType===2` 时补 `findGroupInfo`，避免出现名为 `g_9` 的会话 |
| `services/rtc/webrtc.ts` | **P5 删除**（418 行） |

### `LiveKitSession` 对外形态

```ts
export interface ParticipantState {
  uid: string; name: string; avatar: string; isLocal: boolean
  audioMuted: boolean; videoMuted: boolean; hasVideo: boolean
  speaking: boolean; quality: 'excellent' | 'good' | 'poor' | 'unknown' | 'lost'
  invited: boolean   // 已邀请但未接入（仅群聊）
}

export class LiveKitSession {
  constructor(url: string, token: string, callbacks: {
    onParticipantsChanged(list: ParticipantState[]): void
    onConnectionState(s: 'connecting' | 'connected' | 'reconnecting' | 'disconnected'): void
    onDisconnected(reason: DisconnectReason): void
    onAudioBlocked(): void
    onError(err: unknown): void
  })
  connect(opts: { audio: boolean; video: boolean }): Promise<void>
  disconnect(): Promise<void>
  setMicEnabled(enabled: boolean): Promise<void>
  setCamEnabled(enabled: boolean): Promise<void>
  attachVideo(uid: string, el: HTMLVideoElement | null): void
  attachAudio(uid: string, el: HTMLAudioElement | null): void
  resumeAudio(): Promise<void>
  snapshot(): ParticipantState[]
}
```

`Room` 构造：`{ adaptiveStream: true, dynacast: true, publishDefaults: { simulcast: true }, videoCaptureDefaults: { resolution: VideoPresets.h720.resolution } }`。
事件映射：`ParticipantConnected/Disconnected`、`TrackSubscribed/Unsubscribed`、`TrackMuted/TrackUnmuted`（**静音状态由 LiveKit 自动同步，无需自研状态信令**）、`ActiveSpeakersChanged`、`ConnectionQualityChanged`、`ConnectionStateChanged/Reconnecting/Reconnected`、`Disconnected`。**不要使用 `ReconnectionStateChanged` / `TokenRefresh`（该版本不存在）**。

### `store/call.ts` 重写

- 状态机保留 `idle|outgoing|incoming|connecting|active|ended`；新增 `isGroupCall/channelId/channelType/groupName/hostUid/maxParticipants/participants[]`；删除 `connection/iceServers/pendingCandidates/createConnection/degraded/restoreVideo`。
- `startCall(target: {mode:'p2p',peer} | {mode:'group',channelId,groupName,inviteeUids}, type: 1|2)`：`POST /rtc/call` → 立刻 `joinRoom`（发起即进房，界面显示「等待成员加入…」）；回铃音仅 1v1 播放。
- `acceptCall()` → `POST /rtc/accept` 拿 token → `joinRoom`；**计时触发条件统一为「房间内在场人数 ≥2」**（替代原来依赖 202 信令）。
- `leaveRoom()`（群聊=离开 / 1v1=挂断）、`endRoom()`（host / 群主管理员）、`inviteMembers(uids)`、`toggleAudio/toggleVideo` → 改调 `setMicEnabled/setCamEnabled`；`attachVideo/attachAudio` 透传。
- `onSignal` 瘦身：删除 `RTC_OFFER/RTC_ANSWER/RTC_CANDIDATE` 三个分支（L365-407）；保留自回显过滤（L269-279）；`RTC_INVITE` 扩展解析群字段；`RTC_HANGUP` 群聊分支只把该成员标记为已离开并 toast，**不结束本地通话**。
- `endReason`/`statusText` 扩展：`hostEnded`(群主已结束通话) / `youLeft` / `noOneJoined` / `roomFull`；`outgoing`(群) → 「等待成员加入…」；新增「网络不稳定，重连中…」。

### `CallOverlay` 宫格

- 布局：1 人全屏（等待中）、2 人沿用现有「远端全屏 + 本地 PIP」观感、3-4 人 2×2、5-6 人 3×2、7-9 人 3×3。
- 每格：有视频则 `<video autoplay playsinline>`（`watch(() => p.hasVideo)` + `onMounted` 双保险挂载，并沿用现有 `flush:'post'` 经验）、无视频则头像+昵称兜底、左下静音角标、「邀请中…」角标、说话高亮边框、右上质量点。
- **远端音频必须挂到真实 DOM**：用一个隐藏容器 `audioSink`，对每个远端 `attachAudio(uid, el)`；首帧用户手势后调 `resumeAudio()`（应对自动播放策略）。
- 底部控制栏：静音 / 摄像头 / **邀请成员**（仅群聊，满员或非发起人/管理员时 disabled）/ **结束通话**（仅群聊且发起人或群主管理员）/ 挂断(离开)。顶栏显示 `群名 · N人` + 计时 + 网络角标。

### 群聊入口与成员选择器

- `ChatWindow.vue` header 下拉的 `v-if` 由 `channelType === 1` 放开为 `1 || 2`；已解散群（`channelInfo.disband === 1`）不放开。
- 群聊点击后打开 `GroupCallMemberPicker`（`modelValue/members/currentUid/max=8/disabledUids` → `confirm(uids)`）：头像 + `groupNickname||nickname`、搜索过滤、全选按钮、满员后其余置灰；确认后 `startCall({mode:'group', channelId: activeContact.uid, groupName, inviteeUids: uids}, type)`（群聊下 `activeContact.uid` 已是 `g_9`，无需再拼前缀）。
- 数据来源直接复用 `ChatWindow.vue` 已有的 `groupMembers`（`getGroupMembers()`，L218-224）。

---

## 七、部署与配置

- `application.yml`：删除 `xy.rtc.ice-servers`，新增

```yaml
xy:
  rtc:
    call-timeout-seconds: 90
    max-participants: 9
    max-call-minutes: 180
    livekit:
      url: ws://127.0.0.1:7880        # 下发给前端（生产 wss://）
      api-url: http://127.0.0.1:7880  # 服务端 RoomService（内网）
      api-key: devkey
      api-secret: secret
      token-ttl-seconds: 14400
      empty-timeout-seconds: 300
      departure-timeout-seconds: 20
```

- 新增 `deploy/livekit/docker-compose.yml` + `livekit.yaml`（`livekit/livekit-server`，`network_mode: host`，`rtc.use_external_ip: true`，UDP 端口段收敛为 50000-50100 便于放行，`room.max_participants: 9`，`turn.enabled: true`）。放行端口：`7880/tcp`（生产由 Nginx 反代 `wss://`）、`7881/tcp`、`50000-50100/udp`、`3478/udp`+`5349/tcp`。
- 前端**不需要新增 `VITE_*`**：`livekitUrl` 与 token 全部由后端 `call/accept/token` 下发（保持现有 3 个环境变量）。
- **追加到现有 `docs/rtc/deployment.md`**（不新建文档）：新增 LiveKit 部署章节；把「自建 coturn」「ICE 配置」「`/rtc/ice-servers` 自检」章节改为 LiveKit 对应内容；部署检查清单替换为 LiveKit 端口 + `wss` + `channel_type` 迁移项；标注旧版前端在 P5 后不可用。

---

## 八、实施阶段（每阶段可独立验证）

| 阶段 | 内容 | 验证 |
|---|---|---|
| **P1** 后端 LiveKit 基座（非破坏） | pom×2、application.yml、`RtcProperties`、`LivekitAdminService(Impl)`、`/rtc/health` | `mvn -pl customer-api/fg-chat -am compile -DskipTests`；`dependency:tree` 核对 kotlin/okhttp；启动后 `/rtc/health` 为 `up`；用 `/rtc/token` 的 token 在 LiveKit 官方示例页连入房间 |
| **P2** 1v1 迁移（**破坏性**） | SQL 迁移 + `RtcRoom.channelType`、`RtcServiceImpl` 1v1 路径、accept 返回 `AcceptVO`、`api/rtc.ts`、新增 `livekit.ts`、重写 `store/call.ts`、`CallOverlay` 2 人布局、`IncomingCallDialog`、两处单聊入口签名 | `vue-tsc -b --force`；两账号：呼叫→接听→互看画面→静音→挂断→双方出现 210 记录 |
| **P3** 群聊后端模型 | DTO/VO、`callGroup`/`invite`/`end`、hangup/reject/cancel 分流、`endRoomIfActive`、`expireStaleRooms` 两阶段、群记录推送与权限 | 扩展 `scripts/rtc-api-test.sh` 群用例（需第三账号+测试群）；断言 `channel_type=2`、群频道仅一条 210 |
| **P4** 群聊前端 UI | call store 群字段/邀请/结束、`CallOverlay` 宫格、`GroupCallMemberPicker`、`ChatWindow` 入口 | `vue-tsc`；三账号：3 人通话、2/3/4 人布局切换、选人上限、中途邀请、群主结束、发起人离开房间继续、最后一人离开群内出现记录 |
| **P5** 清理退役（**破坏性**） | 删除 `webrtc.ts`、`signaling.send`、`getIceServers/IceServer`、`/rtc/ice-servers`、`RtcProperties.iceServers`、yml 的 ice 配置；`rtc-api-test.sh` 与 `deployment.md` 更新 | 全局搜 `IceServer|ice-servers|getIceServers|RtcConnection|rtcSignalingService.send` 无残留；前后端编译；脚本全绿 |

> 破坏性说明：P2 起前端不再发 206-208、不再拉 ICE，**前后端需同版本发布**；P5 使旧版前端彻底不可用，建议单独小版本发布并提示强制刷新。

---

## 九、验证方案

```bash
# 后端（新增依赖首次需联网解析，不能用 -o）
mvn -q -pl customer-api/fg-chat -am compile -DskipTests
# 前端
cd y-micro-app/apps/y-chat-sub && npx vue-tsc -b --force
# 依赖复核
mvn -q -pl customer-api/fg-chat dependency:tree -Dincludes=org.jetbrains.kotlin:kotlin-stdlib,com.squareup.okhttp3:okhttp,com.google.protobuf:protobuf-java
```

前端依赖通过 pnpm catalog 声明：`y-micro-app/pnpm-workspace.yaml` 的 `catalog` 加 `livekit-client: ^2.21.0`，`apps/y-chat-sub/package.json` 用 `"livekit-client": "catalog:"` 引用后 `pnpm install`。

`scripts/rtc-api-test.sh` 调整：用例 1「ICE 配置」改为 `/rtc/health` + `/rtc/token`；用例 3 增加 `data.livekitUrl`/`data.token` 非空断言；新增群通话用例（call 群聊 → invite 追加 → 非管理员 `end` 应失败 → `end` 后 DB `status=2`）；DB 断言补 `channel_type` 与 joined 计数。

人工联调清单：1v1 语音/视频（含静音角标同步、连续两通）；拒绝 / 呼叫中取消 / 90s 超时；忙线；群聊 3 人宫格、中途邀请、发起人离开房间继续、群主结束、最后一人离开落记录；弱网（DevTools 限速 + 断网 10s 观察 `reconnecting→reconnected` 与自适应降级）；同账号双设备顶号提示；通话中刷新页面用 `/rtc/room/{roomId}` + `/rtc/token` 重新入房。

---

## 十、风险与缓解

| # | 风险 | 缓解 |
|---|---|---|
| R1 | **kotlin-stdlib 冲突**：LiveKit SDK 需 2.3.0，Spring Boot 3.2.4 的 kotlin-bom 钉在 1.9.23 → 启动 `NoSuchMethodError` | 根 `pom.xml` 覆写 `<kotlin.version>2.3.0</kotlin.version>`；`dependency:tree -Dincludes=org.jetbrains.kotlin:kotlin-stdlib` 确认唯一；P1 验收含启动自检 |
| R2 | **okhttp 版本冲突**：SDK 带 okhttp-bom 5.3.2，fg-chat 直接声明 4.12.0，且悟空 `WuKongClient` 也走 okhttp | 先跑 `dependency:tree`；二选一（排除 SDK 的 okhttp-bom 保持 4.12.0，或统一升 5.x）；回归「发一条普通 IM 消息 + 一次群记录推送」 |
| R3 | **LiveKit 端口/公网 IP 配错**：信令通但无媒体（连上不出画面） | `rtc.use_external_ip: true`、放行 UDP 50000-50100 与 7881/tcp、`wss` 反代；以双机实测出画面为上线门禁（`/rtc/health` 只证明 7880 通，不能替代媒体连通性验证） |
| R4 | **同 identity 顶号**：多端登录/旧页面未关导致偶发掉线 | `identity=uid` 单端策略 + 收到 `DUPLICATE_IDENTITY` 明确提示；多端并存需改 `identity={uid}_{deviceId}`，本期不做并写入文档 |
| R5 | **qiankun/Tauri 下的媒体限制**：自动播放策略、`attach` 元素生命周期、Tauri 权限 | 远端音频挂真实 DOM + 首帧手势 `resumeAudio()`；`flush:'post'` 挂载；Tauri `Info.plist` 已声明权限（改后需重启 dev）；本期不做屏幕共享 |
| R6 | 群频道 210 记录导致会话名为 `g_9` | `store/chat.ts` 的 `syncConversationFromMessage`（L520）在 `channelType===2` 时补 `findGroupInfo` 补全群名/头像 |

**回滚点**：R1/R2 在 P1 编译/启动即暴露且无破坏；P2 的 DB 变更只是新增默认列，回退代码即可（列保留无害）；P5 前旧链路（`webrtc.ts` + 206-208 + coturn）始终可 `git revert` 恢复。

---

## 十一、可复用的既有实现（不要重写）

- 呼叫控制锁与幂等：`utils/DistributedLockHelper.tryLock`
- 群频道系统消息：`wk/ChatMessage.sendGroupMessage`（不受群禁言校验）
- 群成员与权限：`IGroupMemberService.getGroupMemberByUid` + `pojo/enums/GroupMemberRole`
- 210 记录渲染：`components/MessageBox/MessageBox.vue`（灰底居中胶囊，含 `isTipMessage`）
- 铃声：`services/rtc/ringtone.ts` 的 `ringtoneEngine`
- 群成员数据与加载：`ChatWindow.vue` 的 `groupMembers` + `getGroupMembers()`
- 全局浮层挂载点：`layout/index.vue`（`<call-overlay />` / `<incoming-call-dialog />` 同级新增即可）
- 组件自动注册：`src/components` 下新建组件即全局可用（`ChatWindow/` 下的需显式 import）
