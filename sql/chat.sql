-- 创建用户表
create table t_user (
    u_id bigint primary key auto_increment comment 'UID',
    password varchar(500) not null comment '密码',
    nickname varchar(50) not null comment '昵称',
    avatar varchar(255) default '' comment '头像 URl',
    phone varchar(11) default '' comment '中国大陆手机号',
    email varchar(50) default '' comment '邮箱',
    status char(1) default '0' comment '用户状态 0-正常 1-禁用',
    sex char(1) default '0' comment '性别 0-未知 1-男 2-女',
    wx_open_id varchar(255) default '' comment '微信open_id',
    is_bind_chat char(1) default '1' comment '是否绑定聊天应用 0-未绑定 1-已绑定',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间'
) engine = InnoDB default CHARSET=utf8mb4  comment '用户表';

-- 群组表
create table t_group (
    group_id bigint primary key auto_increment comment '群组Id',
    name varchar(64) default '' comment '群组名称',
    avatar varchar(255) default '' comment '群头像',
    owner_uid bigint not null comment '群主Id',
    is_ban int default 0 comment '是否允许发言',
    disband int default 0 comment '是否解散群组',
    create_by bigint not null comment '创建者',
    update_by bigint not null comment '更新者',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间'
) engine = InnoDB default CHARSET=utf8mb4  comment '群组表';

-- 群组成员表
create table t_group_member (
    id bigint primary key auto_increment comment 'id',
    group_id bigint not null comment '关联群组ID',
    u_id bigint not null comment '用户Id',
    role int default 0 comment '角色 0-成员 1-管理员',
    status char(1) default '0' comment '状态 0-正常 1-已退出',
    joined_at datetime comment '加入时间'
) engine = InnoDB default CHARSET=utf8mb4  comment '群组成员表';

-- 好友表
create table t_friend (
    id bigint primary key auto_increment comment 'id',
    u_id bigint comment '用户Id',
    friend_id bigint comment '好友用户ID',
    status char(1) default '0' comment '状态 0-正常 1-拉黑',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间'
) engine = InnoDB default CHARSET=utf8mb4  comment '好友表';

insert into t_user (u_id, password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time) values
    (10000000, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '管理员', '', '18637649731', 'bwyxy@qq.com', '0', '1', '', '0', now(), now());

insert into t_user (u_id, password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time) values
    (10000001, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '系统管理员', '', '18637649731', '1547124996@qq.com', '0', '1', '', '0', now(), now()),
    (10000002, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '飞鸽AI', '', '18637649731', 'email@qq.com', '0', '1', '', '0', now(), now());