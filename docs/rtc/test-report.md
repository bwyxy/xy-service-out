# 1v1 音视频通话系统测试报告

**测试日期**：2026-10-14
**代码版本**：feature/rtc-call（后端 fg-chat + 前端 y-chat-sub）
**测试环境**：macOS（开发机），fg-chat 9080（IDEA 启动），MySQL 8 + Redis + 悟空 IM 本地/联调环境

---

## 1. 静态与构建验证（已完成）

| 项目 | 命令 | 结果 |
|------|------|------|
| 后端编译 | `mvn -pl customer-api/fg-chat -am compile` | **通过**（两次，含全部 RTC 新增类） |
| 前端类型检查 | `pnpm vue-tsc -b` | **通过**（无 RTC 相关类型错误） |
| 前端构建 | `pnpm build` | **通过**（`✓ built in 3.20s`，chunk 体积正常：element-plus 767KB 等） |
| 测试脚本语法 | `bash -n scripts/rtc-api-test.sh` | **通过** |

覆盖文件：后端 12 个（MessageType 枚举、RtcController/RtcServiceImpl/RtcTimeoutTask/RtcProperties、2 Mapper、2 DTO、2 VO、application.yml）+ 前端 14 个 + `src-tauri/Info.plist`。

## 2. API 自动化测试（脚本已就绪，待 fg-chat 重启后执行）

脚本：`scripts/rtc-api-test.sh`（curl + python3 断言，无需额外依赖）

```bash
ACCOUNT_A=<主叫手机号> PASSWORD_A=<密码> \
ACCOUNT_B=<被叫手机号> PASSWORD_B=<密码> \
./scripts/rtc-api-test.sh

# 启用数据库状态断言（可选）
MYSQL_CMD="mysql -h127.0.0.1 -uroot -p<密码> <chat库名>" \
ACCOUNT_A=... PASSWORD_A=... ACCOUNT_B=... PASSWORD_B=... ./scripts/rtc-api-test.sh
```

### 用例清单

| # | 用例 | 断言要点 | 结果 |
|---|------|----------|------|
| 0 | 双账号登录 | 两账号 code=200，token/uid 提取成功 | 待执行 |
| 1 | GET /rtc/ice-servers | code=200，ICE 列表非空 | 待执行 |
| 2 | 呼叫自己 | 业务失败，msg 含"不能呼叫自己" | 待执行 |
| 3 | 正常流程（视频） | call→roomId；accept→200；hangup→200；**A 重复挂断幂等**；对已结束房间接听幂等；接听不存在房间幂等 | 待执行 |
| 4 | 拒绝流程 | B reject→200；重复 reject 幂等 | 待执行 |
| 5 | 主叫取消 | A cancel→200；取消后 B 再接听幂等 | 待执行 |
| 6 | 忙线检测 | A 呼 B 振铃中，B 反呼 A → 失败且 msg 含"忙线" | 待执行 |
| 7 | 重复呼叫 | A 振铃中再次呼 B → 被拒（忙线/通话中） | 待执行 |
| 8 | 房间重建 | 结束后再次 call 成功且生成新 roomId | 待执行 |
| 9 | 数据库状态（可选） | 上述房间 status 均流转为 2（已结束），参与者状态符合状态机 | 待执行 |

**前置条件**：fg-chat 需在 IDEA 中重启加载 RTC 新代码（旧进程 PID 96498 未包含新 Controller）。

## 3. 手动联调清单（双端，待执行）

### 3.1 功能测试

