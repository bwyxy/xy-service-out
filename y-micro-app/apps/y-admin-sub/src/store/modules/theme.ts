import { defineStore } from 'pinia'

type Theme = 'light' | 'dark'

const THEME_KEY = 'app-theme'

const applyTheme = (theme: Theme, withTransition = true) => {
  const html = document.documentElement

  if (withTransition) {
    html.classList.add('theme-transition')
    setTimeout(() => {
      html.classList.remove('theme-transition')
    }, 400)
  }

  if (theme === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

const getStoredTheme = (): Theme => {
  const stored = localStorage.getItem(THEME_KEY)
  if (stored === 'dark' || stored === 'light') {
    return stored
  }
  return 'light'
}

const useThemeStore = defineStore('theme', {
  state: (): { theme: Theme } => ({
    theme: getStoredTheme(),
  }),

  getters: {
    isDark: (state) => state.theme === 'dark',
  },

  actions: {
    init() {
      applyTheme(this.theme, false)
    },

    toggle() {
      this.theme = this.theme === 'light' ? 'dark' : 'light'
      localStorage.setItem(THEME_KEY, this.theme)
      applyTheme(this.theme)
    },

    setTheme(theme: Theme) {
      this.theme = theme
      localStorage.setItem(THEME_KEY, theme)
      applyTheme(theme)
    },
  },
})

export default useThemeStore
