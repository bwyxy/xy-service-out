# 1v1 音视频通话系统部署文档

## 1. 环境要求

| 组件 | 要求 |
|------|------|
| JDK | 17+（fg-chat 为 Spring Boot 3.2.4） |
| MySQL | 8.x，已执行 `sql/chat.sql`（含 `t_rtc_room` / `t_rtc_participant` 建表） |
| Redis | 用于分布式锁与在线状态 |
| 悟空 IM | 已部署，fg-chat 通过 Feign `WuKongClient` 推送信令 |
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
    # ICE 服务器（STUN/TURN），生产环境建议自建 coturn 并补充 turn 配置
    ice-servers:
      - urls: stun:stun.l.google.com:19302
      # - urls: turn:turn.example.com:3478
      #   username: your-username
      #   credential: your-credential
```

可通过环境变量覆盖（Spring 宽松绑定）：`XY_RTC_CALL_TIMEOUT_SECONDS`、`XY_RTC_ICE_SERVERS_0_URLS` 等。

### 2.2 构建与启动

```bash
# 编译
mvn -pl customer-api/fg-chat -am clean package -DskipTests

# 启动（默认端口 9080，context-path 为 /）
java -jar customer-api/fg-chat/target/fg-chat-*.jar
```

开发环境在 IDEA 中直接启动主类即可。**注意：新增了 RTC 代码（Controller/Service/Mapper/枚举/定时任务），老进程必须重启才能生效。**

### 2.3 启动自检

```bash
# 未登录应返回 401 业务码
curl -s http://localhost:9080/rtc/ice-servers
# 登录后（token 替换为真实 JWT）
curl -s -H "token: <jwt>" http://localhost:9080/rtc/ice-servers
# 期望: {"code":200,"data":[{"urls":"stun:stun.l.google.com:19302"}]}
```

### 2.4 定时任务

`RtcTimeoutTask` 每 10 秒执行一次，将"呼叫中"超过 90 秒的房间自动置为结束并向双方推送未接听记录。多实例部署时任务非分布式锁排他，但因 `expireStaleRooms()` 内部按房间幂等（仅 `status=0` 才更新），重复执行无害。

## 3. 自建 TURN 服务器（coturn，Docker）

P2P 直连失败率约 10-15%（对称型 NAT 等场景），生产环境必须部署 TURN 中继兜底。

### 3.1 docker-compose.yml

```yaml
version: "3.8"
services:
  coturn:
    image: coturn/coturn:latest
    network_mode: host          # TURN 需要 host 网络获取真实客户端 IP
    restart: always
    volumes:
      - ./turnserver.conf:/etc/coturn/turnserver.conf
    command: ["-c", "/etc/coturn/turnserver.conf"]
```

### 3.2 turnserver.conf

```conf
# 监听端口（UDP/TCP）
listening-port=3478
# TLS 监听端口（可选，浏览器 https 页面下更安全）
tls-listening-port=5349

# 对外公网 IP（云服务器填弹性公网 IP；NAT 部署填映射后的公网 IP）
external-ip=<YOUR_PUBLIC_IP>

# 认证：静态长期凭据（生产建议换 use-auth-secret 动态凭据）
lt-cred-mech
user=rtcuser:rtcpass2026

# TLS 证书（启用 5349 时需要；可用 Let's Encrypt）
# cert=/etc/letsencrypt/live/turn.example.com/fullchain.pem
# pkey=/etc/letsencrypt/live/turn.example.com/privkey.pem

# 中继端口范围（防火墙/安全组需放行 UDP 49160-49200）
min-port=49160
max-port=49200

# 不建议开启多线程下的同端口复用问题排查项
no-multicast-peers
no-cli
# 日志
log-file=stdout
verbose
```

### 3.3 启动与验证

```bash
mkdir -p /opt/coturn && cd /opt/coturn
# 放入上述两个文件后
docker compose up -d
docker logs -f coturn    # 看到 "SQLite DB ... opened" 即就绪
```

浏览器验证：打开 https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/ ，添加 TURN 服务器 `turn:<公网IP>:3478` + 凭据，点 Gather candidates 出现 `relay` 类型候选即成功。

### 3.4 接入应用

`application.yml` 追加 TURN 条目：

```yaml
xy:
  rtc:
    ice-servers:
      - urls: stun:stun.l.google.com:19302
      - urls: turn:<公网IP>:3478
        username: rtcuser
        credential: rtpass2026
```

前端 `startCall` 时通过 `GET /rtc/ice-servers` 拉取并注入 `RTCPeerConnection`，无需前端改动。

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

## 5. 部署检查清单

- [ ] `t_rtc_room` / `t_rtc_participant` 表已存在（`sql/chat.sql`）
- [ ] fg-chat 已重启，`GET /rtc/ice-servers` 登录后返回 200
- [ ] 悟空 IM 可达：登录后能收发普通消息（信令复用同一通道）
- [ ] Web 端 https 证书有效（或使用桌面端）
- [ ] （生产）coturn 已部署，trickle-ice 验证出现 relay 候选
- [ ] （生产）安全组放行：3478/UDP+TCP、49160-49200/UDP（TURN 中继）
- [ ] `scripts/rtc-api-test.sh` 全部通过（见 test-report.md）
