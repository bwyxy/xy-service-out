import Layout from '@/layout/index.vue'

export const routes = [
    {
        path: '/',
        name: 'Home',
        component: Layout,
        meta: { title: '首页' },
        children: [
            {
                path: '',
                name: 'index',
                component: () => import('@/views/home.vue'),
                meta: { title: 'blog', starry: false }
            }

        ]
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login/index.vue'),
        meta: { title: '登录' }
    },
    {
        path: '/message',
        name: 'Message',
        component: Layout,
        redirect: '/message/board',
        children: [
            {
                path: '/message/board',
                name: 'board',
                component: () => import('@/views/messageBoard/index.vue'),
                meta: { title: '留言板', starry: true }
            }
        ]
    },
    {
        path: '/archive',
        name: 'Archive',
        component: Layout,
        redirect: '/archive/index',
        children: [
            {
                path: '/archive/index',
                name: 'ArchiveIndex',
                component: () => import('@/views/archive/index.vue'),
                meta: { title: '归档', starry: true }
            }
        ]
    },
    {
        path: '/category',
        name: 'Category',
        component: Layout,
        redirect: '/category/index',
        children: [
            {
                path: '/category/index',
                name: 'CategoryIndex',
                component: () => import('@/views/category/index.vue'),
                meta: { title: '分类', starry: true }
            }
        ]
    },
    {
        path: '/resume',
        name: 'Resume',
        component: Layout,
        redirect: '/resume/index',
        children: [
            {
                path: '/resume/index',
                name: 'ResumeIndex',
                component: () => import('@/views/resume/index.vue'),
                meta: { title: '简历', starry: true }
            }
        ]
    },
    {
        path: '/portfolio',
        name: 'Portfolio',
        component: Layout,
        redirect: '/portfolio/index',
        children: [
            {
                path: '/portfolio/index',
                name: 'PortfolioIndex',
                component: () => import('@/views/portfolio/index.vue'),
                meta: { title: '作品集', starry: true }
            }
        ]
    },
    {
        path: '/friendshipLinks',
        name: 'FriendshipLinks',
        component: Layout,
        redirect: '/friendshipLinks/index',
        children: [
            {
                path: '/friendshipLinks/index',
                name: 'FriendshipLinksIndex',
                component: () => import('@/views/friendshipLinks/index.vue'),
                meta: { title: '友情链接', starry: true }
            }
        ]
    },
    {
        path: '/tag',
        name: 'Tag',
        component: Layout,
        redirect: '/tag/index',
        children: [
            {
                path: '/tag/index',
                name: 'TagIndex',
                component: () => import('@/views/tag/index.vue'),
                meta: { title: '标签', starry: true }
            }
        ]
    },
    {
        path: '/article/:url',
        name: 'ArticleDetail',
        component: () => import('@/views/article/detail.vue'),
        meta: { title: '文章详情' }
    }
]