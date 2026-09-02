# 飞鸽即时通讯（IM）完整解决方案

> 版本：v1.0\
> 适用范围：xy-service 项目群聊、私聊、音视频通话能力升级\
> 基于现状：Spring Boot 3.2.4 + Vue 3 + 悟空 IM（WukongIM）+ MySQL 8 + Redis

***

## 1. 项目现状与升级目标

### 1.1 现有能力

当前 `fg-chat` 模块已基于 **悟空 IM** 实现基础即时通讯能力：

- 用户体系：`t_user` 表独立管理聊天用户，与 `sys_user` 隔离。
- 认证授权：Spring Security 6 + JWT，聊天用户 username 格式为 `chat_{uId}`。
- 单聊与群聊基础：悟空 IM 频道（channel）承载消息收发；前端通过 `easyjssdk`/`wukongimjssdk` 连接 WebSocket。
- 消息转发：发送消息统一走 `/message/send` HTTP 接口，由后端转发至悟空 IM，便于内容校验。
- 会话与历史消息：通过 `/conversation/list`、`/message/list` 同步悟空 IM 会话并补充本地元数据。
- 富文本消息：支持文本、Emoji、图片，图片上传到七牛云。
- 好友与通知：`t_friend`、`t_friend_request`、`t_notification` 已落地，支持好友请求与系统通知。

### 1.2 待补齐能力

| 领域  | 当前状态       | 目标能力                             |
| --- | ---------- | -------------------------------- |
| 群聊  | 仅有基础频道与成员表 | 创建/解散、成员管理、禁言、公告、权限分级、群消息类型扩展    |
| 私聊  | 基础文本/图片收发  | 已读回执、消息撤回、语音/文件消息、聊天窗口状态记忆       |
| 音视频 | 无          | 1v1/多人语音、1v1/多人视频、设备管理、弱网优化、通话状态 |

### 1.3 设计原则

1. **复用现有通道**：继续以悟空 IM 作为消息传输与信令通道，避免重复建设长连接网关。
2. **能力域拆分**：群聊管理、私聊增强、RTC 媒体转发作为独立能力域，代码按包/模块隔离。
3. **最小侵入**：新增表与接口，不破坏 `t_user`、JWT、悟空 IM 连接等现有流程。
4. **可扩展性**：消息类型采用 `type + content` 自描述 JSON，RTC 媒体服务支持水平扩展。
5. **安全第一**：传输层 TLS/WSS，消息端到端可选加密，敏感操作权限校验。

***

## 2. 系统架构设计

### 2.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    客户端层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Web (Vue3)  │  │   Android    │  │     iOS      │  │   小程序/H5   │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼─────────────────┼─────────────────┼─────────────────┼────────────┘
          │                 │                 │                 │
          ▼                 ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   接入网关层                                  │
│                    Nginx / Spring Cloud Gateway (SSL 终结)                   │
└─────────────────────────────────────────────────────────────────────────────┘
          │                 │                 │                 │
          ▼                 ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                   业务服务层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   fg-chat    │  │   xy-system  │  │   xy-admin   │  │    xy-ai     │    │
│  │  (端口 9080)  │  │  (端口 8080)  │  │  (端口 9090)  │  │  (端口 9070)  │    │
│  │ · 群聊管理    │  │  · 用户/权限  │  │  · 群管理后台 │  │  · AI 助手   │    │
│  │ · 私聊增强    │  │  · 文件存储   │  │  · 消息审计  │  │              │    │
│  │ · 消息转发    │  │  · 悟空 IM Cl │  │              │  │              │    │
│  │ · 呼叫信令    │  │              │  │              │  │              │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼─────────────────┼─────────────────┴─────────────────┴────────────┘
          │                 │
          │                 ▼
          │   ┌──────────────────────────────────────────────┐
          │   │              悟空 IM 集群 (WukongIM)            │
          │   │  · 长连接网关 (WebSocket/TCP)                  │
          │   │  · 消息路由/投递                               │
          │   │  · 会话/历史消息存储                            │
          │   │  · 音视频信令中转                              │
          │   └──────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────────────────┐
│           RTC 媒体服务 (WebRTC SFU)            │
│  ┌──────────────────────────────────────────┐ │
│  │  mediasoup / Janus Gateway 集群           │ │
│  │  · 媒体流转发 (SFU)                        │ │
│  │  · ICE/STUN/TURN 协调                      │ │
│  │  · 动态码率/弱网适配                       │ │
│  └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                    数据层                                    │
│   MySQL 8 (业务数据)    Redis (缓存/锁/在线状态)    七牛云 OSS (媒体文件)      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

#### 2.2.1 服务端模块

