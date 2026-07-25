import { defineStore } from 'pinia'

const useUserStore = defineStore('user', {
    state() {
        return {
            userInfo: {},
            roles: []
        }
    },
    actions: {

    }
})

export default useUserStore