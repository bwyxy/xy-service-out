# 忆 · 待办 后端设计方案（个人主体版）

- 前端：`y-micro-app/apps/y-todo-mini`（uni-app + Vue3，19 个页面，当前全为本地 mock 数据，尚未联调）
- 后端：复用本仓库 `customer-api/xy-app`（端口 9100，`todo` 库）
- 主体类型：**个人主体**（非个体工商户、非企业）

---

## 0. 结论速览

1. **不新建服务**，把待办的后端能力全部长在 `customer-api/xy-app` 上：它已经有微信登录、`xy_task`/`xy_tag` 完整 CRUD、JWT、`R`/`PageResult` 约定。新建第二个服务对单人维护只有成本。
2. **不引入新中间件**：MySQL（已有）+ Redis（已有）+ Spring `@Scheduled` 足够。提醒用「Redis ZSET 延时队列 or 定时扫描」，不上 MQ。
3. **个人主体的三条硬线决定产品形态**：
   - 不能开通微信支付 → 产品**永久免费**，后端不得出现会员/订单/支付字段，前端不得出现任何"模拟支付"UI（会被判违规）。
   - 不能调用 `getPhoneNumber` → 用户体系**只能靠 openid**，不做手机号绑定。
   - 不能用长期订阅消息 → 提醒**只能走一次性订阅**，必须做「授权额度池」，做不到系统级闹钟。
4. 待办功能落库的关键映射：**清单 = `xy_tag`**、**四象限 = `xy_task.urgency_level`**、**优先级 = 新增 `xy_task.priority`**（两者是不同维度，不可混用）。
5. 必须连同修掉现有代码的 4 个技术债（见 2.2），否则会变成越权 + 重复数据 + 用户建号失败。
6. **用户体系用 `t_user`（客户端用户表，聊天端已在用），不动 `sys_user`**。`sys_user` 是后台管理端（`xy-admin`）的账号表，两者本来就是隔离的两套体系；待办所有用户外键一律指向 `t_user.u_id`，微信登录建号也写 `t_user`。

---

## 1. 约束前提：个人主体小程序的硬边界

### 1.1 能力矩阵

| 能力 | 个人主体 | 对本项目的影响 |
| --- | --- | --- |
| 微信支付 | ❌ 后台无入口，`wx.requestPayment` 直接 fail | 不能收费，不能做任何付费解锁 |
| 手机号快速验证 | ❌ 不支持 | 不做手机号绑定/短信登录 |
| 长期订阅消息 | ❌ 仅政务民生/医疗/交通/金融/教育/设备类目开放 | 提醒只能用一次性订阅 |
| 一次性订阅消息 | ✅ 可用 | 提醒方案的唯一抓手 |
| 直播组件 / 卡券 / 附近的小程序 / 视频号关联 | ❌ | 与本项目无关，不碰 |
| 客服消息 | ⚠️ 基础可用、有限制 | 反馈走自建工单，不依赖客服接口 |
| 云开发 | ✅ 可用 | 备选方案，见 3.2（本期不选） |
| 内容安全接口（`security.msgSecCheck` 等） | ✅ 可用（额度受限） | 昵称/头像机审由服务端代理调用 |
| 流量主广告 | ⚠️ 可开，需 UV 门槛 | 唯一的合规变现路径（后端无需改造） |
| 服务类目 | 仅工具/教育/生活服务/文娱等 | 提审选 **工具 > 效率** |
| ICP 备案 | 必须，个人可备案（上限 5 个） | 上线前置条件 |

### 1.2 六条红线 → 设计对策

| 红线 | 对策 |
| --- | --- |
| 无支付 | 所有「高级功能」不做付费墙；不做"敬请期待"按钮（会被判绕过支付） |
| 无手机号 | `t_user` 只落 openid；联系方式由用户在资料页自愿填写（不校验、不做账号找回） |
| 无长期订阅 | 一次性订阅 + 额度池（第 6 节） |
| 禁社交/社区类目 | **不做**公开排行、好友互看、任务评论、共享清单邀请；`xy_task_collaborator` 表保留但不启用 |
| 必须备案 | 域名备案 + HTTPS + 小程序后台配置「request 合法域名」（**不支持 IP、不支持端口**） |
| 隐私合规 | 后台配置《用户隐私保护指引》，声明收集 openid、昵称、头像、待办内容；前端配合 `wx.getPrivacySetting` |

### 1.3 为什么值得在文档里写死这些

主体类型注册后不可变更；一旦做了支付/社交入口，轻则审核驳回，重则封禁，且**换主体要换 appid**——appid 变了 openid 全部失效，用户数据要重新映射，等于重开一局。所以这份设计的核心不是"能做什么"，而是**先把不能做的排除掉**。

---

## 2. 现状盘点（复用基础）

### 2.1 已有后端能力

