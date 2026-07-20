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

insert into t_user (u_id, password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time) values
    (10000000, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '管理员', '', '18637649731', 'bwyxy@qq.com', '0', '1', '', '0', now(), now());