| 模块          | 职责       | 说明                                     |
| ----------- | -------- | -------------------------------------- |
| `fg-chat`   | IM 业务能力层 | 群聊管理、私聊增强、消息转发、已读/撤回、RTC 信令调度          |
| `xy-system` | 系统能力层    | 用户/权限/文件、悟空 IM Client 封装、七牛云上传         |
| `xy-admin`  | 管理后台     | 群组审核、成员管理、消息审计、系统公告                    |
| `xy-ai`     | AI 能力层   | AI 助手对话（已存在，可扩展为群机器人）                  |
| RTC Service | 独立媒体服务   | 基于 mediasoup/Janus 的 WebRTC SFU，处理音视频流 |

#### 2.2.2 客户端模块

```
y-micro-app/apps/y-chat-sub
├── services/
│   ├── im/                    # 悟空 IM 连接与消息处理（已存在）
│   ├── group/                 # 群聊管理 API + 状态
│   ├── chat/                  # 私聊增强（已读、撤回、语音、文件）
│   └── rtc/                   # WebRTC 通话模块（新增）
├── views/
│   ├── chat/                  # 聊天主界面
│   ├── group/                 # 群资料、群管理、群公告
│   ├── friend/                # 好友关系
│   └── call/                  # 语音/视频通话界面
└── store/
    ├── chat.ts                # 聊天状态（已存在，扩展）
    ├── group.ts               # 群状态
    └── rtc.ts                 # 通话状态
```

### 2.3 核心交互流程

#### 2.3.1 群聊创建流程

```
用户A (群主)
   │
   ▼
POST /api/group/create
   │
   ▼
fg-chat Service
   │ ① 校验用户身份、创建群名
   │ ② 写 t_group / t_group_member（owner）
   ▼
WukongIM API
   │ ③ 创建群频道 channel_type=2
   ▼
返回 { groupId, channelId }
```

#### 2.3.2 消息收发流程（沿用并扩展）

```
Sender          fg-chat             WukongIM           Receiver
  │               │                    │                  │
  │--POST /message/send--------------->│                  │
  │ {channelId,type,content,clientMsgNo}│                │
  │               │--sendMessage()--->│                  │
  │               │                    │--WebSocket push->│
  │               │                    │                  │
  │               │<----ack------------│                  │
  │<-R.ok()-------│                    │                  │
```

#### 2.3.3 音视频呼叫流程

```
Caller                    fg-chat (Signaling)              WukongIM              Callee
  │                              │                              │                  │
  │--POST /rtc/call/initiate--->│                              │                  │
  │ {calleeUid, type: audio|video, roomId}                     │                  │
  │                              │--send cmd/signaling msg---->│                  │
  │                              │                              │--push----------->│
  │                              │                              │   incoming call  │
  │                              │                              │<--accept/reject--│
  │                              │<--ack------------------------│                  │
  │                              │                              │                  │
  │<--返回 roomToken + sfuAddr----│                              │                  │
  │                              │                              │                  │
  │======== 双方连接 RTC SFU (WebRTC) =========>                 │                  │
  │                              │                              │                  │
```

***

## 3. 技术选型说明

### 3.1 通信协议选型

| 协议/技术                 | 用途        | 选型理由                                       |
| --------------------- | --------- | ------------------------------------------ |
| WebSocket (WSS)       | IM 长连接    | 悟空 IM 原生支持，浏览器/移动端兼容好，已在前端 `easyjssdk` 中使用 |
| HTTP/REST             | 业务接口      | 与现有 Spring Boot 体系一致，便于限流、鉴权、日志、Swagger 文档 |
| WebRTC                | 实时音视频媒体传输 | 浏览器原生支持，无需插件，端到端加密，社区生态成熟                  |
| SFU (mediasoup/Janus) | 多人会议媒体转发  | SFU 不解码混合，CPU 低、延迟低，适合多人视频                 |
| STUN/TURN             | NAT 穿透    | WebRTC 必备；TURN 服务器 Coturn 开源稳定             |
| SDP / ICE             | 媒体协商与网络候选 | WebRTC 标准流程                                |

### 3.2 实时数据传输方案

| 数据类型           | 传输通道                   | 说明                    |
| -------------- | ---------------------- | --------------------- |
| 文本/图片/文件消息     | 悟空 IM + 后端转发           | 图片/文件先上传七牛云，消息体存 URL  |
| 语音消息           | 悟空 IM + 七牛云 URL        | 前端录制音频文件上传，消息 type=3  |
| 已读回执/撤回        | 悟空 IM 自定义消息 / cmd 消息   | 轻量信令，不走业务接口           |
| 群公告/禁言/踢人      | HTTP 业务接口 + 悟空 IM 系统消息 | 权限变更后向群内广播            |
| 音视频媒体流         | WebRTC SFU             | 不经过悟空 IM，由独立 RTC 服务转发 |
| 呼叫信令（邀请/接听/挂断） | 悟空 IM cmd 消息           | 复用长连接，降低延迟            |

### 3.3 音视频处理库/服务选型

#### 方案 A：mediasoup（推荐，性能优先）

