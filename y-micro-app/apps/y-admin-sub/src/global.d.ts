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

/** 文章 */
interface Article {
    articleId: number | null;
    /** 文章标题 */
    title: string;
    /** URL 别名 */
    slug: string;
    /** 摘要 */
    summary: string;
    /** 正文 */
    content: string;
    /** 封面 */
    coverImage: string;
    /**
     * 状态
     * 0-草稿 1-已发布 2-私密
     */
    status: string;
    /** 浏览次数 */
    viewCount: number;
    /** 点赞数 */
    likeCount: number;
    /** 评论数 */
    commentCount: number;
    /** 作者 Id */
    authorId: number | null;
    /** 分类 Id */
    categoryId: number | null;
    /** 发布时间 */
    publishedTime: string;
    /** 标签 Ids */
    tagIds: number[];
}

/** 分类 */
interface Category {
    categoryId: number | null;
    /** 分类名称 */
    name: string;
    /** URL 别名 */
    slug: string;
    /** 描述 */
    description: string;
    createTime: string;
    updateTime: string;
}

/** 标签 */
interface Tag {
    tagId: number | null;
    /** 标签名称 */
    name: string;
    /** 颜色 */
    color: string;
    /** 水平 */
    level: number | null;
    /** URL 别名 */
    slug: string;
    /** 描述 */
    description: string;
    /** 文章数量（非数据库字段） */
    articleCount: number | null;
    createTime: string;
    updateTime: string;
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

/**
 * 友链
 */
interface FriendLink {
    /** 站点 Id */
    id?: number;

    /** 站点名称 */
    name?: string;

    /** 站点地址 */
    url?: string;

    /** 站点 logo */
    logo?: string;

    /** 描述 */
    description?: string;

    /** 站长邮箱 */
    email?: string;

    /** 排序 */
    sort?: number;

    /** 状态 1-显示 0-隐藏 */
    status?: number;

    /** 备注 */
    remark?: string;
}

/**
 * 公告
 */
interface Notice {
    /** 公告 Id */
    id?: number;

    /** 标题 */
    title?: string;

    /** 内容 */
    content?: string;

    /** 类型 1-普通 2-重要 3-紧急 */
    type?: NoticeType;

    /** 状态 1-发布 0-草稿 2-下线 */
    status?: NoticeStatus;

    /** 是否置顶 1-是 0-否 */
    isTop?: number;

    /** 排序 */
    sort?: number;

    /** 跳转链接 */
    linkUrl?: string;

    /** 生效时间，为空立即生效 */
    startTime?: string;

    /** 失效时间，为空永久有效 */
    endTime?: string;
}