| 能力 | 位置 | 复用结论 |
| --- | --- | --- |
| 微信登录 | `xy-app/.../controller/WeChatLoginController.java`（`POST /wechat/login`、`GET /wechat/getInfo`、`POST /wechat/logout`） | **改造后复用**：接口不动，内部用户表由 `sys_user` 换为 `t_user`（见 2.2 第 3 条） |
| 客户端用户表 | `t_user`（`sql/chat.sql`，PK `u_id`）、实体 `xy-common/.../entity/User.java`（`@TableName("t_user")`）、`IChatUserService`、后台 `xy-admin/.../ChatUserController.java` | **复用为主**：待办的用户主表 |
| 后台账号表 | `sys_user`（`sql/todo.sql` 里顺带建的） | **待办不使用**，仅 `xy-admin` 后台管理 |
| JWT 签发/校验/登出黑名单 | `xy-common/.../provider/JwtTokenProvider.java`、`xy-framework/.../security/filter/JwtAuthenticationFilter.java`、`TokenBlacklistService` | **直接用** |
| 待办 CRUD | `xy-app/.../controller/TodoTaskController.java` + `ITodoTaskService` + `TodoTaskServiceImpl` | **改造后复用**（见 4.1） |
| 标签（→ 清单）CRUD | `xy-app/.../controller/TodoTagController.java` | **改造后复用** |
| 统一响应/分页/异常/状态码 | `R<T>`、`PageResult<T>`、`ResultCode`、`GlobalExceptionHandler` | **遵循**：所有新接口返回 `R` |
| 匿名放行 / 限流 | `@Anonymous`、`RateLimitInterceptor`（bucket4j + Redis） | **遵循** |
| 表结构 | `sql/todo.sql`：`xy_task`、`xy_tag`、`xy_task_tag`、`xy_task_collaborator`（同文件里的 `sys_user` 属后台账号，待办不用） | **扩展**：用户外键改指 `t_user.u_id` |
| 环境变量 | `.env`：`DB_URL=jdbc:mysql://…:13306/todo`、`WECHAT_APPID=…` | **沿用机制** |
| 版本/公告 | `sql/app-version.sql` | 「关于忆」的版本信息复用 |

### 2.2 必须先修的 4 个技术债

1. **越权风险**：`TodoTaskController` 的入参是 `@RequestBody TodoTask`，`user_id` 由前端传入。必须改成**一律从 token 解析 userId**，忽略请求体里的 `user_id`；查询/更新/删除都要带 `user_id` 条件（`update ... where task_id=? and user_id=?`）。
2. **无幂等**：小程序弱网重试会造成重复任务。`xy_task` 增加 `client_id`，建唯一索引 `uk_user_client(user_id, client_id)`，创建走 `INSERT ... ON DUPLICATE KEY UPDATE`。
3. **待办用户体系没落在 `t_user` 上**：`WeChatLoginServiceImpl` 现在把 openid 当 `username` 写进 `sys_user`（后台账号表），必须改为查/建 `t_user`：
   - 复用已有实体 `com.xy.common.entity.User`（`@TableName("t_user")`），在 `UserMapper` 新增 `selectByOpenId` / `insertWeChatUser`；不要为待办另造一张用户表；
   - JWT 主体改用 `u_id`，并沿用 fg-chat 的客户端前缀约定（`ChatUserDetailsService` 用 `chat_` 前缀区分「客户端用户」与「后台账号」），避免两种 token 混用；
   - `xy_user_profile` / `xy_user_setting` 等扩展表的主键随之为 `u_id`（见 4.2）。

4. **`t_user` 的建表约束会挡住微信用户**（不处理的话第二个用户就插不进去，这是最容易被忽略的一处）：
   - `unique index uk_phone (phone)` 且 `phone default ''` → 两条空串互相冲突，第二个微信用户的 insert 直接失败。建号时 `phone` / `email` **必须写 NULL**（MySQL 唯一索引视多个 NULL 互不相同），或把两列改成 `default null`；
   - `password varchar(500) not null` 且无默认值 → 微信用户没有密码，建号时必须显式写空串（不能省略该列）；
   - `nickname varchar(50) not null` → 建号必须给默认昵称，如「微信用户 + openid 脱敏」（`StringUtils.maskOpenId` 已有）；
   - 缺 openid 唯一键 → 建议 `alter table t_user add unique key uk_wx_open_id (wx_open_id);`（存量空串行会冲突，需先把空串归一为 NULL），否则并发首登会建出两个账号；
   - 纯待办用户 `is_bind_chat` 应写 `'0'`（未绑定聊天应用）。

### 2.3 前端 19 页 → 后端缺口

| 页面 | 现状 | 缺口 |
| --- | --- | --- |
| 今日 / 清单 / 新建任务 | 有 `/task/*`、`/tag/*` 雏形 | 幂等、清单字段、提醒/重复/优先级字段 |
| 工具-番茄专注 | 无 | 番茄记录表 + 今日统计 |
| 工具-日历视图 | 无 | 按月份区间查任务 |
| 工具-四象限 | `urgency_level` 已有 | 按紧急度分组查询 |
| 工具-习惯打卡 | 无 | 习惯表 + 打卡记录表 |
| 工具-待办统计 | 无 | 周聚合 + 清单分布聚合接口 |
| 工具-清单模板 | 无 | 模板表 + 套用接口 |
| 我的-个人资料 | `t_user` 有昵称/头像/性别 | 签名、生日、所在地（**不含手机号**） |
| 我的-提醒设置 | 无 | 用户偏好表 |
| 我的-外观与显示 | 无 | 本地为主，云端冗余备份 |
| 我的-数据同步与备份 | 无 | 增量同步 + 导出 |
| 我的-已完成与归档 | 无 | 归档字段 + 批量操作 |
| 我的-帮助与反馈 | 无 | FAQ（配置化）+ 反馈工单表 |
| 我的-关于忆 | 有 `app-version` | 复用 |
| 搜索 | 无 | 关键词检索 |