| 维度 | 说明                                       |
| -- | ---------------------------------------- |
| 架构 | Node.js + C++ Worker，SFU                 |
| 优势 | 单核性能高（约 150 路 720p），支持 Simulcast/SVC，延迟低 |
| 劣势 | 需自行实现信令与房间管理，开发成本高                       |
| 适用 | 对并发与延迟要求高、团队有 Node.js/C++ 能力的场景          |

#### 方案 B：Janus Gateway（快速落地）

| 维度 | 说明                                         |
| -- | ------------------------------------------ |
| 架构 | C 语言插件化 WebRTC 网关                          |
| 优势 | 功能插件丰富（VideoRoom、AudioBridge、SIP），文档完善，上手快 |
| 劣势 | 单进程架构，高并发扩展相对复杂                            |
| 适用 | 快速原型、中小规模、需要插件扩展的场景                        |

#### 建议

- **第一阶段（MVP）**：使用 **Janus VideoRoom** 插件快速实现 1v1/多人通话。
- **第二阶段（规模化）**：迁移或并行使用 **mediasoup**，获得更高并发与更细粒度的码率控制。

### 3.4 前端技术栈补充

| 能力         | 技术/库                                       | 说明             |
| ---------- | ------------------------------------------ | -------------- |
| WebRTC 客户端 | 浏览器原生 `RTCPeerConnection` + `getUserMedia` | 无需额外库          |
| 通话状态管理     | Pinia `rtc.ts`                             | 与现有 store 体系一致 |
| 音频录制       | `MediaRecorder` + Web Audio API            | 录制语音消息         |
| 视频 UI      | 原生 `<video>` + CSS Grid                    | 多人会议布局         |
| 设备枚举       | `navigator.mediaDevices.enumerateDevices`  | 摄像头/麦克风切换      |

***

## 4. 核心功能实现方案

### 4.1 群聊系统

#### 4.1.1 功能清单

| 功能     | 说明                        |
| ------ | ------------------------- |
| 群创建/解散 | 普通成员可创建群；群主/管理员可解散        |
| 成员管理   | 邀请、移除、禁言、设置管理员、转让群主       |
| 群公告    | 群主/管理员发布，置顶展示，新成员入群可见     |
| 群消息    | 文本、图片、文件、语音、@某人、引用回复      |
| 群权限    | 分级：群主(2) > 管理员(1) > 成员(0) |
| 历史记录   | 新成员入群后可配置是否可见历史消息         |

#### 4.1.2 关键实现

**创建群**

```java
@PostMapping("/api/group/create")
public R<GroupVO> createGroup(@RequestBody @Valid CreateGroupDTO dto) {
    Long ownerUid = SecurityUtils.getUId();
    Group group = groupService.createGroup(ownerUid, dto.getName(), dto.getMemberUids());
    // 同步创建悟空 IM 群频道
    wukongIMService.createChannel(String.valueOf(group.getGroupId()), 2);
    wukongIMService.addSubscribers(String.valueOf(group.getGroupId()), 2,
            dto.getMemberUids().stream().map(String::valueOf).toArray(String[]::new));
    return R.ok(GroupVO.from(group));
}
```

**成员禁言**

- 在 `t_group_member` 增加 `mute_end_time` 字段。
- 发送群消息前校验：若 `mute_end_time > now()` 且用户非管理员，拒绝发送并返回错误码。
- 禁言变更通过 HTTP 接口更新 DB，并发送系统消息到群内通知。

**群公告**

- 新增 `t_group_announcement` 表：标题、内容、发布人、置顶标志、创建时间。
- 发布公告后向群频道发送一条 type=10 的系统消息，payload 携带公告 ID。
- 前端收到后拉取最新公告详情展示。

#### 4.1.3 权限矩阵

| 操作      | 群主 | 管理员     | 成员       |
| ------- | -- | ------- | -------- |
| 发送消息    | ✓  | ✓       | 未被禁言时 ✓  |
| 邀请成员    | ✓  | ✓       | 群设置允许时 ✓ |
| 移除成员    | ✓  | ✓       | ✗        |
| 设置管理员   | ✓  | ✗       | ✗        |
| 禁言成员    | ✓  | ✓（除群主外） | ✗        |
| 发布/编辑公告 | ✓  | ✓       | ✗        |
| 解散群     | ✓  | ✗       | ✗        |

### 4.2 私聊系统

#### 4.2.1 功能清单

| 功能       | 说明                       |
| -------- | ------------------------ |
| 已读状态     | 单聊消息显示“已读/未读”            |
| 消息撤回     | 发送后 2 分钟内可撤回，双方界面同步删除/替换 |
| 聊天记录同步   | 换设备登录后拉取云端历史             |
| 消息类型扩展   | 文本、图片、语音、文件、位置、名片        |
| 聊天窗口状态记忆 | 记住输入框草稿、滚动位置、已选表情        |

#### 4.2.2 已读回执实现

- 消息体中增加 `needReadReceipt: boolean` 字段。
- 当接收方打开聊天窗口时，前端调用 `/message/read` 批量上报已读 messageSeq。
- 后端通过悟空 IM 向发送方推送 type=101 的已读回执消息，payload 为 `{channelId, messageSeqs}`。
- 前端根据回执更新本地消息状态。

