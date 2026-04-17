-- 用户表
create table if not exists sys_user (
    id bigint auto_increment not null comment '用户Id',
    username varchar(30) default '' comment '用户名',
    password varchar(255) default '' comment '密码',
    nickname varchar(30) default '' comment '昵称',
    email varchar(50) default '' comment '邮箱',
    phone varchar(11) default '' comment '手机号',
    sex char(1) default '0' comment '性别 (0 未知 1 男 2 女)',
    avatar varchar(100) default '' comment '头像路径',
    status char(1) default '0' comment '帐号状态 (0 正常 1 停用)',
    wx_open_id varchar(255) default '' comment '微信openid',
    create_time datetime default null comment '创建时间',
    update_time datetime default null comment '更新时间',
    create_by varchar(64) default '' comment '创建者',
    update_by varchar(64) default '' comment '更新者',
    deleted tinyint default '0' comment '删除标志 (0 存在 1 删除)',
    remark varchar(500) default null comment '备注',
    primary key (id),
    unique key uk_username(username)
) engine = InnoDB default CHARSET=utf8mb4 COMMENT='用户表';

create table if not exists sys_role (
  id bigint unsigned auto_increment primary key comment '角色Id',
  role_name varchar(50) not null unique comment '角色名',
  role_key varchar(100) not null comment '角色权限字符串',
    role_sort int not null comment '显示顺序',
    data_scope char(1) not null comment '数据范围 (1 全部 2 自定义)',
  status tinyint default 1 comment '状态: 0-禁用 1正常',
  create_time datetime default current_timestamp,
  update_time datetime default current_timestamp on update current_timestamp,
  create_by varchar(64) default '' comment '创建者',
  update_by varchar(64) default '' comment '更新者',
  deleted tinyint default '0' comment '删除标志 (0 存在 1 删除)',
  remark varchar(500) default null comment '备注',
  index idx_code(role_key),
  index idx_status(status),
    unique key uk_role_key(role_key)
) engine=InnoDB comment '角色表';

-- 菜单表
create table sys_menu (
  menu_id bigint(20) not null auto_increment comment '菜单ID',
  menu_name varchar(50) not null comment '菜单名称',
  parent_id bigint(20) default 0 comment '父菜单ID',
  order_num int(4) default 0 comment '显示顺序',
  path varchar(200) default '' comment '路由地址',
  component varchar(255) default null comment '组件路径',
  query varchar(255) default null comment '路由参数',
  route_name varchar(50) default '' comment '路由名称',
  is_frame int(1) default 1 comment '是否为外链（0是 1否）',
  is_cache int(1) default 0 comment '是否缓存（0缓存 1不缓存）',
  menu_type char(1) default '' comment '菜单类型（M目录 C菜单 F按钮）',
  visible char(1) default 0 comment '菜单状态（0显示 1隐藏）',
  status char(1) default 0 comment '菜单状态（0正常 1停用）',
  perms varchar(100) default null comment '权限标识',
  icon varchar(100) default '#' comment '菜单图标',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime comment '更新时间',
  deleted tinyint default '0' comment '删除标志 (0 存在 1 删除)',
  remark varchar(500) default '' comment '备注',
  primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- 用户和角色关联表
drop table if exists sys_user_role;
create table sys_user_role (
   user_id   bigint(20) not null comment '用户ID',
   role_id   bigint(20) not null comment '角色ID',
   primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- 角色和菜单关联表
drop table if exists sys_role_menu;
create table sys_role_menu (
   role_id   bigint(20) not null comment '角色ID',
   menu_id   bigint(20) not null comment '菜单ID',
   primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

insert into sys_user values
    (10000000, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'todo@qq.com', '13888888888', '1', '', '0', '', sysdate(), sysdate(), '', '', 0, '');

insert into sys_role values
    (1, '超级管理员', 'admin', 1, '1', '1', sysdate(), sysdate(), '', '', 0, '');

insert into sys_user_role values (10000000, 1);