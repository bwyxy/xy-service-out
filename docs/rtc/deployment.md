# 音视频通话系统部署文档（LiveKit SFU）

> 媒体面已由自研 P2P 迁移到 **自建 LiveKit SFU**，单聊/群聊共用一套媒体层；
> 呼叫控制信令（200-205）仍复用悟空 IM 个人频道。
> 旧版前端（依赖 `206-208` 信令与 `/rtc/ice-servers`）在本次发布后**不可用**，需强制刷新。

## 1. 环境要求

| 组件 | 要求 |
|------|------|
| JDK | 17+（fg-chat 为 Spring Boot 3.2.4） |
| MySQL | 8.x，已执行 `sql/chat.sql`（含 `t_rtc_room` / `t_rtc_participant` 建表与 `channel_type` 迁移） |
| Redis | 用于分布式锁与在线状态 |
| 悟空 IM | 已部署，fg-chat 通过 Feign `WuKongClient` 推送信令 |
| LiveKit | 自建 `livekit/livekit-server`（见第 3 节），生产需公网可达 |
| Node.js | 18+（y-micro-app 前端构建，pnpm） |
| 浏览器/桌面端 | 需支持 WebRTC 与 `getUserMedia`（Chrome/Edge/Safari 近两年版本）；**http 非 localhost 下浏览器禁止采集音视频，必须走 https 或桌面端** |

## 2. 后端部署（fg-chat）

### 2.1 配置

`customer-api/fg-chat/src/main/resources/application.yml`：

```yaml
xy:
  rtc:
    # 呼叫超时（秒）：超时未接听自动结束并通知双方
    call-timeout-seconds: 90
    # 通话人数上限（1v1 固定 2 人，群聊取该值）
    max-participants: 9
    # 单次通话最长时长（分钟），超时由定时任务兜底结束
    max-call-minutes: 180
    livekit:
      # 下发给前端的地址（生产使用 wss:// 并由 Nginx 反代）
      url: ${LIVEKIT_URL:ws://127.0.0.1:7880}
      # 服务端 RoomService 调用地址（通常为内网）
      api-url: ${LIVEKIT_API_URL:http://127.0.0.1:7880}
      api-key: ${LIVEKIT_API_KEY:devkey}
      api-secret: ${LIVEKIT_API_SECRET:secret}
      # 接入 token 有效期（秒），需覆盖断线重连
      token-ttl-seconds: 14400
      # 房间空置自动关闭（秒）
      empty-timeout-seconds: 300
      # 参与者掉线宽限（秒）
      departure-timeout-seconds: 20
```

`livekit.api-key` / `api-secret` 必须与 LiveKit 服务端 `keys` 一致。

可通过环境变量覆盖（Spring 宽松绑定）：`LIVEKIT_URL`、`LIVEKIT_API_URL`、`LIVEKIT_API_KEY`、`LIVEKIT_API_SECRET`、`XY_RTC_MAX_PARTICIPANTS`、`XY_RTC_CALL_TIMEOUT_SECONDS` 等。

### 2.2 数据库迁移

`sql/chat.sql` 中 `t_rtc_room` 已新增列（升级已有库需手动执行）：

```sql
ALTER TABLE t_rtc_room
    ADD COLUMN `channel_type` tinyint NOT NULL DEFAULT 1 COMMENT '频道类型 1-单聊 2-群聊' AFTER `channel_id`,
    ADD INDEX idx_channel (channel_id, channel_type);
```

### 2.3 构建与启动

```bash
# 编译
mvn -pl customer-api/fg-chat -am clean package -DskipTests

# 启动（默认端口 9080，context-path 为 /）
java -jar customer-api/fg-chat/target/fg-chat-*.jar
```

开发环境在 IDEA 中直接启动主类即可。**注意：新增了 LiveKit 依赖与 RTC 代码（Controller/Service/Mapper/枚举/定时任务），老进程必须重启才能生效。**

### 2.4 启动自检

```bash
# 未登录应返回 401 业务码
curl -s http://localhost:9080/rtc/health
# 登录后（token 替换为真实 JWT）
curl -s -H "token: <jwt>" http://localhost:9080/rtc/health
# 期望: {"code":200,"data":{"livekit":"up"}}
```

> `/rtc/health` 只能证明后端到 LiveKit 的 7880 信令连通，**不能替代媒体连通性验证**（见 3.4）。

### 2.5 定时任务

`RtcTimeoutTask` 每 10 秒执行一次，`expireStaleRooms()` 分两阶段：

- **阶段 A**：`status=0`（呼叫中）且超时 → 结束房间、全员置为已离开、向未离开者推送取消（201）、落通话记录；
- **阶段 B**：`status=1`（通话中）且超过 `max-call-minutes` → 查询 LiveKit 在线参与者，为空才结束并落记录；LiveKit 不可达时跳过并告警，避免误杀。