---

## 3. 总体架构

### 3.1 架构

```
微信小程序 y-todo-mini
   │  HTTPS（备案域名，无端口）
   ▼
宝塔 Nginx（TLS 终止 + 反向代理）
   ▼
customer-api/xy-app :9100   ← 唯一对外服务（BFF 兼业务）
   ├─ JwtAuthenticationFilter / RateLimitInterceptor
   ├─ controller ─ task | list | habit | focus | stats | archive | template | user | notify | sync | search | feedback
   ├─ service   ─ 事务边界、幂等、聚合
   ├─ mapper    ─ MyBatis-Plus
   ├─ scheduler ─ @Scheduled 提醒扫描（单实例）
   ├─ MySQL 8   ─ 库 todo（复用现有连接）
   ├─ Redis     ─ token 黑名单 / 限流 / 提醒延迟队列 / 分布式锁
   └─ 微信开放接口 ─ jscode2session | subscribeMessage.send | msgSecCheck | imgSecCheck
```

不动的：`wukongim`（IM）、`deploy/livekit`（RTC）、`xy-ai` —— 待办完全不需要它们，保持隔离，别顺手引依赖。

### 3.2 选型取舍

| 方案 | 结论 | 理由 |
| --- | --- | --- |
| **复用 `xy-app` 单体** | ✅ 采用 | 登录/待办/标签/约定都在此；单人维护，一个进程、一份日志、一次发布 |
| 微信云开发 | ❌ 本期不选 | 数据落在微信侧，与现有 Java 体系、后台管理端（`xy-admin` 待办菜单）、`todo` 库割裂；将来换端/换主体迁移成本高 |
| 新建独立 todo 服务 | ❌ 不选 | 要重复做登录、鉴权、R 包装、部署、域名；收益是"干净"，成本是双倍运维 |

### 3.3 包结构（新增部分）

```
customer-api/xy-app/src/main/java/com/xy/app/
├─ controller/
│  ├─ TodoTaskController.java      （改造）
│  ├─ TodoTagController.java       （改造：清单）
│  ├─ TodoHabitController.java     （新增）
│  ├─ TodoFocusController.java     （新增）
│  ├─ TodoStatsController.java     （新增）
│  ├─ TodoTemplateController.java  （新增）
│  ├─ TodoArchiveController.java   （新增）
│  ├─ UserProfileController.java   （新增）
│  ├─ UserSettingController.java   （新增）
│  ├─ NotifyController.java        （新增）
│  ├─ SyncController.java          （新增）
│  └─ FeedbackController.java      （新增）
├─ mapper/UserMapper.java          （新增 selectByOpenId / insertWeChatUser，指向 t_user）
├─ service/ + service/impl/        （同上对应）
├─ scheduler/TodoRemindScheduler.java
└─ client/WeChatApiClient.java      （access_token 缓存、send、msgSecCheck）

xy-common/src/main/java/com/xy/common/entity/  ← 新实体统一放这里（沿用现有 TodoTask 的位置）
customer-api/xy-app/src/main/resources/mapper/  ← 新增 XML（复杂聚合查询用）
```

---

## 4. 数据模型

### 4.1 复用与改造

**`xy_task`（改造）** —— 已有 `task_id/user_id/task_title/description/completed/urgency_level/due_date/deleted` ：

```sql
alter table xy_task
    add column tag_id        bigint       default null comment '清单Id(→xy_tag.tag_id)',
    add column priority      tinyint      default 0    comment '优先级 0-无 1-普通 2-重要（列表页小旗子）',
    add column repeat_rule   varchar(64)  default ''   comment '重复规则 如 DAILY/WEEKLY:1,3/FREQ=WEEKLY;BYDAY=MO',
    add column remind_before int          default 0    comment '提前提醒分钟数，0-不提醒',
    add column remind_at     datetime     default null comment '下次提醒时间（服务端计算，便于索引扫描）',
    add column completed_time datetime    default null comment '完成时间（统计用）',
    add column archived      tinyint      default 0    comment '是否归档 0-否 1-是',
    add column client_id     varchar(64)  default ''   comment '客户端幂等Id',
    add column seq           bigint       default 0    comment '用户维度同步序号',
    add index idx_user_remind (remind_at, deleted),
    add index idx_user_tag (user_id, tag_id),
    add index idx_user_seq (user_id, seq),
    add unique key uk_user_client (user_id, client_id);

create index idx_user_completed_time on xy_task(user_id, completed_time);
create index idx_user_archived on xy_task(user_id, archived, completed);
```

