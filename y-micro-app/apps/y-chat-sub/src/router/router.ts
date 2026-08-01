import { useRouter } from 'vue-router'

/**
 * 跳转界面
 * */
export const toPage = (path :string) => {
    const router = useRouter()

    router.push({
        path: path
    })
}