```java
@PostMapping("/message/read")
public R<?> readMessages(@RequestBody ReadMessageDTO dto) {
    Long uid = SecurityUtils.getUId();
    // 1. 校验频道权限（好友关系或群成员）
    // 2. 记录已读水位线到 Redis：read_receipt:{channelId}:{uid}
    // 3. 向消息发送方推送已读回执
    messageService.sendReadReceipt(dto.getChannelId(), dto.getChannelType(), dto.getMessageSeqs());
    return R.ok();
}
```

#### 4.2.3 消息撤回实现

- 撤回窗口期：2 分钟（业务可配置）。
- 前端发送 type=102 的撤回指令消息到频道。
- 后端校验：消息发送者是否为当前用户、是否在窗口期内。
- 校验通过后，向频道内所有订阅者广播 type=102 的撤回通知，payload `{originalMessageSeq, originalClientMsgNo}`。
- 前端收到后替换原消息为“消息已撤回”。

```java
@PostMapping("/message/recall")
public R<?> recallMessage(@RequestBody RecallMessageDTO dto) {
    Long uid = SecurityUtils.getUId();
    // 1. 从悟空 IM 或本地缓存查询原消息发送者
    // 2. 校验：发送者 == uid 且未超期
    // 3. 广播撤回指令
    messageService.broadcastRecall(dto.getChannelId(), dto.getChannelType(),
            dto.getMessageSeq(), dto.getClientMsgNo());
    return R.ok();
}
```

#### 4.2.4 语音消息实现

1. 前端通过 `MediaRecorder` 录制音频（推荐 MP3/Opus）。
2. 上传七牛云，返回 URL。
3. 发送 type=3 消息，payload `{url, duration, format}`。
4. 接收方点击播放，使用 `<audio>` 或 Web Audio API。

#### 4.2.5 文件消息实现

1. 前端选择文件，上传到七牛云（限制大小/类型）。
2. 后端可异步进行病毒扫描或敏感文件检测。
3. 发送 type=4 消息，payload `{url, name, size, mimeType}`。
4. 接收方点击下载/预览。

#### 4.2.6 聊天窗口状态记忆

- 草稿：`localStorage` 键 `chat_draft:{channelId}`。
- 滚动位置：`sessionStorage` 键 `chat_scroll:{channelId}`。
- 已选表情/常用表情：持久化到 `localStorage`。

### 4.3 语音/视频聊天系统

#### 4.3.1 功能清单

| 功能        | 说明                        |
| --------- | ------------------------- |
| 1v1 语音/视频 | 发起、接听、拒绝、忙线、挂断、超时取消       |
| 多人语音/视频   | 创建房间、邀请加入、主持人控制、成员列表      |
| 设备管理      | 摄像头开关、麦克风开关、扬声器切换、前后置摄像头  |
| 通话质量      | 动态码率、分辨率降级、丢包重传、网络监测      |
| 通话状态      | 呼叫中、振铃中、通话中、已挂断、已拒绝、忙线、超时 |
| 弱网优化      | 自动降级至音频、启用 TURN、降低分辨率     |

#### 4.3.2 RTC 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                         Caller / Callee                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   getUserMedia│  │ RTCPeerConnection│  │  Signaling (WS/cmd)│  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼─────────────────┼───────────────────┼────────────┘
          │                 │                   │
          ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                        RTC Signaling Server                  │
│                     (Spring Boot / Node.js)                  │
│  · 房间管理    · 成员状态    · 信令转发    · Token 校验        │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                        RTC Media Server (SFU)                │
│                   mediasoup / Janus Gateway                  │
│  · Producer/Consumer  · Router  · ICE/STUN/TURN  · Simulcast │
└─────────────────────────────────────────────────────────────┘
```

#### 4.3.3 呼叫状态机

```
          发起呼叫
            │
            ▼
      ┌──────────┐
      │ Calling  │
      └────┬─────┘
           │ 对方接听
           ▼
      ┌──────────┐
      │ Connected│
      └────┬─────┘
           │ 挂断 / 异常
           ▼
      ┌──────────┐
      │  Finished │
      └──────────┘

