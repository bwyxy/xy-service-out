# 踩坑实录：给即时通讯做 1v1 音视频通话，我踩的 7 个坑

> 项目背景：一套基于 Spring Boot + Vue3 + 悟空 IM（WuKongIM）的即时通讯系统，需要新增 1v1 语音/视频通话能力。
> 本文只讲**踩过的坑**和**为什么这么设计**，不讲 WebRTC 基础 API。

## 0. 整体架构：三平面拆分

动手前先把一件事想清楚：**WebRTC 只管媒体面，信令面和控制面必须自己设计。**

最终落成三个平面：

| 平面 | 职责 | 实现方式 |
| --- | --- | --- |
| 控制面 | 发起/接听/拒接/挂断/取消、超时兜底、通话记录 | REST 接口 `/rtc/call \| accept \| reject \| hangup \| cancel`、`/rtc/ice-servers` |
| 信令面 | SDP offer/answer、ICE candidate 交换 | **复用悟空 IM 长连接**，靠消息类型码区分 |
| 媒体面 | 音视频流传输 | P2P，DTLS-SRTP 加密，ICE/STUN/TURN 打洞 |

呼叫状态机：

```
idle → outgoing → connecting → active → ended
         ↘ incoming ↗
```

为什么控制面要单独走 REST 而不是全塞进 IM？因为**控制指令需要权威落库 + 幂等**，而信令只是"管道里的数据"，不需要持久化。这个拆分是后面所有设计的地基。

---

## 坑 1：类型码冲突，系统通知弹了一脸

**现象**：通话刚开始，双方疯狂弹出系统通知，内容是 `{"type":206,...}` 这种原始 JSON。

**定位**：前端 IM 消息分发里有一行约定：

```ts
const isSystemMessageType = (type: number) => type >= 100
```

凡是 `>= 100` 的类型，都会走"系统消息"分支，被渲染成通知气泡。而通话信令恰好被分配在 **200-210** 区间：

```
200 invite / 201 cancel / 202 accept / 203 reject / 204 hangup
205 busy   / 206 offer  / 207 answer / 208 candidate / 210 rtcRecord
```

于是每条 offer/answer/candidate 都被当成"系统通知"弹了出来。一次通话要交换几十条 ICE candidate，用户直接被通知刷屏。

**根因**：新增类型码时只看了"码段有没有被占用"，没看**前端分发逻辑对码段的隐含语义假设**。

**解法**：在 message-handler 里把 RTC 信令做**最前置拦截**——在 `isSystemMessageType` 判断之前就分流出去：

```
// message-handler.ts 分发顺序（顺序即优先级）
RTC_RECORD(210) → FORCE_LOGOUT(220) → 200~208 RTC 信令 → isSystemMessageType(>=100) → 普通聊天
```

**教训**：
> 复用一条通用消息通道时，**新增类型码必须回到"分发器"看一圈**。你以为你只是加了个枚举，实际上你是在修改一个已经存在语义假设的状态机的输入域。

---

## 坑 2：noPersist 信令离线就丢，通话卡死在"呼叫中"

**现象**：A 呼叫 B，B 当时网络抖动掉线 3 秒，恢复后 A 一直显示"正在呼叫…"，永远不会超时也不会失败。

**定位**：信令为了不污染聊天记录，发了 `noPersist=1`。但悟空 IM 的 `noPersist` 意味着**只在线投递，离线直接丢弃，不进离线消息队列**。

于是：
- B 掉线时，A 发的 `invite` 丢了 → B 根本不知道有人呼叫；
- 或者 B 发的 `answer` 丢了 → A 永远停在 `outgoing`。

**根因**：信令面是"仅在线可达"的（best-effort），但我们把它当成了可靠通道来用。信令丢了之后，**双方状态机各自停在半路，谁都不会主动收敛**。

**解法**：加一个 **90 秒超时任务** `RtcTimeoutTask`，到点未进入 `active` 就强制走 `hangup` 收敛状态：

```ts
const CALL_TIMEOUT_SECONDS = 90
// 超时兜底：直接本地收敛，不等对端确认
```

**教训**：
> 用"不可靠通道"传信令，就**必须有一个主动收敛的兜底定时器**。不能指望对端一定会回包。

---

## 坑 3：状态在 DB 和前端 store 各说各话

**现象**：偶发"幽灵通话"——前端显示 `idle`，但后端 DB 里这通电话还是 `active`；再拨打时报"对方正在通话中"。

**定位**：状态实际上存了两份：
- 后端：`rtc_call_record` 表 + 内存态；
- 前端：`call.ts` 里的 `status = ref<CallStatus>('idle')`。

网络乱序下，两条状态更新路径会不一致：比如 B 的 `reject` 先于 A 的 `cancel` 到达，两边各自更新，就岔开了。

**根因**：分布式状态下，"两端各自维护一份状态"必然会分叉。**能救的只有：让所有状态变更都可重入、可幂等、可被对端兜底覆盖。**

**解法**：
1. 所有控制面接口**全幂等**——重复调用 `hangup` 不报错，只做状态收敛；
2. 信令 `content` 里**携带 `roomId`**，收到任何信令时先校验 `roomId` 是否属于当前通话，不匹配直接丢弃（防止上一通电话的迟到信令污染当前通话）；
3. 前端 `hangupCall()` 同时**清本地定时器 + 通知后端**，不做"乐观更新后等确认"。

**教训**：
> 通话状态是**多端共享状态**。凡是共享状态，变更接口就必须幂等 + 带会话标识。迟到信令一定要能被识别并丢弃。

---

## 坑 4：`restartIce()` 死循环重连

