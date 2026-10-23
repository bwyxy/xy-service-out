-- 待办小程序
set foreign_key_checks = 0;

-- --------------------------------------------------------------------------
-- 客户端用户表
-- u_id 是待办侧唯一的用户主键
-- --------------------------------------------------------------------------
create table t_user (
    u_id         bigint       primary key auto_increment comment '客户端用户Id（独立号段，从 1 开始）',
    wx_open_id   varchar(255) default null comment '微信 openid，未绑微信为 NULL',
    nickname     varchar(50)  not null default '' comment '昵称',
    avatar       varchar(255) default '' comment '头像 URL',
    sex          char(1)      default '0' comment '性别 0-未知 1-男 2-女',
    phone        varchar(11)  default null comment '手机号',
    email        varchar(50)  default null comment '邮箱',
    password     varchar(500) not null default '' comment '密码，微信用户为空串',
    status       char(1)      default '0' comment '用户状态 0-正常 1-禁用',
    is_bind_chat char(1)      default '1' comment '是否绑定聊天应用 0-未绑定 1-已绑定',
    create_time  datetime     default current_timestamp comment '创建时间',
    update_time  datetime     default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_wx_open_id (wx_open_id),
    unique key uk_phone (phone),
    unique key uk_email (email)
) engine = InnoDB default charset = utf8mb4 comment = '客户端用户表';

-- --------------------------------------------------------------------------
-- 待办任务
-- --------------------------------------------------------------------------
create table t_todo_task (
    task_id         bigint       primary key auto_increment comment '任务Id',
    user_id         bigint       not null comment '所属用户 t_user.u_id',
    parent_id       bigint       default null comment '父任务Id，子任务用',
    task_title      varchar(255) not null comment '任务标题',
    description     text comment '任务描述',
    completed       bigint       default 0 comment '是否完成 0-未完成 1-已完成',
    completion_time datetime     default null comment '完成时间（统计用）',
    urgency_level   int          default 1 comment '紧急程度 1~4，四象限页用',
    start_time      datetime     default null comment '开始时间',
    due_date        datetime     default null comment '截止时间',
    tag_id          bigint       default null comment '清单Id todo_tag.tag_id',
    priority        tinyint      default 0 comment '优先级 0-无 1-普通 2-重要（列表页小旗子）',
    repeat_rule     varchar(64)  default '' comment '重复规则 DAILY/WEEKLY/MONTHLY，空表示不重复',
    repeat_source_id bigint      default null comment '重复任务：本任务由哪一条完成后生成，用于取消完成时回滚',
    remind_before   int          default 0 comment '提前提醒分钟数，0-不提醒',
    remind_at       datetime     default null comment '下次提醒时间（服务端算好，便于索引扫描）',
    archived        tinyint      default 0 comment '是否归档 0-否 1-是',
    archive_time    datetime     default null comment '归档时间（取消归档时清空）',
    client_id       varchar(64)  default null comment '客户端幂等Id',
    seq             bigint       default 0 comment '用户维度同步序号',
    create_by       varchar(64)  default '' comment '创建者（t_user.u_id 字符串形式）',
    update_by       varchar(64)  default '' comment '更新者',
    deleted         tinyint      default 0 comment '逻辑删除 0-正常 1-已删除',
    create_time     datetime     default current_timestamp comment '创建时间',
    update_time     datetime     default current_timestamp on update current_timestamp comment '更新时间',
    index idx_user_deleted (user_id, deleted),
    index idx_user_due (user_id, due_date),
    index idx_parent (parent_id),
    index idx_repeat_source (repeat_source_id),
    index idx_user_urgency (user_id, urgency_level),
    index idx_user_completed_time (user_id, completion_time),
    index idx_user_tag (user_id, tag_id),
    index idx_user_archived (user_id, archived, completed),
    index idx_archived_completion (archived, completed, completion_time),
    index idx_user_remind (remind_at, deleted),
    index idx_user_seq (user_id, seq),
    unique key uk_user_client (user_id, client_id),
    constraint fk_task_user foreign key (user_id) references t_user (u_id) on delete cascade
) engine = InnoDB default charset = utf8mb4 comment = '待办任务';

