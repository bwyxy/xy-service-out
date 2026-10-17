import request from '@/utils/request'

/**
 * 搜索用户
 * */
export function searchUsers(keyword) {
  return request.get('/friend/search', { keyword })
}

/**
 * 发送好友请求
 * */
export function sendFriendRequest(data) {
  return request({
    url: '/friend/request/send',
    method: 'POST',
    data,
  })
}

/**
 * 处理好友请求
 * */
export function handleFriendRequest(data) {
  return request({
    url: '/friend/request/handle',
    method: 'POST',
    data,
  })
}

/**
 * 收到的好友请求
 * */
export function getReceivedRequests(status) {
  return request.get('/friend/request/received', { status })
}

/**
 * 发送的好友请求
 * */
export function getSentRequests(status) {
  return request.get('/friend/request/sent', { status })
}

/**
 * 好友列表
 * */
export function getFriendList() {
  return request({
    url: '/friend/list',
    method: 'GET',
  })
}
