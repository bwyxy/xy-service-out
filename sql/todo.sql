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

-- 待办任务表
create table xy_task (
    task_id bigint primary key auto_increment comment '任务Id',
    user_id bigint comment '用户Id',
    task_title varchar(255) default '' comment '待办标题',
    description text comment '描述',
    completed tinyint default 0 comment '是否完成 0-未完成 1-已完成',
    urgency_level int default 1 comment '紧急程度: 1-不重要不紧急 2-紧急不重要 3-重要不紧急 4-重要且紧急',
    due_date datetime comment '截止时间',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime comment '更新时间',
    deleted tinyint default '0' comment '删除标志 (0 存在 1 删除)',
    index idx_user_id (user_id),
    index idx_completed (completed),
    index idx_task_title (task_title),
    index idx_urgency_level (urgency_level),
    index idx_due_date (due_date),
    constraint fk_sys_user foreign key (user_id) references sys_user(id) on delete cascade
) engine=innodb comment = '待办任务表';

-- 任务协作成员表
create table xy_task_collaborator (
    task_id bigint not null comment '关联任务Id',
    user_id bigint not null comment '用户Id',
    is_admin varchar(1) default '0' comment '是否任务管理员',
    joined_at datetime default current_timestamp comment '加入时间',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime comment '更新时间',
    primary key (task_id, user_id),
    foreign key (task_id) references xy_task(task_id) on delete cascade,
    foreign key (user_id) references sys_user(id) on delete cascade
) engine=innodb comment = '任务协作成员表';

-- 待办标签表
create table xy_tag (
    tag_id bigint primary key auto_increment comment '标签Id',
    user_id bigint comment '用户Id',
    tag_name varchar(24) not null comment '标签名称',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime comment '更新时间',
    foreign key (user_id) references sys_user(id) on delete cascade
) engine=innodb comment = '待办标签表';

-- 系统英汉词典数据
create table sys_dictionary (
    id bigint primary key auto_increment comment '词典Id',
    word varchar(100) not null comment '单词',
    phonetic varchar(100) default null comment '音标',
    definition text comment '英文释义',
    translation text comment '中文释义',
    pos varchar(20) default null comment '词性',
    collins tinyint default null comment '柯林斯星级 1-5',
    oxford tinyint default 0 comment '牛津核心词标记',
    tag varchar(50) default null comment '标签 (cet4,cet6,ielts)',
    bnc int default null comment 'BNC词频排名',
    frq int default null comment '当代语料库词频排名',
    exchange varchar(255) default null comment '词形变化',
    detail JSON default null comment '额外信息(例句等)',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
    index idx_tag (tag),
    index idx_collins (collins)
) engine=innodb comment '系统英汉词典数据';

insert into xy_tag values
    (1, 10000000,'工作', 'admin', now(), 'admin', now()),
    (2, 10000000,'学习', 'admin', now(), 'admin', now()),
    (3, 10000000,'生活', 'admin', now(), 'admin', now()),
    (4, 10000000,'旅行', 'admin', now(), 'admin', now()),
    (5, 10000000,'健康', 'admin', now(), 'admin', now()),
    (6, 10000000,'购物', 'admin', now(), 'admin', now());

-- 待办任务标签关联表
create table xy_task_tag (
    task_id bigint not null comment '任务Id',
    tag_id bigint not null comment '标签Id',
    primary key (task_id, tag_id),
    index idx_task_id (task_id),
    index idx_tag_id (tag_id),
    constraint fk_task_id foreign key (task_id) references xy_task(task_id),
    constraint fk_tag_id foreign key (tag_id) references xy_tag(tag_id) on delete cascade
) engine=innodb comment = '待办任务标签关联表';

insert into sys_user values
    (10000000, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'xy@qq.com', '13888888888', '1', '', '0', '', sysdate(), sysdate(), '', '', 0, '');
