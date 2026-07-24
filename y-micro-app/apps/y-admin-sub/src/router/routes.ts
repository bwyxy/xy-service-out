
const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: 'Home' }
    }
]

export default routes
