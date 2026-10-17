/** 设备类型分组，与后端 DeviceType 枚举保持一致 */
export const DeviceTypeEnum = {
  /** PC 端 */
  PC: 1,
  /** 平板端 */
  PAD: 2,
  /** 手机端 */
  PHONE: 3,
}

const DEVICE_ID_KEY = 'xy_chat_device_id'

/**
 * 获取设备唯一标识
 *
 * 首次启动时生成随机串并持久化，后续登录均上报同一标识，
 * 用于区分同类型设备下的不同物理设备（配合后端多端登录管控）。
 */
export function getDeviceId() {
  let deviceId = uni.getStorageSync(DEVICE_ID_KEY)

  if (!deviceId) {
    deviceId = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
    uni.setStorageSync(DEVICE_ID_KEY, deviceId)
  }

  return deviceId
}

/**
 * 登录时上报的设备信息：移动端固定为手机端
 */
export function getDeviceInfo() {
  return {
    deviceType: DeviceTypeEnum.PHONE,
    deviceId: getDeviceId(),
  }
}
