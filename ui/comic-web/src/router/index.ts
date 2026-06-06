import {createRouter, createWebHashHistory} from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Layout,
        redirect: '/home',
        children: [
            {
                path: '/home',
                name: 'HomeIndex',
                component: () => import('@/views/home.vue'),
                meta: { title: '首页' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router