| # | 场景 | 操作 | 预期 |
|---|------|------|------|
| F1 | 语音通话主流程 | A 聊天窗点语音 → B 接听 → 通话 → A 挂断 | A 回铃（450Hz 响1s停3s）；B 弹窗响铃（440+480Hz）；接通后双向语音清晰；双端显示计时；聊天流出现"语音通话 · 通话时长 mm:ss"灰色胶囊 |
| F2 | 视频通话主流程 | 同上（视频） | 双向视频流畅；本地 PIP 可与远端互换；计时/挂断正常；记录文案"视频通话 · 通话时长 mm:ss" |
| F3 | 拒绝 | A 呼 B → B 点红拒绝 | A 显示"对方已拒绝"；B 无残留弹窗；记录"已拒绝" |
| F4 | 主叫取消 | A 呼 B → A 挂断（取消） | B 弹窗立即关闭；A 显示"已取消呼叫" |
| F5 | 被叫不接听超时 | A 呼 B → B 不动 | A 前端 90s 超时自动取消；后端定时任务兜底结束房间；双端"未接听"记录 |
| F6 | 忙线 | A↔B 通话中，C 呼 A | C 收"对方忙线"toast |
| F7 | 通话中控制 | 静音/关摄像头切换 | 对端相应流静音/黑屏（远端 track onmute 触发头像舞台） |
| F8 | 弱网降级 | Network Link Conditioner 限速（如 3G/高丢包） | 丢包>15% 或 RTT>500ms 时视频自动降级为纯语音 + 顶部提示；网络恢复后可点"恢复视频" |
| F9 | 断线重连 | 通话中短暂断网（关开 WiFi <5s） | `connectionstate=failed` → offer 方 restartIce 自动恢复；无法恢复则自动挂断，不卡死 |
| F10 | 通话记录红点 | 结束通话后观察会话列表 | 210 记录有红点、可定位；实时信令（200-208）不产生任何红点/消息残留 |
| F11 | 离线被叫 | B 未登录 A 呼 B | 房间 90s 超时结束，A 收"未接听"（悟空 IM 离线消息机制保证 B 上线后可见记录） |
| F12 | 好友页入口 | 好友详情点"语音聊天/视频聊天" | 与聊天窗入口行为一致 |

### 3.2 性能观测（通话中 chrome://webrtc-internals）

| 指标 | 目标 |
|------|------|
| 端到端时延（P2P 直连） | < 200ms（同城/同网 < 50ms） |
| 视频码率 | 1-2.5 Mbps（720p） |
| 音频码率 | ~40 kbps（Opus） |
| 丢包率 | 良好网络 < 1%；>5% 触发 medium 角标；>15% 自动降级 |
| RTT | 良好 < 100ms；>200ms medium；>500ms poor |
| 弱网恢复 | 降级后网络恢复，手动恢复视频成功率 100% |

### 3.3 兼容性

| 端 | 覆盖 | 结果 |
|----|------|------|
| Chrome（Web，最新两个大版本） | 主流程 F1-F10 | 待执行 |
| Safari（Web，https） | 主流程 + 权限弹窗 | 待执行 |
| macOS 桌面端（Tauri） | 首次通话弹摄像头/麦克风授权（Info.plist 文案）；红绿灯窗口下浮层正常 | 待执行（**需重启 tauri dev 使 Info.plist 生效**） |
| Linux 桌面端（deb/appimage） | 主流程 | 待执行 |
| Windows 桌面端 | 主流程 + 自定义窗口按钮 | 待执行 |

### 3.4 安全测试

| # | 场景 | 预期 |
|---|------|------|
| S1 | 无 token 调 /rtc/* | 业务码 401 |
| S2 | B 用 A 的 roomId 调 accept（B 非参与者） | 幂等无效果（参与者状态校验） |
| S3 | webrtc-internals 查看 SRTP | 媒体全程 DTLS-SRTP 加密 |
| S4 | 伪造 candidate（roomId 不匹配） | 前端信令处理前 roomId 校验丢弃 |

## 4. 已知限制与说明

1. **P2P 打通率**：默认仅 Google STUN，无 TURN 时对称型 NAT 下可能连接失败（表现为 connecting 超时后自动挂断）。生产环境按 deployment.md 部署 coturn 后可达 ~100%。
2. **同时同账号多端登录**：信令按 uid 推送，多端同时在线会同时收到来电信令，当前以先到先处理为准，其余端在房间结束后自然复位。
3. **浏览器 http 限制**：非 localhost 的 http 页面浏览器禁止 getUserMedia，Web 端必须 https；桌面端（Tauri）不受限。
4. **超时任务多实例**：`expireStaleRooms()` 无分布式排他锁，但操作幂等，多实例重复执行无害（见 deployment.md 2.4）。

## 5. 结论

- 静态检查、类型检查、构建全部通过，代码可交付联调。
- API 自动化脚本与手动清单已备齐；**待 fg-chat 重启 + tauri dev 重启后执行第 2、3 节，回填"结果"列**。
- 建议回归顺序：先跑自动化脚本（1-2 分钟），再做 F1-F12 手动主流程，最后弱网/兼容性抽测。