> `urgency_level`（1~4）**继续用于四象限页**，`priority`（列表页「重要/普通」小旗）是另一个维度，两者并存不要互相赋值。

**`xy_tag`（改造）** —— 即前端「清单」：

```sql
alter table xy_tag
    add column icon      varchar(32) default '' comment '图标名（对应前端 y-icon 的 name）',
    add column color     varchar(16) default '' comment '主题色',
    add column sort_no   int         default 0  comment '排序',
    add column is_default tinyint    default 0  comment '是否默认清单',
    add column archived  tinyint     default 0  comment '是否归档';
```

> `sql/todo.sql` 里内置的 6 条 tag 种子（工作/学习/生活/旅行/健康/购物）属后台演示数据，线上要给**每个新用户初始化他自己的 4 个清单**（工作/生活/阅读/个人成长，与前端 `pages/list` 一致），初始化放在首次登录成功后，且必须幂等（`is_default=1` 存在即跳过）。

**`xy_task_tag`** —— 保留表结构、本期不启用（前端目前是"单选清单"）。将来做多标签时再启用，无需改表。

**`xy_task_collaborator`** —— 保留表结构、本期不启用（多人协作有社交类目风险）。

**外键迁移（`sys_user` → `t_user`）** —— `sql/todo.sql` 里 `xy_task.user_id`、`xy_task_collaborator.user_id`、`xy_tag.user_id` 的外键都指向 `sys_user(id)`，必须改指 `t_user(u_id)`：

```sql
-- 约束名先用 show create table 确认（内联 foreign key 会自动命名，如 xy_tag_ibfk_1）
alter table xy_task drop foreign key fk_sys_user;
alter table xy_task add constraint fk_task_user foreign key (user_id) references t_user(u_id) on delete cascade;

alter table xy_tag drop foreign key <实际约束名>;
alter table xy_tag add constraint fk_tag_user foreign key (user_id) references t_user(u_id) on delete cascade;

alter table xy_task_collaborator drop foreign key <实际约束名>;
alter table xy_task_collaborator add constraint fk_collab_user foreign key (user_id) references t_user(u_id) on delete cascade;
```

> 迁移顺序：先 `t_user` 建号、再把历史 `xy_task.user_id` 映射到 `u_id`、最后重建外键；直接改外键会因存量 `user_id` 在 `t_user` 中不存在而失败。

### 4.2 新增表

> **以下所有表的 `user_id` 一律指向 `t_user.u_id`**（客户端用户），不再指向 `sys_user.id`；为与已有的 `xy_task.user_id` 保持一致，列名统一保留 `user_id`（不改名 `u_id`），但语义是「`t_user` 主键」。

