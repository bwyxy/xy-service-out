import request from '@/utils/request'

/**
 * 根据用户 uId 查询用户信息
 * */
export function getUserInfo(uId) {
  return request({
    url: `/user/${uId}`,
    method: 'GET',
  })
}

/**
 * 根据系统标识 查询系统用户信息
 * */
export function getSystemUserInfo(systemKey) {
  return request({
    url: `/user/system/${systemKey}`,
    method: 'GET',
  })
}
