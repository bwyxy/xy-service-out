import type { WukongIMMessagePayload } from '@/services/im'

/**
 * 解码悟空 IM 消息的 payload（Base64 → JSON）
 *
 * 后端使用 Base64.getEncoder().encodeToString() 编码，前端需先 Base64 解码再反序列化。
 *
 * @param payload Base64 编码的 JSON 字符串
 * @returns 解析后的消息负载对象；解析失败时回退返回 { type: 1, content: payload }
 */
export function decodePayload(payload: string): WukongIMMessagePayload {
  try {
    const jsonStr = atob(payload)
    return JSON.parse(jsonStr) as WukongIMMessagePayload
  } catch (error) {
    console.error('[WukongIM] payload 解码失败:', error)
    return { type: 1, content: payload }
  }
}

/**
 * 解码 Java 后端返回的 Base64 字符串（兼容中文字符）
 *
 * Java 默认使用 UTF-8 编码生成 Base64，atob 得到的是 Latin1 字节序列，
 * 需要手动转换为 Uint8Array 后再以 UTF-8 解码，才能正确处理中文。
 *
 * @param base64Str Java Base64 编码字符串
 * @returns UTF-8 解码后的文本
 */
export function decodeBase64FromJava(base64Str: string): string {
  const binaryString = atob(base64Str)
  const bytes = new Uint8Array(binaryString.length)

  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i)
  }

  return new TextDecoder('utf-8').decode(bytes)
}
