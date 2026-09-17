import { defineStore } from 'pinia'

const useAppStore = defineStore('app', {
  state: () => ({
    sidebarOpened: false,
    isMobile: false,
  }),

  actions: {
    toggleSidebar() {
      this.sidebarOpened = !this.sidebarOpened
    },

    openSidebar() {
      this.sidebarOpened = true
    },

    closeSidebar() {
      this.sidebarOpened = false
    },

    setMobile(isMobile: boolean) {
      this.isMobile = isMobile
    },
  },
})

export default useAppStore
