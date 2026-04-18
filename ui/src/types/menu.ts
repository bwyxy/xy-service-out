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
