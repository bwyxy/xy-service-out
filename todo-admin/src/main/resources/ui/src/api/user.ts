import type { RuoYiResponse } from '@/utils/request'
import request from '@/utils/request'

/** 用户信息接口 */
export interface UserInfo {
  userId: number
  deptId: number
  userName: string
  nickName: string
  email: string
  phonenumber: string
  sex: string
  avatar: string
  status: string
  delFlag: string
  loginIp: string
  loginDate: string
  createBy: string
  createTime: string
  updateBy: string
  updateTime: string
  remark: string
  roles: Role[]
  perms: string[]
}

/** 角色信息接口 */
export interface Role {
  roleId: number
  roleName: string
  roleKey: string
  roleSort: number
  dataScope: string
  menuCheckStrictly: boolean
  deptCheckStrictly: boolean
  status: string
  flag: boolean
  createTime: string
  remark: string
  admin: boolean
}

/** 获取用户信息 */
export async function getUserInfo(): Promise<UserInfo> {
  const res = await request.get<RuoYiResponse<UserInfo>>('/auth/getInfo')
  const body = res.data
  return body.data
}