分支：
· 对方拒绝 → Rejected
· 对方忙线 → Busy
· 超时未接 → Timeout
· 网络断开 → Disconnected（允许重连）
```

#### 4.3.4 1v1 通话流程

1. **发起**：Caller 调用 `/rtc/call/initiate`，后端生成 `roomId`、`roomToken`。
2. **推送**：通过悟空 IM cmd 消息向 Callee 发送 `{type: 'call_invite', roomId, callerUid, callType}`。
3. **接受**：Callee 点击接听，调用 `/rtc/call/accept`，后端返回 SFU 地址与 Token。
4. **连接**：双方通过 SFU 建立 WebRTC 连接。
5. **挂断**：任意一方调用 `/rtc/call/hangup` 或发送 cmd 消息，后端通知另一方并释放房间。

#### 4.3.5 多人会议流程

1. **创建房间**：主持人调用 `/rtc/room/create`，设置最大人数、是否开启视频、是否静音入会。
2. **邀请成员**：主持人选择成员，后端通过悟空 IM 发送邀请 cmd 消息。
3. **成员加入**：被邀请者调用 `/rtc/room/join`，SFU 返回连接参数。
4. **媒体订阅**：每个参与者发布自己的音视频，按需订阅其他参与者（SFU  selective forwarding）。
5. **主持人控制**：主持人可 mute/unmute 其他成员、踢人、锁定房间。

#### 4.3.6 弱网优化策略

| 策略           | 实现方式                                                                   |
| ------------ | ---------------------------------------------------------------------- |
| 动态码率         | WebRTC `RTCRtpSender.setParameters` 根据 `googAvailableSendBandwidth` 调整 |
| 分辨率降级        | 从 720p → 480p → 360p 阶梯降级                                              |
| 帧率调整         | 从 30fps → 15fps                                                        |
| 仅音频模式        | 视频卡顿严重时自动关闭视频发送，保留音频                                                   |
| TURN 中继      | 配置 Coturn，P2P 失败时走中继                                                   |
| NACK/PLI/FIR | SFU 与客户端开启丢包重传、关键帧请求                                                   |
| 网络监测         | 定时读取 `RTCStatsReport`，展示上下行码率、丢包率、RTT                                  |

***

## 5. 数据模型设计

### 5.1 数据表新增/扩展

#### 5.1.1 群聊相关

**t\_group（扩展）**

```sql
ALTER TABLE t_group
    ADD COLUMN `notice` varchar(500) DEFAULT '' COMMENT '群简介/公告摘要',
    ADD COLUMN `max_member` int DEFAULT 500 COMMENT '最大成员数',
    ADD COLUMN `join_type` tinyint DEFAULT 0 COMMENT '加群方式 0-邀请 1-扫码 2-自由加入',
    ADD COLUMN `invite_permission` tinyint DEFAULT 1 COMMENT '邀请权限 0-仅管理员 1-所有成员',
    ADD COLUMN `speak_permission` tinyint DEFAULT 1 COMMENT '发言权限 0-仅管理员 1-所有成员',
    ADD COLUMN `history_visible` tinyint DEFAULT 1 COMMENT '新成员是否可见历史消息 0-不可见 1-可见';
```

**t\_group\_member（扩展）**

```sql
ALTER TABLE t_group_member
    ADD COLUMN `role` tinyint DEFAULT 0 COMMENT '角色 0-成员 1-管理员 2-群主',
    ADD COLUMN `mute_end_time` datetime DEFAULT NULL COMMENT '禁言结束时间',
    ADD COLUMN `group_nickname` varchar(50) DEFAULT '' COMMENT '群内昵称',
    ADD COLUMN `last_ack_seq` bigint DEFAULT 0 COMMENT '已读到的最大消息seq';