insert into sys_user values
    (10000001, 'xiaoyi', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '小忆', 'xy@qq.com', '13888888888', '1', '', '0', '', sysdate(), sysdate(), '', '', 0, '');

update sys_user set password = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', nickname = '超级管理员', email = 'xy@qq.com', phone = '13888888888', sex = '1', avatar = '', status = '0', wx_open_id = '', create_time = sysdate(), update_time = sysdate(), create_by = '', update_by = '', deleted = 0, remark = '' where id = 10000000;
update sys_user set password = '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', nickname = '小忆', email = 'xy@qq.com', phone = '13888888888', sex = '1', avatar = '', status = '0', wx_open_id = '', create_time = sysdate(), update_time = sysdate(), create_by = '', update_by = '', deleted = 0, remark = '' where id = 10000001;


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
insert into sys_menu values(19, '操作日志', 1,   4, 'operlog',    'system/operlog/index',     '', '', 1, 0, 'C', '0', '0', 'system:operlog:list',   'log',           'admin', sysdate(), '', null, 0, '操作日志菜单');

-- 用户管理按钮权限
insert into sys_menu values(20, '用户新增', 2, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(21, '用户编辑', 2, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(22, '用户删除', 2, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(23, '用户查询', 2, 4, 'query', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '', 'admin', sysdate(), '', null, 0, '');

-- 角色管理按钮权限
insert into sys_menu values(24, '角色新增', 4, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(25, '角色编辑', 4, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(26, '角色删除', 4, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(27, '角色查询', 4, 4, 'query', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '', 'admin', sysdate(), '', null, 0, '');

-- 操作日志按钮权限
insert into sys_menu values(28, '日志删除', 19, 1, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(29, '日志清空', 19, 2, 'clean', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:clean', '', 'admin', sysdate(), '', null, 0, '');

-- 操作日志表
 create table if not exists sys_oper_log (
    id bigint auto_increment not null comment '日志主键',
    title varchar(50) default '' comment '模块标题',
    business_type int(2) default 0 comment '业务类型（0其它 1新增 2修改 3删除）',
    method varchar(200) default '' comment '方法名称',
    request_method varchar(10) default '' comment '请求方式',
    operator_type int(1) default 0 comment '操作类别（0其它 1后台用户 2手机端用户）',
    oper_name varchar(50) default '' comment '操作人员',
    dept_name varchar(50) default '' comment '部门名称',
    oper_url varchar(255) default '' comment '请求URL',
    oper_ip varchar(128) default '' comment '主机地址',
    oper_location varchar(255) default '' comment '操作地点',
    oper_param text comment '请求参数',
    json_result text comment '返回参数',
    status int(1) default 0 comment '操作状态（0正常 1异常）',
    error_msg varchar(2000) default '' comment '错误消息',
    oper_time datetime comment '操作时间',
    cost_time bigint default 0 comment '消耗时间',
    create_time datetime default now() comment '创建时间',
    update_time datetime default now() on update now() comment '更新时间',
    primary key (id),
    index idx_title (title),
    index idx_business_type (business_type),
    index idx_oper_name (oper_name),
    index idx_status (status),
    index idx_oper_time (oper_time)
) engine=InnoDB default charset=utf8mb4 comment='操作日志记录表';
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

-- 字典类型表
 create table if not exists sys_dict_type (
    dict_id bigint auto_increment not null comment '字典主键',
    dict_name varchar(100) default '' comment '字典名称',
    dict_type varchar(100) default '' comment '字典类型',
    status char(1) default '0' comment '状态（0正常 1停用）',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime default null comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime default null comment '更新时间',
    remark varchar(500) default null comment '备注',
    primary key (dict_id),
    unique key uk_dict_type (dict_type)
) engine=InnoDB default charset=utf8mb4 comment='字典类型表';

-- 字典数据表
 create table if not exists sys_dict_data (
    dict_code bigint auto_increment not null comment '字典编码',
    dict_sort int(4) default 0 comment '字典排序',
    dict_label varchar(100) default '' comment '字典标签',
    dict_value varchar(100) default '' comment '字典键值',
    dict_type varchar(100) default '' comment '字典类型',
    css_class varchar(100) default null comment '样式属性（其他样式扩展）',
    list_class varchar(100) default null comment '表格回显样式',
    is_default char(1) default 'N' comment '是否默认（Y是 N否）',
    status char(1) default '0' comment '状态（0正常 1停用）',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime default null comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime default null comment '更新时间',
    remark varchar(500) default null comment '备注',
    primary key (dict_code),
    index idx_dict_type (dict_type)
) engine=InnoDB default charset=utf8mb4 comment='字典数据表';

-- 登录日志表
 create table if not exists sys_logininfor (
    info_id bigint(20) not null auto_increment comment '访问ID',
    user_name varchar(50) default '' comment '用户账号',
    ipaddr varchar(128) default '' comment '登录IP地址',
    login_location varchar(255) default '' comment '登录地点',
    browser varchar(50) default '' comment '浏览器类型',
    os varchar(50) default '' comment '操作系统',
    status char(1) default '0' comment '登录状态（0成功 1失败）',
    msg varchar(255) default '' comment '提示消息',
    login_time datetime comment '访问时间',
    create_time datetime default now() comment '创建时间',
    update_time datetime default now() comment '更新时间',
    primary key (info_id),
    index idx_user_name (user_name),
    index idx_ipaddr (ipaddr),
    index idx_status (status),
    index idx_login_time (login_time)
) engine=InnoDB default charset=utf8mb4 comment='系统访问记录';

-- 新增系统管理子菜单
insert into sys_menu values(30, '字典管理', 1, 5, 'dict', 'system/dict/index', '', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', sysdate(), '', null, 0, '字典管理菜单');
insert into sys_menu values(31, '登录日志', 1, 6, 'logininfor', 'system/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'system:logininfor:list', 'logininfor', 'admin', sysdate(), '', null, 0, '登录日志菜单');
insert into sys_menu values(32, '在线用户', 1, 7, 'online', 'system/online/index', '', '', 1, 0, 'C', '0', '0', 'system:online:list', 'online', 'admin', sysdate(), '', null, 0, '在线用户菜单');

-- 字典管理按钮权限
insert into sys_menu values(33, '字典类型新增', 30, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(34, '字典类型编辑', 30, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(35, '字典类型删除', 30, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(36, '字典类型查询', 30, 4, 'query', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(37, '字典数据新增', 30, 5, 'add', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(38, '字典数据编辑', 30, 6, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(39, '字典数据删除', 30, 7, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(40, '字典数据查询', 30, 8, 'query', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:query', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(41, '字典数据导出', 30, 9, 'export', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:export', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(42, '字典数据导入', 30, 10, 'import', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:data:import', '', 'admin', sysdate(), '', null, 0, '');

-- 登录日志按钮权限
insert into sys_menu values(43, '登录日志删除', 31, 1, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(44, '登录日志清空', 31, 2, 'clean', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:clean', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(45, '登录日志导出', 31, 3, 'export', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:export', '', 'admin', sysdate(), '', null, 0, '');

-- 在线用户按钮权限
insert into sys_menu values(46, '在线用户查询', 32, 1, 'query', '', '', '', 1, 0, 'F', '0', '0', 'system:online:query', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(47, '在线用户强退', 32, 2, 'force', '', '', '', 1, 0, 'F', '0', '0', 'system:online:force', '', 'admin', sysdate(), '', null, 0, '');

-- 操作日志导出按钮
insert into sys_menu values(48, '操作日志导出', 19, 3, 'export', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:export', '', 'admin', sysdate(), '', null, 0, '');

-- 默认字典数据
insert into sys_dict_type values(1, '用户性别', 'sys_user_sex', '0', 'admin', sysdate(), '', null, '用户性别列表');
insert into sys_dict_type values(2, '菜单状态', 'sys_show_hide', '0', 'admin', sysdate(), '', null, '菜单状态列表');
insert into sys_dict_type values(3, '系统状态', 'sys_normal_disable', '0', 'admin', sysdate(), '', null, '系统状态列表');

insert into sys_dict_data values(1, 1, '男', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(2, 2, '女', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(3, 3, '未知', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(6, 1, '正常', '0', 'sys_normal_disable', '', 'success', 'Y', '0', 'admin', sysdate(), '', null, '');
insert into sys_dict_data values(7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', sysdate(), '', null, '');

select distinct m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.query, m.route_name, m.is_frame, m.is_cache, m.menu_type, m.visible, m.status, m.perms, m.icon, m.create_by, m.create_time, m.update_by, m.update_time, m.deleted, m.remark
from sys_menu m
         left join sys_role_menu rm on m.menu_id = rm.menu_id
         left join sys_user_role ur on rm.role_id = ur.role_id
         left join sys_role ro on ur.role_id = ro.id
         left join sys_user u on ur.user_id = u.id
where u.id = 10000001 and m.menu_type in ('M', 'C') and m.status = '0'  AND ro.status = 1
order by m.parent_id, m.order_num;

create table sys_im_user (
    id bigint auto_increment primary key comment '账号Id',
    avatar varchar(255) comment '头像',
    nickname varchar(64) comment '系统账号名称',
    user_key varchar(64) comment '账号标识',
    wk_im_key varchar(64) comment '悟空IM系统账号标识',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间'
) engine=InnoDB default charset=utf8mb4 comment '悟空IM系统账号';

-- 待办管理菜单
insert into sys_menu values(2000, '待办管理', '0', 5, 'todo', null, '', '', 1, 1, 'M', '0', '0', '', 'list', 'admin', sysdate(), '', null, 0, '待办管理目录');
insert into sys_menu values(2001, '任务管理', 2000, 1, 'task', 'todo/task/index', '', '', 1, 0, 'C', '0', '0', 'todo:task:list', 'checkbox', 'admin', sysdate(), '', null, 0, '任务管理菜单');
insert into sys_menu values(2002, '标签管理', 2000, 2, 'tag', 'todo/tag/index', '', '', 1, 0, 'C', '0', '0', 'todo:tag:list', 'collection-tag', 'admin', sysdate(), '', null, 0, '标签管理菜单');

-- 任务管理按钮权限
insert into sys_menu values(2010, '任务新增', 2001, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'todo:task:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2011, '任务编辑', 2001, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'todo:task:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2012, '任务删除', 2001, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'todo:task:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2013, '任务查询', 2001, 4, 'query', '', '', '', 1, 0, 'F', '0', '0', 'todo:task:query', '', 'admin', sysdate(), '', null, 0, '');

-- 标签管理按钮权限
insert into sys_menu values(2020, '标签新增', 2002, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'todo:tag:add', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2021, '标签编辑', 2002, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'todo:tag:edit', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2022, '标签删除', 2002, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'todo:tag:remove', '', 'admin', sysdate(), '', null, 0, '');
insert into sys_menu values(2023, '标签查询', 2002, 4, 'query', '', '', '', 1, 0, 'F', '0', '0', 'todo:tag:query', '', 'admin', sysdate(), '', null, 0, '');

-- 为超级管理员角色绑定待办菜单
insert into sys_role_menu values (1, 2000);
insert into sys_role_menu values (1, 2001);
insert into sys_role_menu values (1, 2002);
insert into sys_role_menu values (1, 2010);
insert into sys_role_menu values (1, 2011);
insert into sys_role_menu values (1, 2012);
insert into sys_role_menu values (1, 2013);
insert into sys_role_menu values (1, 2020);
insert into sys_role_menu values (1, 2021);
insert into sys_role_menu values (1, 2022);
insert into sys_role_menu values (1, 2023);