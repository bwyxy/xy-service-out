/**
 * 论坛帖子
 */
interface ForumPost {
    postId?: number
    authorId?: number
    avatar?: string
    nickname?: string
    boardId?: number
    title?: string
    summary?: string
    content?: string
    contentType?: number
    coverImage?: string
    status?: number
    visibility?: number
    isTop?: number
    isEssence?: number
    viewCount?: number
    likeCount?: number
    commentCount?: number
    favoriteCount?: number
    shareCount?: number
    publishedTime?: string
    createTime?: string
    updateTime?: string
}

/**
 * 论坛板块
 */
interface ForumBoard {
    boardId: number
    parentId?: number
    name?: string
    slug?: string
    icon?: string
    key?: string
    description?: string
    sortOrder?: number
    status?: number
    postCount?: number
    followCount?: number
    createTime?: string
    updateTime?: string
}

interface R<T> {
    code: number

    msg: string

    data: T
}