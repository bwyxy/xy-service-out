# 实战教程：用 Tauri 2 把 Vue3 聊天应用打包成桌面客户端

> 需求：已有的 Vue3 + qiankun 微前端聊天应用，要出一个桌面客户端。
> 核心诉求有三个：**独立的登录小窗口**、**自定义标题栏**、**登录态在窗口间传递**。
> 技术选型：Tauri 2（Rust 后端 + 系统 WebView，包体比 Electron 小一个数量级）。

本文是一条完整的落地路径，代码可直接对照使用。

---

## 1. 双窗口架构：主窗口隐藏，登录窗口先行

桌面客户端的标准形态是：**先弹一个 400×560 的登录小窗，登录成功后再切换到 1200×800 的主窗口。**

关键配置在 [tauri.conf.json](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src-tauri/tauri.conf.json)：

```json
{
  "productName": "飞鸽即时通讯",
  "identifier": "com.xy.chat.desktop",
  "build": {
    "frontendDist": "../dist",
    "devUrl": "http://localhost:9001",
    "beforeDevCommand": "pnpm dev",
    "beforeBuildCommand": "pnpm build:tauri"
  },
  "app": {
    "windows": [
      {
        "label": "main",
        "title": "飞鸽即时通讯",
        "width": 1200,
        "height": 800,
        "minWidth": 900,
        "minHeight": 600,
        "center": true,
        "visible": false,
        "titleBarStyle": "Overlay",
        "hiddenTitle": true
      }
    ],
    "security": { "csp": null }
  },
  "bundle": {
    "active": true,
    "targets": "all",
    "icon": ["icons/32x32.png", "icons/128x128.png", "icons/128x128@2x.png", "icons/icon.icns", "icons/icon.ico"]
  }
}
```

三个要点：

1. **`visible: false`**——主窗口默认不显示，避免"闪一下白屏再切走"。
2. **`titleBarStyle: "Overlay"` + `"hiddenTitle": true`**——macOS 上保留系统红绿灯按钮，同时隐藏标题文字，给自定义标题栏让位。
3. **`"csp": null`**——开发阶段先关掉 CSP，避免资源加载被拦（生产环境应配置具体策略）。

### 用 Rust 动态开登录窗口

主窗口由配置声明，登录窗口则用代码动态创建（因为要支持重复打开）：

```rust
const LOGIN_WINDOW_WIDTH: f64 = 400.0;
const LOGIN_WINDOW_HEIGHT: f64 = 560.0;

fn open_login_window(app: &tauri::AppHandle) -> tauri::Result<()> {
    // 幂等：窗口已存在则只做 show + focus
    if let Some(login) = app.get_webview_window("login") {
        let _ = login.show();
        let _ = login.set_focus();
        return Ok(());
    }

    let builder = WebviewWindowBuilder::new(
        app,
        "login",
        // 关键：hash 直达路由，避免先加载首页再跳转造成白屏
        WebviewUrl::App("index.html#/login?tauriWindow=login".into()),
    )
    .title("飞鸽即时通讯")
    .inner_size(LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT)
    .resizable(false)
    .maximizable(false)
    .center()
    .visible(true);

    #[cfg(target_os = "macos")]
    let login = builder
        .title_bar_style(tauri::TitleBarStyle::Overlay)
        .hidden_title(true)
        .build()?;

    #[cfg(not(target_os = "macos"))]
    let login = builder.decorations(false).build()?;   // 非 macOS 去掉原生边框，用自定义标题栏

    let _ = login.set_focus();
    Ok(())
}
```

**两个细节值得记下：**

- **`WebviewUrl::App("index.html#/login?...")`** —— 直接在 URL 里带上 hash 路由，窗口一打开就是登录页。如果先用 `index.html` 再靠 JS 跳 `/login`，会看到明显白屏闪烁。
- **`?tauriWindow=login` 这个 query 是故意加的** —— 它是**窗口身份识别的兜底手段**，下一节会讲为什么需要。

---

## 2. 窗口身份识别：label 为主，URL 为兜底

同一个前端 bundle，要同时服务 main 和 login 两个窗口，就必须让前端知道"我在哪个窗口里"。

首选方案是同步读取 Tauri 的内部元数据（无需 IPC，无异步时序问题）：

```ts
export function getTauriWindowLabel(): string | null {
    if (!isTauri()) return null
    const internals = (window as any).__TAURI_INTERNALS__
    return internals?.metadata?.currentWindow?.label ?? null
}
```

但**多 webview 场景下，元数据的注入时机可能晚于前端首屏脚本执行**，此时 `label` 读出来是 `null`，页面就会误判身份。所以我们从 URL 里再取一次兜底：

