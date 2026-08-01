import { createWebHashHistory, createRouter } from 'vue-router'
import routes from "@/router/routes.ts";
import { getToken } from '@/utils/auth.ts'
import useUserStore from "@/store/user.ts";

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

const whiteList = ["/login"];
router.beforeEach(async (to, _from, next) => {

    const userStore = useUserStore()
    const hasToken = getToken() || ''
    if (hasToken) {
        if (to.path === "/login") {
            next("/")
        } else if (whiteList.indexOf(to.path) !== -1) {
            next()
        } else {
            try {
                await userStore.setUserInfo()
                next()
            } catch (e) {
                console.error(e)
                userStore.outLogin()
                next(`/login?redirect=${to.path}`);
            }
        }
    } else {
        /**
         * 当前没有登录验证token
         */
        if (whiteList.indexOf(to.path) !== -1) {
            next()
        } else {
            userStore.outLogin()
            next(`/login?redirect=${to.path}`);
        }
    }
})

export default router