-- --------------------------------------------------------------------------
-- 清单（前端叫「清单」，就是 todo_tag）
-- --------------------------------------------------------------------------
create table t_todo_tag (
    tag_id      bigint      primary key auto_increment comment '清单Id',
    user_id     bigint      not null comment '所属用户 t_user.u_id',
    tag_name    varchar(24) not null comment '清单名称',
    icon        varchar(32) default '' comment '图标名（对应前端 y-icon 的 name）',
    color       varchar(16) default '' comment '主题色',
    sort_no     int         default 0 comment '排序',
    is_default  tinyint     default 0 comment '是否默认清单',
    archived    tinyint     default 0 comment '是否归档',
    create_by   varchar(64) default '' comment '创建者（TodoTagMapper 的 admin 查询已引用此列）',
    update_by   varchar(64) default '' comment '更新者（同上）',
    create_time datetime    default current_timestamp comment '创建时间',
    update_time datetime    default current_timestamp on update current_timestamp comment '更新时间',
    deleted tinyint default 0 comment '是否删除',
    index idx_user (user_id),
    index idx_name (tag_name),
    constraint fk_tag_user foreign key (user_id) references t_user (u_id) on delete cascade
) engine = InnoDB default charset = utf8mb4 comment = '待办清单';

-- --------------------------------------------------------------------------
-- 任务-清单关联（前端目前是单选清单，保留结构暂不启用）
-- --------------------------------------------------------------------------
create table t_todo_task_tag (
    task_id bigint not null comment '任务Id',
    tag_id  bigint not null comment '清单Id',
    unique key uk_task_tag (task_id, tag_id),
    index idx_tag (tag_id),
    constraint fk_tt_task foreign key (task_id) references t_todo_task (task_id) on delete cascade,
    constraint fk_tt_tag foreign key (tag_id) references t_todo_tag (tag_id) on delete cascade
) engine = InnoDB default charset = utf8mb4 comment = '任务清单关联';

-- ==========================================================================
-- 以下为设计文档（docs/todo-backend-design.md 第 4.2 节）新增的配套表，
-- user_id 一律指向 t_user.u_id，不指向 sys_user.id。
-- ==========================================================================

-- 用户偏好（提醒设置 / 外观与显示 / 同步）
create table if not exists t_todo_user_setting (
    user_id           bigint primary key comment '用户Id t_user.u_id',
    theme             varchar(16) default 'system' comment 'system/light/dark',
    font_size         varchar(16) default 'standard' comment 'small/standard/large',
    week_start        tinyint     default 1  comment '周起始 0-周日 1-周一',
    task_remind_on    tinyint     default 1  comment '任务提醒开关',
    daily_remind_on   tinyint     default 0  comment '每日提醒开关',
    daily_remind_time varchar(8)  default '08:00' comment '每日提醒时间',
    focus_remind_on   tinyint     default 1  comment '番茄提醒开关',
    quiet_start       varchar(8)  default '22:00' comment '免打扰开始',
    quiet_end         varchar(8)  default '07:00' comment '免打扰结束',
    auto_sync         tinyint     default 1  comment '自动同步/备份开关',
    last_sync_time    datetime    default null comment '上次同步时间',
    create_time       datetime    default current_timestamp,
    update_time       datetime    default current_timestamp on update current_timestamp
) engine = innodb default charset = utf8mb4 comment = '用户偏好设置';

-- 用户资料扩展
# create table if not exists xy_user_profile (
#     user_id     bigint       primary key comment '用户Id t_user.u_id',
#     signature   varchar(100) default '' comment '个性签名',
#     birthday    varchar(10)  default '' comment '生日 yyyy-MM-dd',
#     city        varchar(64)  default '' comment '所在地',
#     update_time datetime     default current_timestamp on update current_timestamp
# ) engine = innodb default charset = utf8mb4 comment = '用户资料扩展';

