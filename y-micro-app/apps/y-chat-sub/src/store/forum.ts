import { defineStore } from 'pinia'

const useForumStore = defineStore('forum', {
    state() {
        return {
            currentBoardId: 0
        }
    },
    actions: {}
})

export default useForumStore;