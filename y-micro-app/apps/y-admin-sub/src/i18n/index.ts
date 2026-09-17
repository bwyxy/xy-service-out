import { createI18n } from 'vue-i18n'
import zhCN from './lang/zh-CN'
import enUS from './lang/en-US'

export const LOCALE_KEY = 'admin-locale'

export const messages = {
  'zh-CN': zhCN,
  'en-US': enUS,
}

export const getDefaultLocale = (): string => {
  const saved = localStorage.getItem(LOCALE_KEY)
  if (saved && messages[saved as keyof typeof messages]) {
    return saved
  }
  const browserLang = navigator.language
  if (browserLang.startsWith('zh')) {
    return 'zh-CN'
  }
  return 'en-US'
}

const i18n = createI18n({
  legacy: false,
  locale: getDefaultLocale(),
  fallbackLocale: 'zh-CN',
  messages,
  silentTranslationWarn: true,
  missingWarn: false,
  fallbackWarn: false,
})

export default i18n