```

**t\_group\_announcement（新增）**

```sql
CREATE TABLE t_group_announcement (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    `group_id` bigint NOT NULL COMMENT '群组ID',
    `title` varchar(100) DEFAULT '' COMMENT '标题',
    `content` text COMMENT '内容',
    `publisher_uid` bigint NOT NULL COMMENT '发布人',
    `is_pinned` tinyint DEFAULT 1 COMMENT '是否置顶',
    `create_time` datetime COMMENT '创建时间',
    `update_time` datetime COMMENT '更新时间',
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群公告表';
```

#### 5.2.2 私聊/消息相关

**t\_private\_message\_extra（新增）**

```sql
CREATE TABLE t_private_message_extra (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `channel_id` varchar(64) NOT NULL COMMENT '频道ID',
    `channel_type` tinyint NOT NULL COMMENT '频道类型 1-个人 2-群',
    `message_seq` bigint NOT NULL COMMENT '消息seq',
    `from_uid` bigint NOT NULL COMMENT '发送者',
    `to_uid` bigint COMMENT '接收者（单聊）',
    `is_recall` tinyint DEFAULT 0 COMMENT '是否已撤回',
    `recall_time` datetime DEFAULT NULL COMMENT '撤回时间',
    `read_count` int DEFAULT 0 COMMENT '已读人数',
    `extra` json COMMENT '扩展字段',
    `create_time` datetime COMMENT '创建时间',
    UNIQUE INDEX uk_channel_seq (channel_id, channel_type, message_seq),
    INDEX idx_from_uid (from_uid),
    INDEX idx_to_uid (to_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息扩展信息';
```

> 说明：核心消息内容仍由悟空 IM 存储，`t_private_message_extra` 仅保存业务扩展字段（撤回、已读计数等），避免重复存储。

**t\_conversation\_setting（新增）**

```sql
CREATE TABLE t_conversation_setting (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `u_id` bigint NOT NULL COMMENT '用户ID',
    `channel_id` varchar(64) NOT NULL COMMENT '频道ID',
    `channel_type` tinyint NOT NULL COMMENT '频道类型',
    `mute` tinyint DEFAULT 0 COMMENT '是否免打扰',
    `top` tinyint DEFAULT 0 COMMENT '是否置顶',
    `draft` varchar(500) DEFAULT '' COMMENT '草稿',
    `create_time` datetime COMMENT '创建时间',
    `update_time` datetime COMMENT '更新时间',
    UNIQUE INDEX uk_user_channel (u_id, channel_id, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话个人设置';
```

#### 5.2.3 RTC 通话相关

**t\_rtc\_room（新增）**

```sql
CREATE TABLE t_rtc_room (
    `room_id` varchar(64) PRIMARY KEY COMMENT '房间ID',
    `room_type` tinyint NOT NULL COMMENT '房间类型 1-语音 2-视频 3-会议',
    `host_uid` bigint NOT NULL COMMENT '房主/主持人',
    `channel_id` varchar(64) COMMENT '关联悟空IM频道',
    `max_participants` int DEFAULT 2 COMMENT '最大人数',
    `status` tinyint DEFAULT 0 COMMENT '状态 0-呼叫中 1-通话中 2-已结束',
    `start_time` datetime COMMENT '开始时间',
    `end_time` datetime COMMENT '结束时间',
    `sfu_node` varchar(255) COMMENT '分配的SFU节点',
    `create_time` datetime COMMENT '创建时间',
    INDEX idx_status (status),
    INDEX idx_host_uid (host_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTC房间';
```

**t\_rtc\_participant（新增）**

```sql
CREATE TABLE t_rtc_participant (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `room_id` varchar(64) NOT NULL COMMENT '房间ID',
    `u_id` bigint NOT NULL COMMENT '用户ID',
    `role` tinyint DEFAULT 0 COMMENT '角色 0-普通 1-主持人',
    `status` tinyint DEFAULT 0 COMMENT '状态 0-呼叫中 1-已加入 2-已离开 3-拒绝 4-忙线',
    `join_time` datetime COMMENT '加入时间',
    `leave_time` datetime COMMENT '离开时间',
    `audio_muted` tinyint DEFAULT 0 COMMENT '是否静音',
    `video_muted` tinyint DEFAULT 0 COMMENT '是否关闭视频',
    `create_time` datetime COMMENT '创建时间',
    UNIQUE INDEX uk_room_user (room_id, u_id),
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTC房间参与者';
```

### 5.2 核心数据结构

#### 5.2.1 消息 Payload 统一格式

```json
{
  "type": 1,
  "content": "文本内容",
  "extra": {
    "clientMsgNo": "abc123",
    "atUids": [10001, 10002],
    "replyToSeq": 0
  }
}
```

**type 枚举**

| 值   | 类型                  | 说明    |
| --- | ------------------- | ----- |
| 1   | TEXT                | 文本    |
| 2   | IMAGE               | 图片    |
| 3   | AUDIO               | 语音    |
| 4   | FILE                | 文件    |
| 5   | LOCATION            | 位置    |
| 6   | CARD                | 名片    |
| 10  | GROUP\_ANNOUNCEMENT | 群公告   |
| 100 | RTC\_INVITE         | 音视频邀请 |
| 101 | READ\_RECEIPT       | 已读回执  |
| 102 | RECALL              | 消息撤回  |
| 103 | RTC\_ACCEPT         | 接听    |
| 104 | RTC\_REJECT         | 拒绝    |
| 105 | RTC\_HANGUP         | 挂断    |
| 106 | RTC\_BUSY           | 忙线    |

***

## 6. 安全策略

### 6.1 用户认证机制

| 层级       | 机制                  | 说明                                   |
| -------- | ------------------- | ------------------------------------ |
| 业务接口     | JWT（`token` Header） | 复用现有 `JwtTokenProvider`，过期 24h，刷新 7d |
| 悟空 IM 连接 | UID + Token         | 登录时刷新悟空 IM Token，与 JWT 绑定            |
| RTC 房间   | 短期 Token            | 房间 Token 有效期 5 分钟，单次使用               |
| 文件上传     | 七牛云上传 Token         | 后端签发带过期时间的上传凭证                       |

### 6.2 消息加密方案

| 场景   | 方案                  | 说明                                            |
| ---- | ------------------- | --------------------------------------------- |
| 传输层  | TLS 1.3 / WSS       | 所有 WebSocket 与 HTTP 均走 HTTPS/WSS              |
| 消息内容 | Base64 编码 + 可选端到端加密 | 当前使用 Base64；敏感场景可引入 Signal Protocol / AES-GCM |
| 媒体流  | DTLS-SRTP           | WebRTC 原生加密                                   |
| 文件存储 | 七牛云私有空间 + 签名 URL    | 防止直链泄露                                        |

### 6.3 权限控制逻辑

- 所有群操作在 Service 层通过 `GroupPermissionChecker` 校验角色。
- 使用 Spring Security `@PreAuthorize` 注解标注管理员接口。
- 敏感接口（解散群、移除成员、禁言）使用 Redis 分布式锁防止并发覆盖。
- 文件上传校验 MIME 类型与大小，禁止可执行文件。

***

## 7. 性能优化方案

### 7.1 高并发扩展策略

| 层面      | 策略                                                  |
| ------- | --------------------------------------------------- |
| 业务服务    | `fg-chat` 多实例部署，Nginx 负载均衡，无状态设计                    |
| 悟空 IM   | 集群部署，多节点分担长连接；利用其去中心化 Raft 自动扩容                     |
| RTC SFU | 按房间哈希分配到不同 SFU 节点；单节点满载时自动迁移新房间                     |
| 数据库     | 读写分离；`t_private_message_extra` 按 `channel_id` 分表或分区 |
| 缓存      | Redis 缓存用户在线状态、群成员列表、已读水位线                          |

### 7.2 消息推送优化

- 利用悟空 IM 的离线消息与多设备同步能力，避免业务层重复实现。
- 已读回执批量上报，降低请求频率（每 3 秒或离开窗口时上报一次）。
- 群消息 @提醒通过 cmd 消息单独推送，避免全量消息解析。
- 免打扰会话不弹本地通知，但消息仍入库。

### 7.3 音视频通话质量保障

| 措施    | 实现                                   |
| ----- | ------------------------------------ |
| 就近接入  | SFU 节点按地域部署，DNS/Anycast 路由到最近节点      |
| 码率自适应 | WebRTC GCC + mediasoup Simulcast     |
| 丢包恢复  | NACK、FEC、RTX                         |
| 抖动缓冲  | 动态 Jitter Buffer                     |
| 回声消除  | 浏览器 AEC + AGC + NS                   |
| 通话监测  | 每 2s 采集 `RTCStatsReport`，上报丢包/码率/RTT |

***

## 8. 测试计划

### 8.1 功能测试用例

| 模块  | 用例          | 预期结果                    |
| --- | ----------- | ----------------------- |
| 群聊  | 创建群并邀请成员    | 群创建成功，成员收到系统消息，频道可收发消息  |
| 群聊  | 群主禁言普通成员    | 被禁言成员发送消息失败，解禁后恢复       |
| 群聊  | 发布群公告       | 群成员收到公告系统消息，置顶展示        |
| 私聊  | 发送消息后对方打开窗口 | 发送方看到“已读”状态更新           |
| 私聊  | 2 分钟内撤回消息   | 双方消息列表显示“消息已撤回”         |
| 私聊  | 发送语音/文件     | 接收方可正常播放/下载             |
| 音视频 | 1v1 语音通话    | 建立成功，双向音频清晰，挂断状态同步      |
| 音视频 | 多人视频会议      | 3 人以上可正常视频，主持人可 mute 成员 |
| 音视频 | 弱网环境通话      | 自动降级为音频，通话不中断           |

### 8.2 性能测试指标

| 指标             | 目标值       | 测试方法         |
| -------------- | --------- | ------------ |
| 单聊消息端到端延迟      | < 200ms   | 同机房客户端抓包     |
| 群消息 1000 人在线投递 | < 1s      | 压测工具模拟       |
| 历史消息拉取 100 条   | < 300ms   | API 压测       |
| 1v1 视频通话延迟     | < 250ms   | WebRTC stats |
| 多人会议 16 路 720p | CPU < 60% | SFU 服务器监控    |
| 悟空 IM 长连接并发    | 按官方指标     | 官方压测报告       |

### 8.3 兼容性测试范围

- **浏览器**：Chrome / Edge / Firefox / Safari（最新 2 个主版本）。
- **移动端**：Android WebView、iOS WKWebView、微信内置浏览器。
- **桌面端**：Electron（可选）。
- **网络环境**：Wi-Fi、4G/5G、弱网（高丢包、高延迟）。

***

## 9. 部署方案

### 9.1 环境配置要求

| 环境   | 配置                                                               |
| ---- | ---------------------------------------------------------------- |
| 开发环境 | 单机部署：MySQL 8 + Redis 6 + 悟空 IM 单机 + fg-chat + Janus 单机           |
| 测试环境 | 双节点：业务服务双实例 + 悟空 IM 双节点 + Janus 双实例 + 独立 TURN                    |
| 生产环境 | 多节点：业务服务多实例 + 悟空 IM 集群 + mediasoup 集群 + Redis Cluster + MySQL 主从 |

### 9.2 服务部署架构（生产）

```
                              ┌─────────────┐
                              │   CDN / OSS  │
                              │  (七牛云)    │
                              └──────┬──────┘
                                     │
   Internet ──▶ Nginx (SSL/LB) ──▶  fg-chat 集群
                                     │
                     ┌───────────────┼───────────────┐
                     ▼               ▼               ▼
               悟空 IM 集群      RTC SFU 集群      xy-system/xy-admin
                     │               │
                     ▼               ▼
                MySQL 8         Redis Cluster
              (主从+读写分离)    (缓存/会话/锁)
```

### 9.3 关键端口

| 服务        | 端口                              | 说明                                |
| --------- | ------------------------------- | --------------------------------- |
| fg-chat   | 9080                            | 业务 API                            |
| 悟空 IM     | 5000/5100/5200                  | HTTP API / TCP / WebSocket（按官方默认） |
| Janus     | 8088 (HTTP) / 10000-20000 (RTP) | 信令与媒体端口范围                         |
| mediasoup | 3000 (信令) / 10000-10100 (RTP)   | 按 Worker 配置                       |
| Coturn    | 3478 (STUN/TURN) / 5349 (TLS)   | NAT 中继                            |

### 9.4 容器化建议

- 悟空 IM、Janus、Coturn 均提供官方 Docker 镜像，建议统一使用 Docker Compose / Kubernetes 部署。
- `fg-chat` 构建为 JAR 镜像，通过环境变量注入数据库、Redis、悟空 IM、RTC 配置。
- 使用 Prometheus + Grafana 监控悟空 IM、RTC SFU、业务服务指标。

***

## 10. 关键功能技术验证思路

### 10.1 群聊管理验证

1. 调用 `/api/group/create` 创建群，检查 `t_group`、`t_group_member` 与悟空 IM 频道一致性。
2. 禁言成员后尝试发送消息，验证服务端拦截与前端提示。
3. 发布群公告后，新登录成员可见最新公告。

### 10.2 已读/撤回验证

1. 单聊发送消息，接收方不打开窗口时显示“未读”；打开后发送方显示“已读”。
2. 已读回执批量上报 3 次，确认无重复推送。
3. 发送消息后 1 分钟撤回，双方列表替换为“消息已撤回”；超过 2 分钟撤回失败。

### 10.3 音视频通话验证

1. 本地启动 Janus，配置 STUN/TURN。
2. 前端实现 `RTCPeerConnection`，通过 `/rtc/call/initiate` 获取房间信息。
3. 验证 1v1 视频通话说双向音视频正常，通话状态同步。
4. 使用 Clumsy/Network Link Conditioner 模拟 20% 丢包，验证自动降级为音频。

### 10.4 弱网优化验证

1. 监控 `RTCStatsReport` 中 `packetsLost`、`jitter`、`availableOutgoingBitrate`。
2. 在带宽骤降时，确认编码码率与分辨率阶梯下降。
3. P2P 失败后，确认连接切换到 TURN 服务器。

***

## 11. 实施路线图

| 阶段   | 周期  | 内容                            |
| ---- | --- | ----------------------------- |
| 第一阶段 | 2 周 | 群聊增强：创建/解散、成员管理、禁言、公告、权限      |
| 第二阶段 | 2 周 | 私聊增强：已读回执、消息撤回、语音/文件消息、会话设置   |
| 第三阶段 | 3 周 | 音视频通话：RTC 服务搭建、1v1 语音/视频、多人会议 |
| 第四阶段 | 2 周 | 弱网优化、性能测试、兼容测试、生产部署           |

***

## 12. 附录：接口清单（新增）

### 12.1 群聊接口

| 接口                                          | 方法       | 说明         |
| ------------------------------------------- | -------- | ---------- |
| `/api/group/create`                         | POST     | 创建群        |
| `/api/group/{groupId}/disband`              | POST     | 解散群        |
| `/api/group/{groupId}/members`              | GET/POST | 群成员列表/邀请成员 |
| `/api/group/{groupId}/members/{uid}/remove` | POST     | 移除成员       |
| `/api/group/{groupId}/members/{uid}/mute`   | POST     | 禁言/解禁      |
| `/api/group/{groupId}/announcement`         | POST/GET | 发布/获取公告    |
| `/api/group/{groupId}/setting`              | PUT      | 群设置        |

### 12.2 私聊/消息接口

| 接口                      | 方法      | 说明     |
| ----------------------- | ------- | ------ |
| `/message/read`         | POST    | 已读回执上报 |
| `/message/recall`       | POST    | 撤回消息   |
| `/conversation/setting` | GET/PUT | 会话个人设置 |

### 12.3 RTC 接口

| 接口                                | 方法   | 说明     |
| --------------------------------- | ---- | ------ |
| `/rtc/call/initiate`              | POST | 发起通话   |
| `/rtc/call/accept`                | POST | 接听通话   |
| `/rtc/call/reject`                | POST | 拒绝通话   |
| `/rtc/call/hangup`                | POST | 挂断通话   |
| `/rtc/room/create`                | POST | 创建会议房间 |
| `/rtc/room/{roomId}/join`         | POST | 加入房间   |
| `/rtc/room/{roomId}/leave`        | POST | 离开房间   |
| `/rtc/room/{roomId}/participants` | GET  | 房间成员   |

***

*文档结束*