**现象**：弱网下 ICE 频繁 failed，代码里 `oniceconnectionstatechange` 检测到 failed 就 `pc.restartIce()`，结果**疯狂重连**，CPU 飙满、耗电爆炸。

**定位**：

```ts
// webrtc.ts
if (iceState === 'failed') {
  this.pc.restartIce()   // 每次 failed 都触发
}
```

网络持续不通时，restart 后再 failed，再 restart……形成风暴。

**解法**：**只重试一次**，用一个标志位锁住：

```ts
private restartRetried = false
// failed 且未重试过 → restartIce() + 置位
// 已重试过仍 failed → 放弃，走降级/挂断
```

**教训**：
> 任何"失败后自动重试"的逻辑，都必须有**次数上限或退避策略**。否则弱网会把它变成 DoS 自己的攻击。

---

## 坑 5：弱网不该"卡成 PPT"，应该"降级成对讲机"

**现象**：网络差的时候，画面卡成幻灯片，声音断续，体验还不如直接打电话。

**设计**：加一个 **媒体质量监控 + 自动降级**：

```ts
startStatsMonitor() {
  // 每 5s 拉一次 getStats()
  // 采集：rtt、lossRate、bytesSent、bitrate
  // bitrate = ((bytesSent - lastBytesSent) * 8) / (now - lastStatsTime)
}
```

判定阈值：**丢包率 > 15% 或 RTT > 500ms** → 触发降级。

降级动作非常轻——不是重新协商 SDP，而是**直接把视频轨从 sender 上摘掉**：

```ts
degradeVideo() {
  this.savedVideoTrack = sender.track       // 先存好
  await sender.replaceTrack(null)           // 再摘掉
}
restoreVideo() {
  await sender.replaceTrack(this.savedVideoTrack)  // 网络恢复，装回去
}
```

`replaceTrack` 的好处是**不需要重新协商、不打断连接**，秒级完成，通话不断。

**坑点**：`sender.replaceTrack(null)` 后，**远端**会触发 `track.onmute`。如果远端 UI 没处理 `onmute`，对方画面上会留一个"冻住"的最后一帧，看起来像卡死。所以远端必须监听 `onmute/onunmute`，主动切成"对方已关闭摄像头"的占位图。

**教训**：
> 弱网体验的核心不是"想办法保住画质"，而是**优雅降级**。降级路径要短、要无损、要能自动恢复——`replaceTrack` 比重新协商适合得多。

---

## 坑 6：谁来采集？被叫在接听后采集并发 offer

**现象**：一开始让主叫 `getUserMedia` 后发 offer，结果"还没接听，摄像头就亮了"——用户体验极差，还容易被当成偷拍。

**设计**：把"采集时机"绑到"接听动作"上。

主时序：

```
A                        后端                        B
|-- POST /rtc/call ------>|                          |
|<-- 200 (roomId) --------|-- IM 200 invite -------->|
|   status=outgoing       |                          |  status=incoming
|                         |<-- POST /rtc/accept -----|
|<-- IM 202 accept -------|<-- 200 OK ---------------|
|  getUserMedia           |                          |  getUserMedia
|<-- IM 206 offer --------|                          |  (接听后采集)
| setRemote + answer      |                          |
|-- IM 207 answer ------->|------------------------->|
|  ...ICE candidate 双向交换（208）...              |
|================== P2P 媒体流（DTLS-SRTP）==================|
```

关键点：**B 是在点了"接听"之后才 `getUserMedia` 的**，然后 B 发 offer、A 回 answer（谁是 offerer 不重要，重要的是采集发生在用户明确同意之后）。

**教训**：
> 摄像头/麦克风的 `getUserMedia` 调用时机 = **用户明确同意的那个动作**。别在呼叫发起时就采集，那不是技术问题，是信任问题。

---

## 坑 7：Tauri 桌面端调不出摄像头

**现象**：Web 端通话正常，但打包成 Tauri 桌面应用后，`getUserMedia` 直接失败。

**根因**：macOS 沙盒下，**应用必须声明摄像头/麦克风用途**，否则系统直接拒绝。Tauri 打包的 app 默认没有这些声明。

**解法**：在 `Info.plist` 里补上：

```xml
<key>NSCameraUsageDescription</key>
<string>用于视频通话</string>
<key>NSMicrophoneUsageDescription</key>
<string>用于语音通话</string>
```

如果之前已经拒绝过一次授权，macOS 会**记住拒绝状态**，改配置也不弹窗。需要重置权限缓存：

```bash
tccutil reset Camera com.xy.chat.desktop
tccutil reset Microphone com.xy.chat.desktop
```

然后重启 `tauri dev` 才会重新弹授权框。

**教训**：
> 桌面端 WebRTC 的坑一半在**系统权限声明**上，不在代码里。改完 `Info.plist` 记得 `tccutil reset`，否则你会对着"代码明明没问题"抓半天头。

---

## 小结：一张"该想清楚"的清单

做 WebRTC 1v1，真正难的不是 `RTCPeerConnection` 的 API，而是这些**分布式 + 时序 + 权限**的边界：

1. **类型码**要和现有分发器的语义假设对齐（否则通知弹满脸）。
2. **不可靠信令**必须有超时兜底收敛。
3. **共享状态**的变更接口必须幂等 + 带会话标识。
4. **重试**必须有上限（`restartIce` 别死循环）。
5. **弱网**要设计降级路径（`replaceTrack` 比重新协商香）。
6. **采集时机**绑到用户同意动作（接听后才开摄像头）。
7. **桌面端权限**是系统级问题（`Info.plist` + `tccutil`）。

把这 7 条填平，一通稳定的 1v1 通话才算真正跑通。