-- 提醒（一个任务可有多个提醒点）
create table if not exists t_todo_task_reminder (
    reminder_id bigint       primary key auto_increment,
    task_id     bigint       not null comment '任务Id',
    user_id     bigint       not null comment '用户Id t_user.u_id',
    remind_at   datetime     not null comment '提醒时间',
    template_id varchar(64)  default '' comment '订阅消息模板Id',
    status      varchar(16)  default 'pending' comment 'pending/sending/sent/failed/canceled',
    retry_count int          default 0 comment '已重试次数',
    send_time   datetime     default null comment '实际下发时间',
    fail_reason varchar(255) default '' comment '失败原因（errcode+errmsg）',
    create_time datetime     default current_timestamp,
    update_time datetime     default current_timestamp on update current_timestamp,
    index idx_status_remind (status, remind_at),
    index idx_task (task_id),
    index idx_user (user_id, remind_at)
) engine = innodb default charset = utf8mb4 comment = '任务提醒';

-- 一次性订阅授权额度池（一次授权 = 一条消息）
create table if not exists t_todo_subscribe_quota (
    user_id     bigint      not null comment '用户Id t_user.u_id',
    template_id varchar(64) not null comment '模板Id',
    remain      int         not null default 0 comment '剩余可下发条数',
    total       int         not null default 0 comment '累计获得授权次数',
    update_time datetime    default current_timestamp on update current_timestamp,
    primary key (user_id, template_id)
) engine = innodb default charset = utf8mb4 comment = '订阅消息授权额度';

-- 下发日志（排查「用户说没收到提醒」的唯一凭据）
create table if not exists t_todo_notify_log (
    id          bigint       primary key auto_increment,
    user_id     bigint       not null comment '用户Id t_user.u_id',
    task_id     bigint       default null,
    reminder_id bigint       default null,
    template_id varchar(64)  default '',
    page        varchar(128) default '' comment '点击跳转页',
    errcode     int          default 0,
    errmsg      varchar(255) default '',
    send_time   datetime     default current_timestamp,
    index idx_user_time (user_id, send_time)
) engine = innodb default charset = utf8mb4 comment = '订阅消息下发日志';

-- 习惯 / 打卡
create table if not exists t_todo_habit (
    habit_id    bigint      primary key auto_increment,
    user_id     bigint      not null comment '用户Id t_user.u_id',
    name        varchar(64) not null comment '习惯名称',
    icon        varchar(32) default '' comment '图标名',
    color       varchar(16) default '' comment '主题色',
    freq_type   varchar(16) default 'daily' comment 'daily/weekly',
    freq_days   varchar(32) default '' comment 'weekly 时的星期集合 如 1,3,5',
    target_days int         default 0 comment '目标天数（0 不限）',
    sort_no     int         default 0,
    status      tinyint     default 1 comment '1-进行中 0-已结束',
    deleted     tinyint     default 0,
    create_time datetime    default current_timestamp,
    index idx_user (user_id, status)
) engine = innodb default charset = utf8mb4 comment = '习惯';

create table if not exists t_todo_habit_log (
    id          bigint   primary key auto_increment,
    habit_id    bigint   not null,
    user_id     bigint   not null comment '用户Id t_user.u_id',
    log_date    date     not null comment '打卡日期',
    status      tinyint  default 1 comment '1-已打卡',
    create_time datetime default current_timestamp,
    unique key uk_habit_date (habit_id, log_date),
    index idx_user_date (user_id, log_date)
) engine = innodb default charset = utf8mb4 comment = '习惯打卡记录';

-- 番茄专注
create table if not exists t_todo_focus_session (
    session_id       bigint      primary key auto_increment,
    user_id          bigint      not null comment '用户Id t_user.u_id',
    task_id          bigint      default null comment '可关联任务',
    mode             varchar(16) default 'focus' comment 'focus/short/long',
    planned_seconds  int         default 1500 comment '计划秒数',
    actual_seconds   int         default 0 comment '实际专注秒数',
    start_time       datetime    default null,
    end_time         datetime    default null,
    status           varchar(16) default 'running' comment 'running/finished/aborted',
    client_id        varchar(64) default null comment '幂等Id（必须为 NULL 而不是空串，否则唯一键冲突）',
    create_time      datetime    default current_timestamp,
    unique key uk_user_client (user_id, client_id),
    index idx_user_start (user_id, start_time)
) engine = innodb default charset = utf8mb4 comment = '番茄专注记录';

