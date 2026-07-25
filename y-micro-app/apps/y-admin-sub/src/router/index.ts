import { createWebHashHistory, createRouter } from 'vue-router'
import routes from '@/router/routes.ts'
import { getToken } from '@/utils/auth.ts'

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

router.beforeEach((to, _from, next) => {
    if (getToken()) {
        if (to.path === '/login') {
            next({ path: '/' })
        } else {}
    }
})

export default router
