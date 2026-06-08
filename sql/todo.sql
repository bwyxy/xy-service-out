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

alter table sys_menu modify column is_frame tinyint default '1' comment '是否为外链（0是 1否）';
alter table sys_menu modify column is_cache tinyint default '0' comment '是否缓存（0缓存 1不缓存）';

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

-- 内容配置表
create table sys_config (
    config_id bigint primary key auto_increment comment '配置ID',
    parent_id bigint default 0 comment '父配置ID',
    config_name varchar(255) not null comment '配置名称',
    config_value varchar(500) default '' comment '配置值',
    config_link varchar(255) default '' comment '配置链接',
    config_type varchar(100) default '' comment '配置类型',
    status tinyint default 1 comment '0-隐藏 1-显示',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime comment '更新时间',
    deleted tinyint default '0' comment '删除标志 (0 存在 1 删除)'
) engine=innodb comment = '内容配置表';

create index idx_config_type on sys_config(config_type);
create index idx_status on sys_config(status);

insert into sys_user values
    (10000000, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'todo@qq.com', '13888888888', '1', '', '0', '', sysdate(), sysdate(), '', '', 0, '');
insert into sys_user values
    (10000001, 'xiaoyi', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '小忆', 'todo@qq.com', '13888888888', '1', '', '0', '', sysdate(), sysdate(), '', '', 0, '');

insert into sys_role values
    (1, '超级管理员', 'admin', 1, '1', '1', sysdate(), sysdate(), '', '', 0, '');
insert into sys_role values
    (2, '普通用户', 'user', 2, '2', '1', sysdate(), sysdate(), '', '', 0, '');


insert into sys_user_role values (10000000, 1);
insert into sys_user_role values (10000001, 2);

insert into sys_menu values(1, '系统管理', '0', 1, 'system',null, '', '', 1, 1, 'M', '0', '0', '', 'system',   'admin', sysdate(), '', null, 0,'系统管理目录');
insert into sys_menu values(2,  '用户管理', 1,   1, 'user','system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', sysdate(), '', null, 0,'用户管理菜单');
insert into sys_menu values(4,  '角色管理', 1,   2, 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', sysdate(), '', null, 0, '角色管理菜单');
insert into sys_menu values(5,  '菜单管理', 1,   3, 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', sysdate(), '', null, 0,'菜单管理菜单');
insert into sys_menu values (3,'仪表盘', 0,1,'/', 'dashboard/index', '',1,1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '仪表盘');
insert into sys_menu values
    (6,'我的博客', 0,2,'/blog', 'blog/index', '',1,1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '我的博客');

insert into sys_menu values
    (7,'文章管理', 0,3,'article', null, '','',1,1,'M','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '文章管理'),
    (8,'文章中心', 0,1,'article', '/article/article/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '文章管理'),
    (9,'标签管理', 0,2,'tag', '/article/tag/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '标签管理'),
    (10,'分类管理', 0,3,'category', '/article/category/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '分类管理');

insert into sys_menu values
    (14,'内容配置', 0,2,'config', null, '','',1,1,'M','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '内容配置');

insert into sys_menu values
    (15,'菜单配置', 14,1,'menu', 'config/menu/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '菜单配置'),
    (16,'工具配置', 14,2,'utils', 'config/utils/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '工具配置'),
    (17,'底部栏', 14,3,'tabbar', 'config/tabbar/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '底部栏');
insert into sys_menu values
    (18,'轮播图', 14,4,'swiper', 'config/swiper/index', '','',1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '轮播图');

insert into sys_menu values
    (11,'添加文章', 7,4,'add', '/article/article/operate', '',1,1,0,'C','1','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '添加文章'),
    (12,'编辑文章', 7,4,'edit', '/article/article/operate', '',1,1,0,'C','1','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '编辑文章');

insert into sys_menu values
    (13,'内容配置', 0,4,'/contentItem/index', 'contentItem/index', '',1,1,0,'C','0','0', '','dashboard' ,'admin',sysdate(),'', sysdate(),0, '通用内容配置');

# 2005,仪表盘,0,1,/,dashboard/index,,"",1,1,C,0,0,,dashboard,hzy,2025-12-24 13:16:11,"",,""

select distinct m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.query, m.route_name, m.is_frame, m.is_cache, m.menu_type, m.visible, m.status, m.perms, m.icon, m.create_by, m.create_time, m.update_by, m.update_time, m.deleted, m.remark from sys_menu m;

delete from sys_user where id = 2046084966175932418;

delete from sys_user where id = 10000002;

alter table sys_user auto_increment = 10000002;