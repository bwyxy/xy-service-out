import { defineStore } from 'pinia'
import i18n, { LOCALE_KEY, getDefaultLocale } from '@/i18n'

type LocaleType = 'zh-CN' | 'en-US'

const useLocaleStore = defineStore('locale', {
  state: () => ({
    locale: getDefaultLocale() as LocaleType,
  }),

  getters: {
    isZh(): boolean {
      return this.locale === 'zh-CN'
    },
  },

  actions: {
    setLocale(locale: LocaleType) {
      this.locale = locale
      i18n.global.locale.value = locale
      localStorage.setItem(LOCALE_KEY, locale)
    },

    toggleLocale() {
      this.setLocale(this.locale === 'zh-CN' ? 'en-US' : 'zh-CN')
    },
  },
})

export default useLocaleStore
