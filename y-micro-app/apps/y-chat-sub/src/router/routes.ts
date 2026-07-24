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
            }
        ]
    }
]

export default routes
