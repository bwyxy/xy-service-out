import { createWebHashHistory, createRouter } from 'vue-router'
import routes from "@/router/routes.ts";
import { getToken } from '@/utils/auth.ts'
import useUserStore from "@/store/user.ts";
import { isTauri, isTauriLoginWindow } from '@/utils/tauri.ts'

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

const whiteList = ["/login"];
router.beforeEach(async (to, _from, next) => {

    const userStore = useUserStore()

    // 桌面登录小窗口：仅承载登录/注册页，不参与鉴权
    if (isTauriLoginWindow()) {
        to.path === "/login" ? next() : next("/login")
        return
    }

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
                if (isTauri()) {
                    // 桌面主窗口：凭据已失效，由 Rust 切回登录小窗口
                    next(false)
                } else {
                    next(`/login?redirect=${to.path}`);
                }
            }
        }
    } else {
        /**
         * 当前没有登录验证token
         */
        if (whiteList.indexOf(to.path) !== -1) {
            next()
        } else if (isTauri()) {
            // 桌面主窗口无凭据：仅取消导航（主窗口保持隐藏）。
            // 窗口切换由 main.ts 登录桥接统一管理（登录事件 / 重启凭据拉取），
            // 不在此处触发退出流程，避免与凭据回填时序冲突
            next(false)
        } else {
            userStore.outLogin()
            next(`/login?redirect=${to.path}`);
        }
    }
})

export default router
