/**
 * Tauri 桌面环境工具
 * 负责桌面环境检测、窗口身份识别与 Rust 命令调用封装
 */

/** 是否运行在 Tauri 桌面环境 */
export function isTauri(): boolean {
    return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window
}

/**
 * 获取当前 Tauri 窗口 label（同步读取，无需 IPC）
 * main = 应用主窗口，login = 登录小窗口
 */
export function getTauriWindowLabel(): string | null {
    if (!isTauri()) return null
    const internals = (window as any).__TAURI_INTERNALS__
    return internals?.metadata?.currentWindow?.label ?? null
}

/**
 * 从 hash 查询参数中读取窗口标记（Rust 创建窗口时通过 URL 注入）。
 * 作为 label 检测的可靠兜底，规避多 webview 下元数据时序差异。
 */
function getUrlWindowFlag(): string | null {
    const hash = window.location.hash || ''
    const queryIndex = hash.indexOf('?')
    if (queryIndex === -1) return null
    return new URLSearchParams(hash.slice(queryIndex + 1)).get('tauriWindow')
}

/** 当前是否为桌面登录小窗口 */
export function isTauriLoginWindow(): boolean {
    if (!isTauri()) return false
    return getTauriWindowLabel() === 'login' || getUrlWindowFlag() === 'login'
}

/** 当前是否为桌面主窗口 */
export function isTauriMainWindow(): boolean {
    if (!isTauri()) return false
    return getTauriWindowLabel() === 'main' || getUrlWindowFlag() === 'main'
}

/**
 * 登录成功：通知 Rust 持久化登录状态、
 * 注入凭据到主窗口、重载并显示主窗口、关闭登录小窗口
 * @param token 登录 token
 * @param wssAddr IM WebSocket 地址
 * @param wssKey IM WebSocket cookie key
 */
export async function tauriLoginSuccess(token: string, wssAddr: string, wssKey: string): Promise<void> {
    const { invoke } = await import('@tauri-apps/api/core')
    await invoke('login_success', { token, wssAddr, wssKey })
}

/**
 * 退出登录：通知 Rust 清除登录状态、
 * 隐藏主窗口并重新打开登录小窗口
 */
export async function tauriLogout(): Promise<void> {
    const { invoke } = await import('@tauri-apps/api/core')
    await invoke('logout')
}