```sql
-- 用户偏好（提醒设置 / 外观与显示 / 同步）
create table xy_user_setting (
    user_id           bigint primary key comment '用户Id',
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
    last_sync_time    datetime    default null comment '上次同步时间（同步页状态卡）',
    create_time datetime default current_timestamp,
    update_time datetime default current_timestamp on update current_timestamp
) engine=innodb default charset=utf8mb4 comment='用户偏好设置';

-- 用户资料扩展（不往 t_user 上加列，避免污染客户端用户主表）
create table xy_user_profile (
    user_id   bigint primary key comment '用户Id',
    signature varchar(100) default '' comment '个性签名',
    birthday  varchar(10)  default '' comment '生日 yyyy-MM-dd',
    city      varchar(64)  default '' comment '所在地',
    update_time datetime default current_timestamp on update current_timestamp
) engine=innodb default charset=utf8mb4 comment='用户资料扩展';

-- 提醒（一个任务可有多个提醒点）
create table xy_task_reminder (
    reminder_id bigint primary key auto_increment,
    task_id     bigint   not null comment '任务Id',
    user_id     bigint   not null comment '用户Id',
    remind_at   datetime not null comment '提醒时间',
    template_id varchar(64) default '' comment '订阅消息模板Id',
    status      varchar(16) default 'pending' comment 'pending/sending/sent/failed/canceled',
    retry_count int default 0 comment '已重试次数',
    send_time   datetime default null comment '实际下发时间',
    fail_reason varchar(255) default '' comment '失败原因（errcode+errmsg）',
    create_time datetime default current_timestamp,
    update_time datetime default current_timestamp on update current_timestamp,
    index idx_status_remind (status, remind_at),
    index idx_task (task_id),
    index idx_user (user_id, remind_at)
) engine=innodb default charset=utf8mb4 comment='任务提醒';

-- 一次性订阅授权额度池（核心：一次授权=一条消息）
create table xy_subscribe_quota (
    user_id     bigint      not null comment '用户Id',
    template_id varchar(64) not null comment '模板Id',
    remain      int         not null default 0 comment '剩余可下发条数',
    total       int         not null default 0 comment '累计获得授权次数',
    update_time datetime default current_timestamp on update current_timestamp,
    primary key (user_id, template_id)
) engine=innodb default charset=utf8mb4 comment='订阅消息授权额度';

-- 下发日志（排查"用户说没收到提醒"的唯一凭据）
create table xy_notify_log (
    id          bigint primary key auto_increment,
    user_id     bigint not null,
    task_id     bigint default null,
    reminder_id bigint default null,
    template_id varchar(64) default '',
    page        varchar(128) default '' comment '点击跳转页',
    errcode     int default 0,
    errmsg      varchar(255) default '',
    send_time   datetime default current_timestamp,
    index idx_user_time (user_id, send_time)
) engine=innodb default charset=utf8mb4 comment='订阅消息下发日志';

-- 习惯 / 打卡
create table xy_habit (
    habit_id  bigint primary key auto_increment,
    user_id   bigint not null,
    name      varchar(64) not null comment '习惯名称',
    icon      varchar(32) default '' comment '图标名',
    color     varchar(16) default '' comment '主题色',
    freq_type varchar(16) default 'daily' comment 'daily/weekly',
    freq_days varchar(32) default '' comment 'weekly 时的星期集合 如 1,3,5',
    target_days int default 0 comment '目标天数（0 不限）',
    sort_no   int default 0,
    status    tinyint default 1 comment '1-进行中 0-已结束',
    create_time datetime default current_timestamp,
    deleted   tinyint default 0,
    index idx_user (user_id, status)
) engine=innodb default charset=utf8mb4 comment='习惯';

create table xy_habit_log (
    id       bigint primary key auto_increment,
    habit_id bigint not null,
    user_id  bigint not null,
    log_date date   not null comment '打卡日期',
    status   tinyint default 1 comment '1-已打卡',
    create_time datetime default current_timestamp,
    unique key uk_habit_date (habit_id, log_date),
    index idx_user_date (user_id, log_date)
) engine=innodb default charset=utf8mb4 comment='习惯打卡记录';

-- 番茄专注
create table xy_focus_session (
    session_id bigint primary key auto_increment,
    user_id    bigint not null,
    task_id    bigint default null comment '可关联任务',
    mode       varchar(16) default 'focus' comment 'focus/short/long',
    planned_seconds  int default 1500 comment '计划秒数',
    actual_seconds   int default 0    comment '实际专注秒数',
    start_time datetime default null,
    end_time   datetime default null,
    status     varchar(16) default 'running' comment 'running/finished/aborted',
    client_id  varchar(64) default '' comment '幂等Id',
    create_time datetime default current_timestamp,
    unique key uk_user_client (user_id, client_id),
    index idx_user_start (user_id, start_time)
) engine=innodb default charset=utf8mb4 comment='番茄专注记录';

-- 清单模板
create table xy_task_template (
    template_id bigint primary key auto_increment,
    code      varchar(32) not null unique comment '模板编码',
    name      varchar(64) not null,
    icon      varchar(32) default '',
    description varchar(255) default '',
    items     json default null comment '预置任务数组 [{title,priority}]',
    sort_no   int default 0,
    status    tinyint default 1,
    create_time datetime default current_timestamp
) engine=innodb default charset=utf8mb4 comment='清单模板';

-- 反馈工单（替代客服消息）
create table xy_feedback (
    id        bigint primary key auto_increment,
    user_id   bigint not null,
    content   varchar(1000) not null,
    images    varchar(1000) default '' comment '图片URL逗号分隔',
    contact   varchar(100) default '' comment '用户自愿填写的联系方式',
    status    tinyint default 0 comment '0-待处理 1-已回复',
    reply     varchar(1000) default '',
    create_time datetime default current_timestamp,
    index idx_user (user_id, create_time)
) engine=innodb default charset=utf8mb4 comment='用户反馈';

-- 增量同步游标（一用户一设备一游标）
create table xy_sync_cursor (
    user_id   bigint not null,
    device_id varchar(64) not null,
    last_seq  bigint default 0 comment '已同步到的服务端序号',
    last_sync_time datetime default null,
    primary key (user_id, device_id)
) engine=innodb default charset=utf8mb4 comment='同步游标';

-- 用户维度序号发号器
create table xy_user_seq (
    user_id bigint primary key,
    seq     bigint not null default 0
) engine=innodb default charset=utf8mb4 comment='用户同步序号发号器';
```

### 4.3 前端概念 → 字段映射

| 前端 | 后端 |
| --- | --- |
| 用户身份 | `t_user.u_id`（JWT 主体），openid 只用于建号与查询 |
| 清单分类（工作/生活/阅读/个人成长） | `xy_tag`，首次登录初始化 4 条 |
| 任务所属清单（新建页单选） | `xy_task.tag_id` |
| 重要/普通小旗 | `xy_task.priority` |
| 四象限 | `xy_task.urgency_level`（1~4） |
| 到期时间 | `xy_task.due_date` |
| 提醒 | `xy_task.remind_before` + `xy_task_reminder` |
| 重复 | `xy_task.repeat_rule` |
| 已完成 / 归档 | `completed` + `completed_time` + `archived` |
| 备注 | `description` |
| 打卡格子 | `xy_habit_log` |
| 本周柱状图 / 清单分布 | 聚合 `xy_task`（实时 SQL，不建汇总表，量小没必要） |

---

## 5. 接口设计

### 5.1 统一约定

