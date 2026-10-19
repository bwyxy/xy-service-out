/**
 * 论坛帖子
 */
interface ForumPost {
    postId: number
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
    liked?: boolean
    favorited?: boolean
    isEssence?: number
    viewCount?: number
    likeCount?: number
    commentCount?: number
    favoriteCount?: number
    shareCount?: number
    publishedTime?: string
    createTime?: string
    updateTime?: string,
    ip?: string
    ipAddr?: string
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

interface CreatorStat {
    followerCount: number
    followingCount: number
    likeCount: number
    favoriteCount: number
    commentCount: number
}

interface R<T> {
    code: number

    msg: string

    data: T
}

interface PageResult<T> {

    pageNum: number

    pageSize: number

    pages: number

    total: number

    list: T
}

interface GroupMember {
    id: number

    uid: number

    nickname: string

    avatar: string

    sex: string

    role: number

    status: number

    groupNickname: string

    /** 禁言结束时间，为空表示未被禁言 */
    muteEndTime?: string | null
}
