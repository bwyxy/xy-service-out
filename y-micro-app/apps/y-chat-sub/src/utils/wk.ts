import { WKIM } from 'easyjssdk'

let im: any = null

export interface WKConfig {
  uid: string
  token: string
  wsUrl: string
}

/** 初始化悟空IM WebSocket 连接 */
export function initWK(config: WKConfig) {
  if (im) {
    im.disconnect()
  }
  im = WKIM.init(config.wsUrl, {
    uid: config.uid,
    token: config.token,
  })
  return im
}

export function getIM() {
  return im
}

/** 断开连接 */
export function disconnect() {
  if (im) {
    im.disconnect()
    im = null
  }
}

/** 发送消息到个人频道 */
export function sendPersonalMessage(imInstance: any, toUid: string, content: string) {
  return imInstance.send(toUid, 1, {
    type: 1,
    content,
  })
}

/** 发送消息到群组频道 */
export function sendGroupMessage(imInstance: any, groupId: string, content: string) {
  return imInstance.send(groupId, 2, {
    type: 1,
    content,
  })
}

/** 获取会话列表 */
export function getConversations(imInstance: any) {
  return imInstance.getConversationList()
}

/** 同步消息 */
export function syncMessages(imInstance: any, channelId: string, channelType: number, startSeq: number, limit: number) {
  return imInstance.syncMessage(channelId, channelType, startSeq, limit)
}