多实例部署时任务非分布式锁排他，但内部按房间幂等（仅未结束的房间才更新，以受影响行数判定唯一触发者），重复执行无害。

## 3. LiveKit 生产部署（Docker + Caddy，替代原 coturn）

单机部署由两个容器组成：`livekit`（SFU）与 `caddy`（TLS 终结，自动申请并续期证书）。
两者都使用 **host 网络**，因为都需要直接占用宿主机端口与 UDP 段；容器间通过 `127.0.0.1` 互访（Caddy → LiveKit:7880）。

仓库已提供完整可用的生产配置，整体拷到服务器 `/opt/livekit/` 即可：

| 文件 | 作用 |
|------|------|
| [`deploy/livekit/docker-compose.yml`](../deploy/livekit/docker-compose.yml) | 服务编排、日志轮转、ulimit |
| [`deploy/livekit/livekit.yaml`](../deploy/livekit/livekit.yaml) | SFU 全部参数（端口段、公网 IP、房间上限、TURN） |
| [`deploy/livekit/Caddyfile`](../deploy/livekit/Caddyfile) | `wss://` 入口与证书自动化 |

### 3.1 落地前必须改的 3 处

**① 域名**：`Caddyfile` 里的 `livekit.example.com`（`email` 与站点块两处），改成你的域名，A 记录指向本机公网 IP。Caddy 会自动完成 ACME 校验并签发证书，无需人工干预。

**② API 密钥**（`livekit.yaml` 的 `keys` 段）：

```bash
echo "LIVEKIT_API_KEY=$(openssl rand -hex 12)"
echo "LIVEKIT_API_SECRET=$(openssl rand -hex 32)"
```

也可以用官方工具生成（输出直接就是 `key: secret` 形式，省去手工拼接）：

```bash
docker run --rm livekit/livekit-server:latest generate-keys
```

用输出替换 `keys:` 下的占位键值对（格式 `API_KEY: API_SECRET`），随后 `chmod 600 livekit.yaml`。
这两个值必须与后端环境变量完全一致；沿用示例值等于任何人都能自行签发进房令牌。

**③ 公网 IP 探测**：默认 `rtc.use_external_ip: true` 即可。若主机有多张网卡、或 STUN 探测到错误 IP（症状：信令通但始终无媒体），改为显式指定 `rtc.node_ip: <公网IP>`。

### 3.2 启动

```bash
mkdir -p /opt/livekit && cd /opt/livekit
# 放入上述三个文件，改完域名与密钥后先做一次配置自检
docker run --rm -v "$PWD/livekit.yaml:/etc/livekit.yaml:ro" \
  livekit/livekit-server:latest --config /etc/livekit.yaml --check
# 自检通过后再启动
docker compose up -d
docker logs -f livekit          # 出现 "starting LiveKit server" 即就绪
docker logs -f livekit-caddy    # 首次启动申请证书，出现证书签发成功日志即可用
```

证书与 ACME 账号数据落在 `./caddy/data`，**务必持久化**；丢失会导致每次重启重新签发，触发 Let's Encrypt 限流。

### 3.3 端口放行

| 端口 | 协议 | 用途 |
|------|------|------|
| 80 | tcp | ACME 证书校验与跳转 https |
| 443 | tcp | `wss://` 信令入口（Caddy → LiveKit:7880） |
| 7881 | tcp | WebRTC over TCP 回退（UDP 被墙时） |
| 50000-50100 | udp | 媒体端口段，**漏开必现"连上但黑屏/无声"** |
| 5349 | tcp | 仅启用 TURN/TLS 时需要 |
| 443 | udp | 仅启用 TURN over UDP 时需要 |
| 7880 | tcp | 仅本机监听，勿对公网开放 |
| 6789 | tcp | Prometheus 指标，仅内网 |

云安全组与主机防火墙（ufw / firewalld / iptables）都要放行。

### 3.4 连通性验证

```bash
# 1) 容器内信令
curl -s http://127.0.0.1:7880          # 返回 OK
# 2) 外网 wss 与证书
curl -s https://livekit.example.com    # 返回 OK 且证书有效
# 3) 应用侧（见 2.4）：登录后 GET /rtc/health 应返回 {"livekit":"up"}
# 4) 媒体连通性（必做）：/rtc/health 为 up 只证明信令连通，
#    必须双机登录前端实测出画面/出声，或用 https://livekit.io/connection-test 跑完 ICE 流程
```

### 3.5 TURN（默认关闭，按需启用）

LiveKit 内置 TURN，无需再部署 coturn。默认关闭的原因是 TURN/TLS 必须提供证书，而 443 已被 Caddy 占用。
只有当目标用户大量处于"屏蔽 UDP 且屏蔽非 443 TCP"的企业内网时才需要开启，步骤：