```ts
function getUrlWindowFlag(): string | null {
    const hash = window.location.hash || ''
    const queryIndex = hash.indexOf('?')
    if (queryIndex === -1) return null
    return new URLSearchParams(hash.slice(queryIndex + 1)).get('tauriWindow')
}

export function isTauriLoginWindow(): boolean {
    if (!isTauri()) return false
    return getTauriWindowLabel() === 'login' || getUrlWindowFlag() === 'login'
}
```

**经验**：
> 判断"我在哪个窗口"，**同步读 `__TAURI_INTERNALS__.metadata.currentWindow.label`** 最稳（比 `getCurrentWindow()` 这类异步 API 强，后者会让首屏渲染产生竞态）。
> 但一定要有 **URL 兜底**，因为元数据注入时机不受你控制。

---

## 3. 登录态传递：独立 webview 不共享 cookie

这是**最容易踩的坑**：Tauri 里每个 `WebviewWindow` 是**独立的 webview 实例，cookie / localStorage 不互通**。

在登录窗口写下 token，主窗口读不到。怎么办？

**方案：把状态提到 Rust 侧持久化，用事件回填主窗口。**

### 数据结构

```rust
#[derive(serde::Serialize, serde::Deserialize, Clone, Default)]
#[serde(default)]
#[serde(rename_all = "camelCase")]   // Rust 的 snake_case ↔ JS 的 camelCase
struct LoginState {
    logged_in: bool,
    token: String,
    wss_addr: String,
    wss_key: String,
}
```

存到 `login_state.json`，好处是**杀进程重启后仍是登录态**，不用重新登录。

### 登录成功

```rust
#[tauri::command]
fn login_success(
    app: tauri::AppHandle,
    token: String,
    wss_addr: String,
    wss_key: String,
) -> Result<(), String> {
    // 1. 写文件持久化
    // 2. 向 main 窗口 emit("login-success", state) 注入凭据
    // 3. main.show() + set_focus()
    // 4. 延迟 300ms 后 login.destroy()
    Ok(())
}
```

前端这样调用：

```ts
export async function tauriLoginSuccess(token: string, wssAddr: string, wssKey: string): Promise<void> {
    const { invoke } = await import('@tauri-apps/api/core')
    await invoke('login_success', { token, wssAddr, wssKey })
}
```

### 关键细节：`destroy()` 不是 `close()`

关闭登录窗口时用的是 **`destroy()` 而不是 `close()`**。原因：

我们在 `on_window_event` 里定义了"**关掉登录窗口 = 退出整个应用**"的语义：

```rust
.on_window_event(|window, event| {
    if let tauri::WindowEvent::CloseRequested { .. } = event {
        if window.label() == "login" {
            window.app_handle().exit(0);
        }
    }
})
```

如果登录成功后用 `close()` 关登录窗，会**触发这个 `CloseRequested`，把刚登录的主窗口一起杀掉**。

而 `destroy()` 是**强制销毁，不派发 `CloseRequested` 事件**，正好绕开这个语义。

> 一句话记住：**`close()` 会走"用户点了关闭"的逻辑（可拦截），`destroy()` 是直接销毁（不可拦截）。**
> 登录成功后关小窗用 `destroy()`；用户手动点小窗关闭用 `close()`（= 退出应用）。

### 启动时恢复登录态

```rust
#[tauri::command]
fn get_stored_credentials(app: tauri::AppHandle) -> LoginState {
    // 读 login_state.json
    // loggedIn 为真时，token 必然有效（登出时会清空）
}
```

前端在 `main.ts` 里判断：如果是 Tauri 主窗口且已存凭据 → 直接进主界面；否则弹登录窗。

---

## 4. 自定义标题栏 + capabilities 权限

非 macOS 平台用 `decorations(false)` 去掉原生边框后，窗口就"没有按钮了"，得自己画最小化/最大化/关闭。

而这些自定义按钮要调用的窗口 API，**必须在 capabilities 里显式授权**，否则会被 Tauri 2 的权限系统拒绝：