- 统一返回 `R<T>`（`code/msg/data/timestamp`），分页返回 `PageResult<T>`；异常走 `GlobalExceptionHandler`。
- 除登录外全部需要 `Authorization` 头；`userId` **只从 token 取**，请求体里的 `user_id` 一律忽略。
- 写接口全部支持幂等：请求体带 `clientId`（前端 uuid）。
- 时间：**统一 `Asia/Shanghai`**，DB 存 `datetime`（与既有字段一致）。容器必须设 `TZ=Asia/Shanghai`，JVM 加 `-Duser.timezone=Asia/Shanghai`，否则 `@Scheduled` 和 `remind_at` 比对会整体偏移。
- 路径风格：延续现有 `/task`、`/tag` 的短路径，不加 `/api/v1` 前缀（与仓库既有习惯一致）。

### 5.2 接口清单

**任务**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/task/day` | 今日任务（已有，补权限过滤） |
| POST | `/task/list` | 列表（清单/状态/是否归档/时间区间/排序） |
| POST | `/task/month` | 日历视图：按月取有任务的日期 + 当日条数 |
| POST | `/task/quadrant` | 四象限：按 `urgency_level` 分组返回 |
| GET | `/task/{id}` | 详情 |
| POST | `/task/create` | 创建（`clientId` 幂等） |
| POST | `/task/update` | 更新 |
| POST | `/task/completed` | 完成/取消完成（服务端写 `completed_time`） |
| POST | `/task/archive` | 归档 / 恢复（批量，`taskIds`） |
| POST | `/task/clear-completed` | 清空已完成（物理删或标记，见 7 节墓碑） |
| DELETE | `/task/{id}` | 删除（逻辑删除 + 写 `seq` 墓碑） |
| GET | `/task/search` | 关键词 + 范围（全部/清单/已完成） |

**清单 / 模板 / 统计**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/tag/list` | 清单列表（含各清单待办数） |
| POST/PUT/DELETE | `/tag[/{id}]` | 清单增改删 |
| GET | `/template/list` | 模板列表 |
| POST | `/template/{code}/apply` | 套用模板（批量建任务，`clientId` 前缀去重） |
| GET | `/stats/week` | 本周完成柱状图 |
| GET | `/stats/distribution` | 清单分布 |

**习惯 / 番茄**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/habit/list` | 习惯 + 本周打卡状态 + 本周完成率 |
| POST/PUT/DELETE | `/habit[/{id}]` | 习惯增改删 |
| POST | `/habit/{id}/check` | 打卡/取消（`log_date` 唯一键幂等） |
| GET | `/focus/today` | 今日番茄数、累计分钟 |
| POST | `/focus/session/start` | 开始（`clientId` 幂等） |
| POST | `/focus/session/finish` | 结束（上报 `actualSeconds`、`status`） |

**用户 / 设置 / 同步 / 反馈**

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET / PUT | `/user/profile` | 资料：昵称/头像/性别落 `t_user`，签名/生日/所在地落 `xy_user_profile`（主键 `u_id`） |
| POST | `/user/avatar` | 头像上传（`wx.uploadFile` → 对象存储，返回 URL） |
| GET / PUT | `/user/setting` | 偏好设置（第 4.2 节字段） |
| POST | `/sync/pull` | 增量拉取（入参 `sinceSeq`） |
| POST | `/sync/push` | 批量提交本地变更（数组，逐条幂等） |
| POST | `/sync/export` | 导出全量 JSON（"备份"） |
| GET | `/notify/quota` | 各模板剩余授权额度 |
| POST | `/notify/subscribe` | 上报前端 `requestSubscribeMessage` 结果，累加额度 |
| DELETE | `/notify/reminder/{id}` | 取消单条提醒 |
| POST | `/feedback` | 提交反馈 |

### 5.3 三个关键接口细则

**`POST /task/create`**

```json
// req
{ "clientId":"3f2a…", "title":"整理产品改版思路", "tagId":11, "priority":2,
  "urgencyLevel":4, "dueDate":"2026-10-23 10:00:00", "repeatRule":"WEEKLY:1,3",
  "remindBefore":15, "description":"…" }