1. 签发证书：`docker run --rm -p 80:80 -v /etc/letsencrypt:/etc/letsencrypt certbot/certbot certonly --standalone -d turn.example.com --email you@example.com --agree-tos --no-eff-email`
2. 解开 `docker-compose.yml` 中 `/etc/letsencrypt` 的挂载
3. 取消 `livekit.yaml` 中 `turn` 段的注释（`tls_port` 用 5349，443 归 Caddy）
4. 放行 `5349/tcp` 与 `443/udp`

### 3.6 其他生产要点

- **版本固定**：compose 中 LiveKit 用 `latest` 便于起步，生产建议固定具体版本号，随发布流程统一升级。
- **Redis**：单机不需要；多节点集群必须配置 `redis.address` 才能共享房间状态。
- **启动顺序**：后端 `createRoom` 要求 LiveKit 已就绪，LiveKit 重启窗口内发起的通话会失败，需对 `/rtc/health` 做告警。
- **单端策略**：`identity = uid`，同账号新连接会顶掉旧连接，用户多端登录时会提示"该账号已在其他设备加入通话"。
- **密钥轮换**：轮换 `keys` 需同步改后端 `LIVEKIT_API_KEY` / `LIVEKIT_API_SECRET` 并重启；已签发的旧 token 在 TTL（默认 4h）内仍有效。

## 4. 前端部署（y-chat-sub）

### 4.1 Web 端

```bash
cd y-micro-app
pnpm install
pnpm build          # 产物 dist/，部署到任意静态服务器/Nginx
```

Nginx 需为页面配置 **https**（否则 `getUserMedia` 被浏览器禁止），并保证悟空 IM 的 WSS 地址可达。

### 4.2 桌面端（Tauri，v2）

```bash
cd y-micro-app/apps/y-chat-sub

# 开发
pnpm desktop:dev                      # 根 package.json 转发

# macOS 打包（dmg）
pnpm desktop:build:mac

# Linux 打包（deb/rpm/appimage，走 Docker 交叉构建）
pnpm desktop:linux:image              # 构建 y-chat-linux:latest 镜像（已就绪）
pnpm desktop:linux:run                # 容器内执行 tauri build，产物挂载输出
```

**macOS 摄像头/麦克风权限**：`src-tauri/Info.plist` 已声明（打包时自动合并）：

```xml
<key>NSCameraUsageDescription</key>
<string>用于视频通话，需要访问摄像头</string>
<key>NSMicrophoneUsageDescription</key>
<string>用于语音/视频通话，需要访问麦克风</string>
```

> 注意：**修改 Info.plist 后需重启 `tauri dev`** 才会重新生成 .app 并生效；已打包的旧 dmg 需重新打包。

**Windows**：Tauri v2 麦克头/摄像头默认无系统级弹窗，无需额外清单配置。

### 4.3 环境变量

`.env.production` 中 `VITE_BASE_API_URL` 指向 fg-chat 网关地址（生产域名），`VITE_WSS_KEY` 指向悟空 IM WSS。

无需新增 `VITE_*`：LiveKit 地址与接入 token 全部由后端 `call` / `accept` / `token` 接口下发。

## 5. 部署检查清单

- [ ] `t_rtc_room` / `t_rtc_participant` 表已存在，且 `t_rtc_room.channel_type` 迁移已执行
- [ ] 三个配置文件已落地 `/opt/livekit/`，`Caddyfile` 域名已改，`livekit.yaml` 密钥已替换且 `chmod 600`
- [ ] `xy.rtc.livekit.api-key` / `api-secret` 与 LiveKit `keys` 一致
- [ ] fg-chat 已重启，`GET /rtc/health` 登录后返回 `{"livekit":"up"}`
- [ ] 安全组/防火墙已放行：80/tcp、443/tcp、7881/tcp、50000-50100/udp（启用 TURN 时另加 5349/tcp、443/udp）
- [ ] `./caddy/data` 已持久化，`curl -s https://<域名>` 返回 OK 且证书有效
- [ ] 生产 `xy.rtc.livekit.url` 为 `wss://<域名>`，`LIVEKIT_API_URL` 走本机 `http://127.0.0.1:7880`
- [ ] `rtc.use_external_ip: true`（NAT/云主机），多网卡时已用 `rtc.node_ip` 显式指定
- [ ] 悟空 IM 可达：登录后能收发普通消息（信令复用同一通道）
- [ ] Web 端 https 证书有效（或使用桌面端）
- [ ] **媒体连通性已双机实测出画面/出声**（仅 `/rtc/health` 为 up 不够）
- [ ] 前端已强制刷新到新版本（旧版前端依赖 206-208 信令与 `/rtc/ice-servers`，已不可用）
- [ ] `scripts/rtc-api-test.sh` 全部通过（见 test-report.md；群用例需设置 `GROUP_ID`）
