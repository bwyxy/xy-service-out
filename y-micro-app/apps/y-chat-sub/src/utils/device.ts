import { isTauri } from '@/utils/tauri.ts'

/** 设备类型分组，与后端 DeviceType 枚举保持一致 */
export const DeviceTypeEnum = {
  /** PC 端 */
  PC: 1,
  /** 平板端 */
  PAD: 2,
  /** 手机端 */
  PHONE: 3,
} as const

const DEVICE_ID_KEY = 'xy_chat_device_id'

/**
 * 获取设备唯一标识
 *
 * 首次访问时生成随机 UUID 并持久化到 localStorage，
 * 后续登录均上报同一标识，用于区分同类型下的不同物理设备。
 */
export function getDeviceId(): string {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY)
  if (!deviceId) {
    deviceId = generateDeviceId()
    localStorage.setItem(DEVICE_ID_KEY, deviceId)
  }
  return deviceId
}

/**
 * 检测当前设备类型：1=PC端 2=平板端 3=手机端
 *
 * 桌面客户端固定为 PC 端；浏览器依据 User-Agent 推断
 * （iPad 的 Safari UA 与 Mac 一致，需借助触摸点数进一步区分）。
 */
export function detectDeviceType(): number {
  if (isTauri()) {
    return DeviceTypeEnum.PC
  }

  const ua = navigator.userAgent.toLowerCase()

  if (ua.includes('ipad') || ua.includes('tablet') || ua.includes('pad')) {
    return DeviceTypeEnum.PAD
  }
  if (ua.includes('macintosh') && navigator.maxTouchPoints > 1) {
    return DeviceTypeEnum.PAD
  }
  if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone')) {
    return DeviceTypeEnum.PHONE
  }

  return DeviceTypeEnum.PC
}

/**
 * 登录时上报的设备信息
 */
export function getDeviceInfo(): { deviceType: number; deviceId: string } {
  return {
    deviceType: detectDeviceType(),
    deviceId: getDeviceId(),
  }
}

function generateDeviceId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
}