[capabilities/default.json](file:///Users/xiaoyi/me-project/xy-service-out/y-micro-app/apps/y-chat-sub/src-tauri/capabilities/default.json)：

```json
{
  "$schema": "../gen/schemas/desktop-schema.json",
  "identifier": "default",
  "description": "enables the default permissions",
  "windows": ["main", "login"],
  "permissions": [
    "core:default",
    "core:window:allow-start-dragging",
    "core:window:allow-minimize",
    "core:window:allow-maximize",
    "core:window:allow-unmaximize",
    "core:window:allow-toggle-maximize",
    "core:window:allow-close",
    "core:window:allow-is-maximized"
  ]
}
```

要点：

- **`windows: ["main", "login"]`** —— 两个窗口都要列进去，漏了登录窗，小窗上的关闭按钮就会失效。
- **`allow-start-dragging`** —— 自定义标题栏实现"拖拽移动窗口"必须的权限。
- **`allow-is-maximized`** —— 用来同步最大化/还原图标的显示状态。

macOS 则**不要**用 `decorations(false)`，保留 `titleBarStyle: Overlay` 让系统红绿灯原生工作，前端用 `isMacOS()` 判断是否渲染自定义按钮：

```ts
export function isMacOS(): boolean {
    if (typeof navigator === 'undefined') return false
    return /mac/i.test(navigator.userAgent)
}
```

---

## 5. macOS 摄像头/麦克风权限（通话功能必需）

如果桌面端要做音视频通话，macOS 下**必须声明用途**，否则 `getUserMedia` 直接失败。

在 `src-tauri/Info.plist` 中：

```xml
<key>NSCameraUsageDescription</key>
<string>用于视频通话</string>
<key>NSMicrophoneUsageDescription</key>
<string>用于语音通话</string>
```

踩坑提醒：**一旦用户拒绝过一次，macOS 会记住这个拒绝**，改配置也不会再弹窗。开发期要重置授权缓存：

```bash
tccutil reset Camera com.xy.chat.desktop
tccutil reset Microphone com.xy.chat.desktop
```

然后重启 `tauri dev` 才会重新出现授权弹窗。

---

## 6. 打包：必须用原生 runner，不能交叉编译

最后一个硬约束：**Tauri 无法从 macOS 交叉编译到 Windows / Linux 的安装包。** 想要三端产物，只能用 GitHub Actions 的对应平台原生 runner。

[.github/workflows/tauri-build.yml](file:///Users/xiaoyi/me-project/xy-service-out/.github/workflows/tauri-build.yml) 的核心结构：

```yaml
on:
  workflow_dispatch:
  push:
    tags: ['v*']

jobs:
  build:
    strategy:
      fail-fast: false
      matrix:
        include:
          - platform: ubuntu-22.04      # 产出 .deb / .rpm / .AppImage
          - platform: windows-latest   # 产出 .msi / .exe(NSIS)
    runs-on: ${{ matrix.platform }}
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: { version: 10.33.2 }
      - uses: actions/setup-node@v4
        with: { node-version: 22 }
      - uses: dtolnay/rust-toolchain@stable
      - uses: swatinem/rust-cache@v2
        with:
          workspaces: y-micro-app/apps/y-chat-sub/src-tauri -> target
      # Ubuntu 需要 Tauri v2 的系统依赖
      - name: Install Linux dependencies
        if: matrix.platform == 'ubuntu-22.04'
        run: |
          sudo apt-get update
          sudo apt-get install -y \
            libwebkit2gtk-4.1-dev build-essential curl wget file \
            libxdo-dev libssl-dev libayatana-appindicator3-dev librsvg2-dev
      - name: Install frontend dependencies
        working-directory: y-micro-app
        run: |
          pnpm install --frozen-lockfile
          pnpm build:packages     # 先构建 workspace 内部包
      - uses: tauri-apps/tauri-action@v0
        with:
          projectPath: y-micro-app/apps/y-chat-sub
      - uses: actions/upload-artifact@v4
        with:
          name: desktop-${{ matrix.platform }}
          path: y-micro-app/apps/y-chat-sub/src-tauri/target/release/bundle
          if-no-files-found: error
```

三个容易漏的点：

1. **Linux 系统依赖要装全**，尤其 `libwebkit2gtk-4.1-dev`（Tauri 2 用的是 4.1，不是 4.0）和 `libayatana-appindicator3-dev`。
2. **workspace 内部包要先 build**（`pnpm build:packages`），否则前端构建会找不到 `@xy/utils` 之类的工作区依赖。
3. **Rust 缓存路径要指向 `src-tauri -> target`**，否则每次 CI 都从零编译，慢到无法接受。

---

## 总结

Tauri 2 做桌面客户端，把这几件事想清楚就顺了：

| 问题 | 方案 |
| --- | --- |
| 主窗口先隐藏 | 配置 `visible: false`，登录成功后再 `show()` |
| 避免白屏 | `WebviewUrl::App("index.html#/login?...")` hash 直达 |
| 窗口身份识别 | 同步读 `metadata.currentWindow.label` + URL query 兜底 |
| 窗口间传凭据 | Rust 持久化 `login_state.json` + `emit("login-success")` |
| 登录后关小窗 | 用 `destroy()`，别触发 `CloseRequested` 退出逻辑 |
| 自定义标题栏 | `decorations(false)` + capabilities 显式授权窗口 API |
| macOS 权限 | `Info.plist` 声明 + `tccutil reset` 重置 |
| 多平台打包 | 原生 runner 矩阵构建，禁止交叉编译 |

Web 应用做桌面端，最大的心智转变是：**前端不再是"唯一的运行时"**。窗口的创建、销毁、权限、跨窗口状态，统统由 Rust 侧兜底。想清楚"谁的状态存在哪里"，比会写多少 `invoke` 更重要。