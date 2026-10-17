import request from '@/utils/request'

/**
 * 社区 / 论坛接口
 *
 * 与网页端 y-chat-sub/src/api/forum.ts 一一对应，仅把 axios 封装换成 utils/request。
 * 接口全部需要登录态（token 由 request 统一携带）。
 */

/** 查询版块列表（parentId 不传等价于 0，返回顶级版块） */
export function getBoards(data) {
	return request.post('/api/forum/boards', data)
}

/** 分页查询帖子列表，data 支持 { pageNum, pageSize, boardId }；boardId 为 0 表示全部 */
export function getPosts(data) {
	return request.post('/forum/posts', data)
}

/** 帖子详情（服务端会累加浏览量） */
export function getPostDetail(postId) {
	return request.get(`/forum/posts/${postId}`)
}

/** 点赞 / 取消点赞，返回操作后的点赞状态 */
export function setPostLike(postId) {
	return request.post(`/forum/posts/${postId}/like`)
}

/** 查询当前用户是否已点赞 */
export function hasPostLike(postId) {
	return request.get(`/forum/posts/${postId}/like`)
}

/** 发布帖子，返回新帖 postId */
export function createPost(data) {
	return request.post('/forum/posts/create', data)
}

/** 个人帖子列表（page 从 1 开始） */
export function getMePosts(page, size) {
	return request.get('/forum/posts/me', { page, size })
}

/** 创作中心统计 */
export function getCreatorStat() {
	return request.get('/forum/creator/stat')
}
