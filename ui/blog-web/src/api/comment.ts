import http from "@/utils/request/index.ts";

/**
 * 获取评论树
 * */
export const getComments = (data :any) => {
    return http.post('/web/comment/tree', data)
}

/**
 * 获取评论列表
 * */
export const commentList = (data :any) => {
    return http.post('/web/comment/list', data)
}

/**
 * 发表评论
 * */
export const sendComment = (data :any) => {
    return http.post('/web/comment/add', data)
}