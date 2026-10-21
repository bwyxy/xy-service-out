import request from '@/utils/request'

/**
 * 获取会话列表
 *
 * 后端 /conversation/list 从 query 参数绑定（与 Web 端保持一致），故用 params 传递。
 * */
export function getConversation(params) {
  return request({
    url: '/conversation/list',
    method: 'post',
    params,
  })
}

/**
 * 获取历史消息
 * */
export function getMessageList(data) {
  return request({
    url: '/message/list',
    method: 'post',
    data,
  })
}

/**
 * 发送消息（经服务端转发至悟空 IM）
 *
 * 统一走服务端接口，便于后续做内容安全校验、图片鉴黄等业务扩展。
 *
 * @param data { channelId, channelType, type, content, clientMsgNo? }
 */
export function sendMessage(data) {
  return request({
    url: '/message/send',
    method: 'post',
    data,
  })
}