// resp: R<{ taskId: 1001, remindAt: "2026-10-23 09:45:00", seq: 88 }>
```

服务端行为：写 `xy_task`（含 `seq` 发号）→ 若 `remindBefore>0 && dueDate` 则插一条 `xy_task_reminder` → 计算并回写 `remind_at` → 回传 `seq` 供前端更新本地水位。

**`POST /sync/pull`**

```json
// req:  { "sinceSeq": 87, "deviceId": "dev_xxx" }
// resp: { "serverSeq": 91, "tasks": [ …含 deleted 墓碑 ], "tags": […], "settings": {…} }
```

**`POST /notify/subscribe`**

```json
// req:  { "templateId":"TPL_TASK_REMIND", "count": 2 }   // 前端拿到的实际 accept 次数
// resp: { "remain": 3, "total": 5 }
```

---

## 6. 提醒（订阅消息）闭环设计 ★

这是整个后端**最容易做错、也最影响口碑**的一块，因为它受个人主体限制最狠。

### 6.1 为什么必须做「额度池」

一次性订阅的语义是：**用户授权一次 → 开发者可下发一条**。所以：

- 不能像企业长期订阅那样"设了时间就一定会响"；
- 必须精确记账：用户授权 N 次就最多能发 N 条，多发的调用会直接返回错误；
- 授权是**累积**的（用户可多次点击订阅，后台显示累计订阅次数），所以额度池天然支持"多授权一次，多一条保障"。

交互落点：新建任务选择"15 分钟前提醒"时 → 前端弹订阅授权 → 把结果上报 `/notify/subscribe` → 额度 +1。用户关了提醒设置页时也可引导"再授权 3 次，未来 3 条提醒不遗漏"。

### 6.2 状态机与定时任务

```
pending ──(扫描命中 & remain>0 & 不在免打扰)──► sending ──► sent   (remain-1, 写 xy_notify_log)
   │                                             └──失败──► failed (retry_count<3 则退回 pending；否则终态)
   ├──(超出有效期/任务已完成/被用户取消)──► canceled
   └──(remain==0)──► pending 保留，进小程序时提示"提醒额度用完，请再次授权"
