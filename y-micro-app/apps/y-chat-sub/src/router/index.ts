import { createWebHashHistory, createRouter } from 'vue-router'
import routes from "@/router/routes.ts";

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router
