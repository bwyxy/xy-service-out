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
    update_time datetime comment '更新时间',
    unique index uk_phone (phone),
    unique index uk_email (email)
) engine = InnoDB default CHARSET=utf8mb4  comment '用户表';

alter table t_user auto_increment 10000003;

insert into t_user (password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time)
    VALUES ('$2a$10$HVwA2Z6fkWn3ufP9Cbcl9u/AcJ0iAtC1xELB/nIa4xCuqgLTciVkS', 'test1', '', '18699999999', 'test1@qq.com', 0, 1, '', '1', now(), now()),
           ('$2a$10$HVwA2Z6fkWn3ufP9Cbcl9u/AcJ0iAtC1xELB/nIa4xCuqgLTciVkS', 'test2', '', '17699999999', 'test2@qq.com', 0, 1, '', '1', now(), now());


-- 群组表
create table t_group (
    group_id bigint primary key auto_increment comment '群组Id',
    name varchar(64) default '' comment '群组名称',
    avatar varchar(255) default '' comment '群头像',
    owner_uid bigint not null comment '群主Id',
    is_ban int default 0 comment '是否允许发言',
    disband int default 0 comment '是否解散群组',
    notice varchar(500) default '' comment '群简介/公告摘要',
    max_member int default 200 comment '最大成员数',
    join_type tinyint default 0 comment '加群方式 0-邀请 1-扫码 2-自由加入',
    invite_permission tinyint default 1 comment '邀请权限 0-仅管理员 1-所有成员',
    speak_permission tinyint default 1 comment '发言权限 0-仅管理员 1-所有成员',
    history_visible tinyint default 1 comment '新成员是否可见历史消息 0-不可见 1-可见',
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
    role int default 0 comment '角色 0-成员 1-管理员 2-群主',
    status tinyint default 1 comment '状态 1-正常 0-已退出',
    joined_at datetime comment '加入时间',
    exit_time datetime default null comment '退出时间',
    mute_end_time datetime default null comment '禁言结束时间',
    group_nickname varchar(50) default '' comment '群内昵称',
    last_ack_seq bigint default 0 comment '已读到的最大消息seq'
) engine = InnoDB default CHARSET=utf8mb4  comment '群组成员表';

create table t_group_announcement (
  id bigint primary key auto_increment comment '公告ID',
    group_id bigint not null comment '群组ID',
  title varchar(100) default '' comment '标题',
    content text comment '内容',
  publisher_uid bigint not null comment '发布人Id',
  is_pinned tinyint default 1 comment '是否置顶',
  create_time datetime comment '创建时间',
  update_time datetime comment '更新时间'
) engine = InnoDB default CHARSET=utf8mb4  comment '群公告表';

-- 好友表
create table t_friend (
    id bigint primary key auto_increment comment 'id',
    u_id bigint not null comment '用户Id',
    friend_id bigint not null comment '好友用户ID',
    status char(1) default '0' comment '状态 0-正常 1-拉黑',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    unique index uk_user_friend (u_id, friend_id)
) engine = InnoDB default CHARSET=utf8mb4  comment '好友表';

insert into t_friend (u_id, friend_id, status, create_time, update_time) VALUES
    (10000000, 10000002, 0, now(), now()),
    (10000002, 10000000, 0, now(), now());

-- 好友请求表
create table t_friend_request (
    id bigint primary key auto_increment comment 'id',
    from_uid bigint not null comment '请求发送者用户ID',
    to_uid bigint not null comment '请求接收者用户ID',
    status tinyint default 0 comment '状态 0-待处理 1-已同意 2-已拒绝 3-已取消 4-已过期',
    apply_msg varchar(255) default '' comment '申请备注消息',
    reply_msg varchar(255) default '' comment '处理回复消息',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    unique index uk_from_to (from_uid, to_uid),
    index idx_to_uid_status (to_uid, status)
) engine = InnoDB default CHARSET=utf8mb4  comment '好友请求表';

-- 通知表
create table t_notification (
    id bigint primary key auto_increment comment 'id',
    u_id bigint not null comment '接收通知的用户ID',
    type tinyint not null comment '通知类型 1-好友请求 2-好友同意 3-系统通知',
    title varchar(100) default '' comment '通知标题',
    content varchar(500) default '' comment '通知内容',
    related_id bigint comment '关联业务记录ID（如好友请求ID）',
    is_read tinyint default 0 comment '是否已读 0-未读 1-已读',
    extra json comment '扩展信息',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    index idx_user_read (u_id, is_read),
    index idx_user_type (u_id, type)
) engine = InnoDB default CHARSET=utf8mb4  comment '通知表';

