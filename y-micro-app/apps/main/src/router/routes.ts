
const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('@/views/home.vue'),
        meta: { title: '首页' }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/app.vue'),
        meta: { title: "管理后台" }
    },
    {
        path: '/chat',
        name: 'Chat',
        component: () => import('@/views/app.vue'),
        meta: { title: '飞鸽即时通讯' }
    }
]

export default routes;
