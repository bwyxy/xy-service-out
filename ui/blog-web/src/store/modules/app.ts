import { defineStore } from 'pinia'
import { getBlogConfig } from '@/api/blog.ts'

const useAppStore = defineStore('app', {
    state() {
        return {
            blogInfo: {} as any
        }
    },
    actions: {
        async setBlogInfo() {
            const res :R<any> = await getBlogConfig()

            if (res.code == 200) {
                this.blogInfo = res.data
            }
        }
    }
})

export default useAppStore