```

`TodoRemindScheduler`（`@Scheduled(fixedDelay = 30_000)`，单实例安全）：

1. 取 `status='pending' and remind_at <= now()` 的批次（`limit 200`，加 `idx_status_remind`）；
2. 检查开关（`xy_user_setting.task_remind_on`）、免打扰时段、任务是否已完成/已删；
3. 检查额度，`remain<=0` → 跳过并标记待提示；
4. 调用 `subscribeMessage.send`（`page` 指向任务详情/今日页）；
5. 按结果更新状态、扣额度、写 `xy_notify_log`；
6. 多次实例部署时用 Redis `setIfAbsent(job:remind, 30s)` 加锁（当前单实例不需要）。

`access_token` 必须集中缓存（Redis，`expires_in-200s` 过期），**严禁**每次发送都取；并发取 token 会互相顶掉（微信是后取的有效），这是经典事故点。

### 6.3 降级方案（必须做）

因为额度会耗尽、用户可能拒授权、免打扰时段不能发，所以**不能只依赖订阅消息**：

- 小程序内「今日页」红点/角标：进入即显示"已到点未完成"的任务（纯前端用本地 `dueDate` 判断，不消耗额度）；
- 首页顶部提示条："你还有 3 条提醒额度，用完需再次授权"；
- 若未来必须做"到点必响"，唯一合规路径是升级为个体工商户/企业主体（届时 `openid` 会变，用户需重新授权登录，需提前准备迁移映射方案）。

---

## 7. 同步与备份

**方案：服务端权威 + 用户维度单调 `seq` + 墓碑删除。**

- 每次写操作：`update xy_user_seq set seq = seq + 1`（`SELECT … FOR UPDATE` 或 `insert … on duplicate key update seq = seq + 1` 后取值），写入 `xy_task.seq`；
- `pull`：返回 `seq > sinceSeq` 的全部变更，**包含 `deleted=1` 的墓碑行**，前端据此删除本地记录；
- `push`：逐条按 `clientId` 幂等落库，返回每条的最新 `seq`；
- 删除一律逻辑删除，`clear-completed` 也是标记 + 墓碑，不做物理删（否则其他设备拉不到删除动作，会出现"删了又回来"）；
- 冲突策略：字段级不做合并，**最后写入胜**（单人自用场景足够）；
- "立即同步"：`push` 待发队列 → `pull` 增量 → 更新 `xy_sync_cursor` 与 `xy_user_setting.last_sync_time`（同步页状态卡）；
- "导出备份"：`/sync/export` 返回全量 JSON，前端 `wx.getFileSystemManager().writeFile` 落盘到用户可保存目录。

> 这与仓库里 IM 消息去重的教训同源：**只靠自增 id 判重会失效**，必须有稳定的客户端幂等键（`clientId`）+ 服务端单调序号（`seq`）。

---

## 8. 安全与合规

| 项 | 做法 |
| --- | --- |
| 鉴权 | 沿用 JWT；`u_id` 从 token 解析，禁止前端传；白名单只放 `/wechat/login`、`/actuator/health` |
| 越权 | 所有单条操作 SQL 带 `user_id = 当前 u_id` 条件；`/task/{id}` 校验归属；待办 token 与后台 `sys_user` token 不互通 |
| 限流 | 复用 `RateLimitInterceptor`；额外对 `/notify/subscribe`、`/feedback` 单独限流防刷 |
| 敏感信息 | `AppSecret` 只在服务端；日志中 openid 用 `StringUtils.maskOpenId` 脱敏 |
| 内容安全 | 昵称走 `security.msgSecCheck`、头像走 `security.imgSecCheck/mediaCheckAsync`，由 `WeChatApiClient` 代理调用；**前端不得直连微信接口** |
| 隐私合规 | 后台配置《用户隐私保护指引》声明收集范围；前端配合 `wx.getPrivacySetting` / `onNeedPrivacyAuthorization` |
| 数据最小化 | 不采集手机号、不采集通讯录、不做位置采集（"所在地"是用户手填文本） |
| 备案 | 域名 ICP 备案 + HTTPS；小程序备案（个人上限 5 个） |
| 红线自查 | 无支付/会员/虚拟币字段；无公开排行、无 UGC 公开展示；无"敬请期待"按钮；无引导加群/关注 |

---

## 9. 部署与运维

- **进程**：`xy-app` 打包 fat jar，按现有方式 `java -jar`（或 docker）。JVM 与容器时区固定 `Asia/Shanghai`。
- **Nginx**：宝塔反代 `api.<域名>` → `127.0.0.1:9100`，强制 HTTPS，开启 gzip；沿用仓库 LiveKit 那套"复用现有 Nginx、不启新 443 入口"的经验，不要为待办再起一个 Caddy/新 443 监听。
- **小程序域名**：`request` 合法域名必须是 **HTTPS + 已备案域名 + 不带端口**，不能用 IP。开发期用"不校验合法域名"开关。
- **MySQL**：库 `todo` 已有；新表 DDL 落到 `sql/` 下新增文件（如 `sql/todo-v2.sql`），与现有 `todo.sql` 分开，便于对照升级。
- **备份**：每日 `mysqldump` 全量 + 保留 7 天；Redis 仅缓存不备份（丢了只是重登）。
- **监控**：放行 `/actuator/health`；重点盯两个指标——`subscribeMessage.send` 失败率、`/task/create` 幂等命中率。
- **成本**：现有服务器 + 现有 MySQL/Redis 即可承载（个人应用量级）；头像存储用对象存储（个人实名可开通），不走服务器本地盘（避免换机器丢图）。

---

## 10. 实施阶段

| 阶段 | 内容 | 完成标志 |
| --- | --- | --- |
| **P0 打通主链路** | 修 3 个技术债（2.2）；`xy_task`/`xy_tag` 改造；`/task/create|list|day|update|completed|delete`；清单初始化 | 今日页/清单页/新建页三页真机跑通，弱网重试不产生重复任务 |
| **P1 数据完整** | 归档/清空已完成/搜索；统计两周聚合；用户资料 + 头像上传 + 用户设置 | 「我的」7 页除同步外全部联通 |
| **P2 提醒闭环** | `xy_task_reminder` + 额度池 + `access_token` 缓存 + 定时任务 + 下发日志；习惯、番茄记录 | 真机收到一次订阅消息提醒；额度耗尽有明确提示 |
| **P3 同步与提审** | `seq` 增量同步 + 导出备份；反馈工单；模板按需入库；备案 + 隐私指引 + 类目「工具>效率」提审 | 换设备能恢复数据；提审通过 |

---

## 11. 风险与替代方案

| 风险 | 影响 | 替代/应对 |
| --- | --- | --- |
| 一次性订阅额度耗尽 | 提醒漏发 | 站内提示 + 引导再授权；首页红点兜底；**不承诺"必响"** |
| 无法获取手机号 | 无法做账号找回/短信验证 | 账号即微信身份，不做账号密码；跨端身份留待将来 |
| 不能支付 | 无收入 | 唯一合规变现是流量主广告（需 UV 门槛）；若将来收费须升级个体工商户 → 需重新注册小程序、appid 变化 → openid 全变，**必须先做用户映射迁移预案** |
| 禁社交类目 | 多人共享清单不可做 | 保留 `xy_task_collaborator` 表但不启用 |
| 无 unionid（个人难开开放平台） | 与公众号/App 无法打通身份 | 未来若做 App，需自建账号体系并做"微信绑定"，现在就把 `t_user.u_id` 作为稳定主键对外暴露（**不要对外暴露 openid**） |
| 订阅消息模板审核 | 模板被拒 | 优先用公共模板库通用标题（如"待办提醒"），关键词在已有模板下申请新增，别自造模板 |
| 备案周期 | 上线时间不可控 | 开发期同步提备案，别等代码完成才提交 |

---

## 12. 明确不做（本期排除清单）

- ❌ 微信支付、会员、订阅、内购、任何"解锁高级功能"入口
- ❌ 手机号获取与短信验证
- ❌ 长期订阅消息
- ❌ 多人协作/共享清单/任务评论/公开排行
- ❌ 引入 MQ、ES、新数据库、新微服务模块
- ❌ 自建用户密码体系（个人主体场景没有收益，只增加被撞库风险）

---

## 附：前端需配合的 5 处改造

1. 统一 `request` 封装：注入 `Authorization`，统一解 `R.code`，401 自动重新 `wx.login` 换 token。
2. 所有写操作带 `clientId`（uuid），重试沿用同一个 `clientId`（这是幂等的前提）。
3. 本地缓存 + 水位：`storage` 存 `lastSeq` 与任务快照，进页面先渲染本地再拉增量。
4. 订阅消息授权封装：`requestSubscribeMessage` 结果必须上报 `/notify/subscribe`，否则服务端额度为 0、提醒永远发不出。
5. 隐私弹窗：接入 `wx.getPrivacySetting`，并在「个人资料」页用「头像昵称填写能力」替代已失效的 `getUserProfile`。
