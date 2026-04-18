import request from '@/utils/request'

export interface CaptchaResult {
  imgSrc: string
  uuid: string
}

/** 解析若依 /captchaImage 常见返回（顶层或包在 data 内） */
function parseCaptchaBody(body: unknown): CaptchaResult | null {
  if (!body || typeof body !== 'object') return null
  const o = body as Record<string, unknown>
  let img: string | undefined
  let uuid: string | undefined

  if (typeof o.img === 'string') img = o.img
  if (typeof o.uuid === 'string') uuid = o.uuid
  if (typeof o.imgUrl === 'string' && !img) img = o.imgUrl

  const nested = o.data
  if (nested && typeof nested === 'object') {
    const d = nested as Record<string, unknown>
    if (!img && typeof d.img === 'string') img = d.img
    if (!uuid && typeof d.uuid === 'string') uuid = d.uuid
    if (!img && typeof d.imgUrl === 'string') img = d.imgUrl
  }

  if (!img || !uuid) return null
  // 若依前端常用 gif；若已是 data URL 则沿用
  const imgSrc = img.startsWith('data:') ? img : `data:image/gif;base64,${img}`
  return { imgSrc, uuid }
}

/** GET /captchaImage（若依预设） */
export async function fetchCaptchaImage(): Promise<CaptchaResult> {
  const res = await request.get<unknown>('/captchaImage')
  const parsed = parseCaptchaBody(res.data)
  if (!parsed) {
    throw new Error('无法解析验证码，请检查后端 /captchaImage 响应格式')
  }
  return parsed
}
