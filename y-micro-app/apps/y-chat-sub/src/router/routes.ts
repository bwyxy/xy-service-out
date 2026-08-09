import Layout from '@/layout/index.vue'

const routes = [
    {
        path: '/',
        name: 'Chat',
        component: Layout,
        meta: { title: 'chat' },
        redirect: '/chat',
        children: [
            {
                path: '/chat',
                name: 'ChatPage',
                component: () => import('@/views/chat/index.vue'),
                meta: { title: 'chatPage' }
            },
            {
                path: '/friend',
                name: 'FriendPage',
                component: () => import('@/views/friend/index.vue'),
                meta: { title: '好友' }
            }
        ]
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '登录' }
    }
]

export default routes