CREATE TABLE t_private_message_extra (
     `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
     `channel_id` varchar(64) NOT NULL COMMENT '频道ID',
     `channel_type` tinyint NOT NULL COMMENT '频道类型 1-个人 2-群',
     `message_seq` bigint NOT NULL COMMENT '消息seq',
     `from_uid` bigint NOT NULL COMMENT '发送者',
     `to_uid` bigint COMMENT '接收者（单聊）',
     `is_recall` tinyint DEFAULT 0 COMMENT '是否已撤回',
     `recall_time` datetime DEFAULT NULL COMMENT '撤回时间',
     `read_count` int DEFAULT 0 COMMENT '已读人数',
     `extra` json COMMENT '扩展字段',
     `create_time` datetime COMMENT '创建时间',
     UNIQUE INDEX uk_channel_seq (channel_id, channel_type, message_seq),
     INDEX idx_from_uid (from_uid),
     INDEX idx_to_uid (to_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息扩展信息';

CREATE TABLE t_conversation_setting (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `u_id` bigint NOT NULL COMMENT '用户ID',
    `channel_id` varchar(64) NOT NULL COMMENT '频道ID',
    `channel_type` tinyint NOT NULL COMMENT '频道类型',
    `mute` tinyint DEFAULT 0 COMMENT '是否免打扰',
    `top` tinyint DEFAULT 0 COMMENT '是否置顶',
    `draft` varchar(500) DEFAULT '' COMMENT '草稿',
    `cleared_seq` bigint DEFAULT 0 COMMENT '删除聊天记录时的消息序号',
    `deleted_seq` bigint DEFAULT 0 COMMENT '删除会话时的消息序号',
    `create_time` datetime COMMENT '创建时间',
    `update_time` datetime COMMENT '更新时间',
    UNIQUE INDEX uk_user_channel (u_id, channel_id, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话个人设置';

-- 已存在的库执行以下迁移

CREATE TABLE t_rtc_room (
    `room_id` varchar(64) PRIMARY KEY COMMENT '房间ID',
    `room_type` tinyint NOT NULL COMMENT '房间类型 1-语音 2-视频 3-会议',
    `host_uid` bigint NOT NULL COMMENT '房主/主持人',
    `channel_id` varchar(64) COMMENT '关联悟空IM频道（单聊=对端uid，群聊=g_{groupId}）',
    `channel_type` tinyint NOT NULL DEFAULT 1 COMMENT '频道类型 1-单聊 2-群聊',
    `max_participants` int DEFAULT 2 COMMENT '最大人数',
    `status` tinyint DEFAULT 0 COMMENT '状态 0-呼叫中 1-通话中 2-已结束',
    `start_time` datetime COMMENT '开始时间',
    `end_time` datetime COMMENT '结束时间',
    `sfu_node` varchar(255) COMMENT '分配的SFU节点',
    `create_time` datetime COMMENT '创建时间',
    INDEX idx_status (status),
    INDEX idx_host_uid (host_uid),
    INDEX idx_channel (channel_id, channel_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTC房间';

CREATE TABLE t_rtc_participant (
   `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
   `room_id` varchar(64) NOT NULL COMMENT '房间ID',
   `u_id` bigint NOT NULL COMMENT '用户ID',
   `role` tinyint DEFAULT 0 COMMENT '角色 0-普通 1-主持人',
   `status` tinyint DEFAULT 0 COMMENT '状态 0-呼叫中 1-已加入 2-已离开 3-拒绝 4-忙线',
   `join_time` datetime COMMENT '加入时间',
   `leave_time` datetime COMMENT '离开时间',
   `audio_muted` tinyint DEFAULT 0 COMMENT '是否静音',
   `video_muted` tinyint DEFAULT 0 COMMENT '是否关闭视频',
   `create_time` datetime COMMENT '创建时间',
   UNIQUE INDEX uk_room_user (room_id, u_id),
   INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTC房间参与者';

insert into t_user (u_id, password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time) values
    (10000000, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '管理员', '', '18637649731', 'bwyxy@qq.com', '0', '1', '', '0', now(), now());

insert into t_user (u_id, password, nickname, avatar, phone, email, status, sex, wx_open_id, is_bind_chat, create_time, update_time) values
    (10000001, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '系统管理员', '', '18637649731', '1547124996@qq.com', '0', '1', '', '0', now(), now()),
    (10000002, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '飞鸽AI', '', '18637649731', 'email@qq.com', '0', '1', '', '0', now(), now());

select u.u_id, u.nickname, u.avatar, u.sex, tgm.role, tgm.status, tgm.group_nickname from t_user u
    left join t_group_member tgm on tgm.u_id = u.u_id
    where tgm.group_id = 9