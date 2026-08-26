interface ColumnType {
    key: string
    label: string
    prop: string
    width?: string | number
    type?: string,
    render?: ColumnRender,
    children?: ColumnType[]
}

interface ColumnRender {
    el: ('search' | 'select'),
    list?: Value[]
}

interface Value {
    label: string,
    value: string | number,
    elTagType: ( "primary" | "success" | "info" | "warning" | "danger")
}

interface PageResult<T> {

    pageNum: number

    pageSize: number

    pages: number

    total: number

    list: T
}

interface R<T> {
    code: number

    msg: string

    data: T
}

/**
 * 论坛板块
 */
interface ForumBoard {
    boardId?: number
    parentId?: number
    name?: string
    slug?: string
    icon?: string
    description?: string
    sortOrder?: number
    status?: number
    postCount?: number
    followCount?: number
    createTime?: string
    updateTime?: string
}

/**
 * 论坛帖子
 */
interface ForumPost {
    postId?: number
    authorId?: number
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
 * 论坛评论
 */
interface ForumComment {
    commentId?: number
    postId?: number
    authorId?: number
    parentId?: number
    rootId?: number
    replyToUid?: number
    content?: string
    likeCount?: number
    status?: number
    createTime?: string
    updateTime?: string
}

/**
 * 论坛话题
 */
interface ForumTopic {
    topicId?: number
    name?: string
    slug?: string
    description?: string
    coverImage?: string
    postCount?: number
    followCount?: number
    status?: number
    createTime?: string
    updateTime?: string
}

/**
 * 论坛举报
 */
interface ForumReport {
    reportId?: number
    reporterId?: number
    targetType?: number
    targetId?: number
    reason?: string
    status?: number
    handlerId?: number
    result?: string
    createTime?: string
    updateTime?: string
}

/**
 * 版主
 */
interface ForumModerator {
    id?: number
    boardId?: number
    uId?: number
    role?: number
    createTime?: string
}

/**
 * 论坛用户扩展资料
 */
interface ForumUserProfile {
    id?: number
    uId?: number
    level?: number
    signature?: string
    postCount?: number
    commentCount?: number
    likeReceivedCount?: number
    score?: number
    createTime?: string
    updateTime?: string
}

/**
 * 论坛关注
 */
interface ForumFollow {
    id?: number
    uId?: number
    followType?: number
    targetId?: number
    createTime?: string
}

/**
 * 论坛操作日志
 */
interface ForumOperLog {
    id?: number
    uId?: number
    operType?: string
    targetType?: string
    targetId?: number
    detail?: string
    ip?: string
    createTime?: string
}