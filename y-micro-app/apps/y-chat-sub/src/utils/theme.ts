/** 主题色预设 */
export interface ThemePreset {
  /** 预设标识，对应 CSS 中的 :root[data-theme] */
  key: string
  /** 展示名称 */
  name: string
  /** 主色，用于色块展示 */
  color: string
}

/** 可选主题色预设（需与 assets/styles/theme.scss 保持一致） */
export const themePresets: ThemePreset[] = [
  { key: 'green', name: '默认绿', color: '#52c41a' },
  { key: 'blue', name: '天空蓝', color: '#2f54eb' },
  { key: 'purple', name: '典雅紫', color: '#722ed1' },
  { key: 'orange', name: '活力橙', color: '#fa8c16' },
  { key: 'magenta', name: '樱花粉', color: '#eb2f96' },
]

const THEME_STORAGE_KEY = 'xy_chat_theme'

/** 默认主题色 */
export const DEFAULT_THEME = 'green'

/** 读取本地持久化的主题色，未设置时返回默认值 */
export const getTheme = (): string => localStorage.getItem(THEME_STORAGE_KEY) || DEFAULT_THEME

/** 应用主题色：切换根节点 data-theme 属性，驱动 CSS 变量生效 */
export const applyTheme = (key: string): void => {
  document.documentElement.setAttribute('data-theme', key)
}

/** 切换并持久化主题色 */
export const setTheme = (key: string): void => {
  localStorage.setItem(THEME_STORAGE_KEY, key)
  applyTheme(key)
}

/** 启动时应用已保存的主题色 */
export const initTheme = (): void => applyTheme(getTheme())
