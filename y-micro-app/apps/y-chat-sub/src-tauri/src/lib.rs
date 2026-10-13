use std::fs;
use tauri::{Emitter, Manager, WebviewUrl, WebviewWindowBuilder};

/// 登录状态持久化文件名（位于系统应用配置目录）
const LOGIN_STATE_FILE: &str = "login_state.json";

/// 登录小窗口尺寸（参考微信桌面客户端登录窗口）
const LOGIN_WINDOW_WIDTH: f64 = 400.0;
const LOGIN_WINDOW_HEIGHT: f64 = 560.0;

/// 持久化的登录凭据：跨应用重启恢复登录态。
/// webview 的会话 cookie 不跨重启，需由 Rust 侧保存并在启动时回填给主窗口。
#[derive(serde::Serialize, serde::Deserialize, Clone, Default)]
#[serde(default)]
#[serde(rename_all = "camelCase")]
struct LoginState {
    logged_in: bool,
    token: String,
    wss_addr: String,
    wss_key: String,
}

fn state_file_path(app: &tauri::AppHandle) -> Option<std::path::PathBuf> {
    app.path()
        .app_config_dir()
        .ok()
        .map(|dir| dir.join(LOGIN_STATE_FILE))
}

fn read_login_state(app: &tauri::AppHandle) -> LoginState {
    state_file_path(app)
        .and_then(|path| fs::read_to_string(path).ok())
        .and_then(|content| serde_json::from_str::<LoginState>(&content).ok())
        .unwrap_or_default()
}

/// 凭据有效性校验：logged_in 为真且 token 非空才算已登录。
/// 兼容旧版本只含 logged_in 字段的状态文件（token 缺省为空，视为未登录）
fn is_logged_in(state: &LoginState) -> bool {
    state.logged_in && !state.token.is_empty()
}

fn write_login_state(app: &tauri::AppHandle, state: &LoginState) {
    if let Some(path) = state_file_path(app) {
        if let Some(parent) = path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        if let Ok(content) = serde_json::to_string(state) {
            let _ = fs::write(&path, content);
        }
    }
}

/// 打开登录小窗口（若已存在则仅聚焦，保证幂等）
fn open_login_window(app: &tauri::AppHandle) -> tauri::Result<()> {
    if let Some(login) = app.get_webview_window("login") {
        let _ = login.show();
        let _ = login.set_focus();
        return Ok(());
    }

    // 通过 hash 直达登录路由并附带窗口标记：
    // 登录窗口初始即位于 /login（白名单），不依赖运行时窗口身份检测即可渲染，
    // 避免初始路径 "/" 被路由守卫取消导航而导致白屏
    let login = WebviewWindowBuilder::new(
        app,
        "login",
        WebviewUrl::App("index.html#/login?tauriWindow=login".into()),
    )
    .title("飞鸽即时通讯")
    .inner_size(LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT)
    .resizable(false)
    .maximizable(false)
    .center()
    .visible(true)
    .build()?;
    let _ = login.set_focus();
    Ok(())
}

/// 登录成功：持久化凭据 -> 通知主窗口写入凭据并进入主界面 -> 关闭登录窗口
#[tauri::command]
fn login_success(app: tauri::AppHandle, token: String, wss_addr: String, wss_key: String) -> Result<(), String> {
    let state = LoginState {
        logged_in: true,
        token: token.clone(),
        wss_addr: wss_addr.clone(),
        wss_key: wss_key.clone(),
    };
    write_login_state(&app, &state);

    if let Some(main) = app.get_webview_window("main") {
        // 主窗口与登录窗口是独立 webview，凭据不共享，
        // 通过事件携带凭据由主窗口 JS 写入 cookie 后路由跳转
        let _ = main.emit("login-success", state);
        let _ = main.show();
        let _ = main.set_focus();

        // 延迟关闭登录窗口，配合前端缩放动画。
        // 使用 destroy 而非 close：destroy 不触发 CloseRequested 事件，
        // 避免被"用户点击关闭登录窗口=退出应用"的判定误杀（后台线程关闭时序无法保证）
        let handle = app.clone();
        std::thread::spawn(move || {
            std::thread::sleep(std::time::Duration::from_millis(300));
            if let Some(login) = handle.get_webview_window("login") {
                let _ = login.destroy();
            }
        });
    }
    Ok(())
}

/// 退出登录：清除持久化凭据 -> 隐藏主窗口 -> 打开登录小窗口
#[tauri::command]
fn logout(app: tauri::AppHandle) -> Result<(), String> {
    write_login_state(&app, &LoginState::default());

    if let Some(main) = app.get_webview_window("main") {
        let _ = main.hide();
    }
    open_login_window(&app).map_err(|e| e.to_string())
}

/// 主窗口启动时拉取持久化凭据（应用重启后恢复登录态）。
/// 返回值保证：loggedIn 为真时 token 必然有效
#[tauri::command]
fn get_stored_credentials(app: tauri::AppHandle) -> LoginState {
    let mut state = read_login_state(&app);
    if !is_logged_in(&state) {
        state.logged_in = false;
    }
    state
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }

            let handle = app.handle().clone();
            if is_logged_in(&read_login_state(&handle)) {
                // 上次退出时已登录：直接显示主窗口，凭据由前端拉取回填
                if let Some(main) = handle.get_webview_window("main") {
                    let _ = main.show();
                    let _ = main.set_focus();
                }
            } else {
                // 未登录：主窗口保持隐藏，先展示登录小窗口
                open_login_window(&handle)?;
            }
            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { .. } = event {
                // 用户在登录小窗口点击关闭：视为放弃登录，退出整个应用。
                // 程序化关闭走 destroy()，不会进入此分支
                if window.label() == "login" {
                    window.app_handle().exit(0);
                }
            }
        })
        .invoke_handler(tauri::generate_handler![
            login_success,
            logout,
            get_stored_credentials
        ])
        .run(tauri::generate_context!())
        .expect("error while building tauri application");
}
