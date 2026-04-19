import { BaseTable } from './BaseTable'

export interface Menu extends BaseTable {
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
  deleted: number;
  remark: string;
  menuId: number;
  menuName: string;
  parentId: number;
  orderNum: number;
  path: string;
  component: string;
  query: string;
  routeName: string;
  isFrame: string;
  isCache: string;
  menuType: string;
  visible: string;
  status: string;
  perms: string;
  icon: string;
  children: Menu[];
}

/** 若依后端 /getRouters 常见菜单结构（与 Vue 版对齐） */
export interface RouterMenu {
  name?: string
  path?: string
  hidden?: boolean
  component?: string
  redirect?: string
  alwaysShow?: boolean
  meta?: {
    title?: string
    icon?: string
    noCache?: boolean
    link?: string
  }
  children?: RouterMenu[]
}