-- 清单模板（模板是全局数据，不区分用户）
create table if not exists t_todo_task_template (
    template_id bigint       primary key auto_increment,
    code        varchar(32)  not null unique comment '模板编码',
    name        varchar(64)  not null comment '模板名，套用后即为新清单名',
    icon        varchar(32)  default '' comment '图标名（对应前端 y-icon 的 name）',
    description varchar(255) default '' comment '模板简介',
    category    varchar(16)  default '' comment '分类（生活/工作/阅读）',
    items       json         default null comment '预置任务数组 [{title,priority}]',
    sort_no     int          default 0,
    status      tinyint      default 1 comment '0-下架 1-上架',
    create_time datetime     default current_timestamp
) engine = innodb default charset = utf8mb4 comment = '清单模板';

-- 模板种子数据（重复执行会按 code 覆盖内容，方便改了文案直接重跑）
insert into t_todo_task_template (code, name, icon, description, category, items, sort_no, status) values
('morning', '晨间计划', 'sun', '喝水、拉伸、写下今天最重要的一件事', '生活',
 '[{"title":"喝一杯温水","priority":1},{"title":"拉伸五分钟","priority":1},{"title":"写下今天最重要的一件事","priority":2},{"title":"想清楚今天必须完成的三件事","priority":1},{"title":"出门前检查随身物品","priority":1}]', 1, 1),
('travel', '旅行准备', 'briefcase', '证件、衣物、充电设备，出发前逐项确认', '生活',
 '[{"title":"确认身份证与车票机票","priority":2},{"title":"换洗衣物按天数清点","priority":1},{"title":"充电器与充电宝","priority":2},{"title":"洗漱用品分装","priority":1},{"title":"常用药品","priority":1},{"title":"雨具与防晒","priority":1},{"title":"关好门窗水电","priority":2},{"title":"把行程同步给家人","priority":1}]', 2, 1),
('review', '周复盘', 'chart', '回看本周完成的事，列出下周三个重点', '工作',
 '[{"title":"回看本周完成的任务","priority":1},{"title":"记录本周最有价值的一件事","priority":1},{"title":"列出下周三个重点","priority":2},{"title":"安排下周的时间块","priority":1}]', 3, 1),
('reading', '读书笔记', 'book', '摘抄、感受、可以马上用起来的一句话', '阅读',
 '[{"title":"摘抄三句打动我的话","priority":1},{"title":"写下读到这里的一点感受","priority":1},{"title":"记下一条可以马上用起来的做法","priority":2}]', 4, 1)
on duplicate key update
    name = values(name),
    icon = values(icon),
    description = values(description),
    category = values(category),
    items = values(items),
    sort_no = values(sort_no),
    status = values(status);

-- 反馈工单（替代客服消息）
create table if not exists t_todo_feedback (
    id          bigint        primary key auto_increment,
    user_id     bigint        not null comment '用户Id t_user.u_id',
    content     varchar(1000) not null,
    images      varchar(1000) default '' comment '图片URL逗号分隔',
    contact     varchar(100)  default '' comment '用户自愿填写的联系方式',
    status      tinyint       default 0 comment '0-待处理 1-已回复',
    reply       varchar(1000) default '',
    create_time datetime      default current_timestamp,
    index idx_user (user_id, create_time)
) engine = innodb default charset = utf8mb4 comment = '用户反馈';

-- 增量同步游标（一用户一设备一游标）
create table if not exists t_todo_sync_cursor (
    user_id        bigint      not null comment '用户Id t_user.u_id',
    device_id      varchar(64) not null,
    last_seq       bigint      default 0 comment '已同步到的服务端序号',
    last_sync_time datetime    default null,
    primary key (user_id, device_id)
) engine = innodb default charset = utf8mb4 comment = '同步游标';

-- 用户维度序号发号器
create table if not exists t_todo_user_seq (
    user_id bigint primary key comment '用户Id t_user.u_id',
    seq     bigint not null default 0
) engine = innodb default charset = utf8mb4 comment = '用户同步序号发号器';

set foreign_key_checks = 1;