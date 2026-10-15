-- ==========================================================================
-- 博客多平台分发模块 建表脚本
-- 依赖：sys_article(文章表)、sys_role(角色表)、sys_menu(菜单表)
-- 说明：本脚本可重复执行前请先确认目标表不存在（脚本末尾提供清理语句，默认注释）
-- ==========================================================================

-- --------------------------------------------------------------------------
-- 1. 分发平台配置表
-- credential_cipher 存放 AES 加密后的凭证密文，禁止明文落库
-- --------------------------------------------------------------------------
create table blog_dispatch_platform (
    id bigint primary key auto_increment comment '主键Id',
    platform_code varchar(50) not null comment '平台编码 juejin/csdn/zhihu/wechat/jianshu/oschina',
    platform_name varchar(50) not null comment '平台名称',
    icon varchar(255) default '' comment '平台图标地址',
    description varchar(255) default '' comment '平台说明',
    enabled tinyint not null default 0 comment '是否启用 0-禁用 1-启用',
    credential_cipher text comment '账号凭证密文(AES加密后的JSON)',
    config_json text comment '发布默认规则(JSON, 非敏感)',
    title_max_length int not null default 100 comment '标题最大长度',
    tag_max_count int not null default 5 comment '标签最大数量',
    summary_max_length int not null default 200 comment '摘要最大长度',
    content_max_length int not null default 100000 comment '正文最大长度',
    rate_limit_per_minute int not null default 30 comment '每分钟最大请求数',
    request_timeout_seconds int not null default 15 comment '单次请求超时(秒)',
    max_retry int not null default 3 comment '最大重试次数',
    sort int not null default 0 comment '排序',
    last_test_time datetime default null comment '最近一次连通性测试时间',
    last_test_result varchar(500) default '' comment '最近一次连通性测试结果',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime default current_timestamp comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
    remark varchar(500) default '' comment '备注',
    unique key uk_platform_code (platform_code),
    index idx_enabled_sort (enabled, sort)
) engine=innodb comment = '分发平台配置表';

-- --------------------------------------------------------------------------
-- 2. 分发平台角色权限表（按角色控制可使用的分发平台）
-- 超级管理员(role_key=admin)不受该表限制
-- --------------------------------------------------------------------------
create table blog_dispatch_platform_role (
    id bigint primary key auto_increment comment '主键Id',
    platform_code varchar(50) not null comment '平台编码',
    role_id bigint not null comment '角色Id',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_platform_role (platform_code, role_id),
    index idx_role_id (role_id)
) engine=innodb comment = '分发平台角色权限表';

-- --------------------------------------------------------------------------
-- 3. 分发任务表
-- --------------------------------------------------------------------------
create table blog_dispatch_task (
    id bigint primary key auto_increment comment '任务Id',
    batch_no varchar(64) default '' comment '批次号(批量分发时同批共用)',
    article_id bigint not null comment '文章Id',
    article_title varchar(255) default '' comment '文章标题快照',
    platform_code varchar(50) not null comment '目标平台编码',
    platform_name varchar(50) default '' comment '目标平台名称快照',
    status tinyint not null default 0 comment '状态 0-待分发 1-分发中 2-分发成功 3-分发失败',
    retry_count int not null default 0 comment '已重试次数',
    max_retry int not null default 3 comment '最大重试次数',
    error_type varchar(32) default '' comment '错误类型编码',
    error_msg varchar(1000) default '' comment '失败原因',
    platform_article_id varchar(128) default '' comment '平台侧文章Id',
    result_url varchar(512) default '' comment '平台侧文章访问链接',
    duration_ms bigint default 0 comment '执行耗时(毫秒)',
    start_time datetime default null comment '开始执行时间',
    finish_time datetime default null comment '执行结束时间',
    operator_id bigint default null comment '触发分发的用户Id',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
    index idx_status_create (status, create_time),
    index idx_article (article_id),
    index idx_platform (platform_code),
    index idx_batch_no (batch_no),
    constraint fk_dispatch_task_article foreign key (article_id) references sys_article(article_id) on delete cascade
) engine=innodb comment = '分发任务表';

-- --------------------------------------------------------------------------
-- 4. 分发文章运营数据表（定时拉取聚合）
-- --------------------------------------------------------------------------
create table blog_dispatch_article_stat (
    id bigint primary key auto_increment comment '主键Id',
    task_id bigint not null comment '分发任务Id',
    article_id bigint not null comment '文章Id',
    platform_code varchar(50) not null comment '平台编码',
    platform_article_id varchar(128) default '' comment '平台侧文章Id',
    result_url varchar(512) default '' comment '平台侧文章访问链接',
    view_count int not null default 0 comment '浏览量',
    like_count int not null default 0 comment '点赞数',
    comment_count int not null default 0 comment '评论数',
    collect_count int not null default 0 comment '收藏数',
    stat_time datetime default null comment '最近一次采集时间',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
    unique key uk_task_id (task_id),
    index idx_platform (platform_code),
    index idx_article (article_id)
) engine=innodb comment = '分发文章运营数据表';

-- ==========================================================================
-- 5. 初始化六个主流平台（默认禁用，需配置凭证后启用）
-- ==========================================================================
insert into blog_dispatch_platform
(platform_code, platform_name, description, enabled, config_json, title_max_length, tag_max_count,
 summary_max_length, rate_limit_per_minute, request_timeout_seconds, max_retry, sort, remark)
values
('juejin', '掘金', '掘金社区，支持 Markdown 正文，需配置 Cookie（真实适配器，创建草稿后自动发布）', 0,
 '{"categoryId":"6809637769959178254","tagIds":["6809640445233070094","6809640703325372423"],"original":true,"publishAsDraft":false}',
 100, 3, 100, 20, 20, 3, 1, '非官方开放接口，要求摘要 50-100 字且至少一个标签'),
('csdn', 'CSDN', 'CSDN 博客，支持 Markdown 正文，需配置 Cookie（真实适配器，自动签名）', 0,
 '{"account":"","categories":"","type":"original","readType":"public","publishAsDraft":true}',
 100, 5, 200, 20, 20, 3, 2, '非官方开放接口，内置签名'),
('zhihu', '知乎', '知乎专栏，正文以 HTML 富文本提交', 0,
 '{"column":"","defaultTags":["技术"],"original":true,"publishAsDraft":true}',
 100, 5, 200, 15, 20, 3, 3, '非官方开放接口'),
('wechat', '微信公众平台', '微信公众号，使用官方草稿箱接口，需配置 AppId/AppSecret 且服务器 IP 在白名单', 0,
 '{"author":"","thumbMediaId":"","defaultTags":[],"original":true,"publishAsDraft":true,"needCover":true}',
 64, 0, 120, 30, 20, 3, 4, '官方开放接口'),
('jianshu', '简书', '简书，支持 Markdown 正文，需配置 Cookie', 0,
 '{"notebook":"","defaultTags":["随笔"],"original":true,"publishAsDraft":true}',
 100, 5, 200, 15, 20, 3, 5, '非官方开放接口'),
('oschina', '开源中国', '开源中国(OSChina)博客，支持 Markdown 正文，需配置 Cookie', 0,
 '{"catalog":"","defaultTags":["开源"],"original":true,"publishAsDraft":true}',
 100, 5, 200, 15, 20, 3, 6, '非官方开放接口');

-- ==========================================================================
-- 6. 菜单与权限
--    挂载在现有「文章管理」(menu_id = 7) 目录下，与文章中心同级
-- ==========================================================================
insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, deleted, remark)
values
(2022, '分发平台配置', 7, 5, 'dispatchPlatform', 'article/dispatch/platform/index', '', '', 1, 0,
 'C', '0', '0', 'blog:dispatch:platform:list', '#', 'admin', sysdate(), '', sysdate(), 0, '多平台分发-平台配置'),
(2023, '分发任务', 7, 6, 'dispatchTask', 'article/dispatch/task/index', '', '', 1, 0,
 'C', '0', '0', 'blog:dispatch:task:list', '#', 'admin', sysdate(), '', sysdate(), 0, '多平台分发-任务列表'),
(2024, '分发数据统计', 7, 7, 'dispatchStat', 'article/dispatch/stat/index', '', '', 1, 0,
 'C', '0', '0', 'blog:dispatch:stat:view', '#', 'admin', sysdate(), '', sysdate(), 0, '多平台分发-运营数据聚合');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, deleted, remark)
values
(2025, '平台新增', 2022, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:platform:add', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2026, '平台编辑', 2022, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:platform:edit', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2027, '平台删除', 2022, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:platform:remove', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2028, '平台连通性测试', 2022, 4, 'test', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:platform:test', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2029, '文章分发', 2023, 1, 'dispatch', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:task:dispatch', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2030, '任务重试', 2023, 2, 'retry', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:task:retry', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2031, '任务删除', 2023, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:task:remove', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2032, '运营数据同步', 2024, 1, 'sync', '', '', '', 1, 0, 'F', '0', '0', 'blog:dispatch:stat:sync', '#', 'admin', sysdate(), '', sysdate(), 0, '');

-- 授予超级管理员角色（role_key = admin），普通角色按需在「角色管理」中勾选
insert into sys_role_menu (role_id, menu_id)
select r.id, m.menu_id
from sys_role r
         join sys_menu m on m.menu_id between 2022 and 2032
where r.role_key = 'admin';

-- ==========================================================================
-- 7. 回滚脚本（默认注释，谨慎执行）
-- ==========================================================================
-- delete from sys_role_menu where menu_id between 2022 and 2032;
-- delete from sys_menu where menu_id between 2022 and 2032;
-- drop table if exists blog_dispatch_article_stat;
-- drop table if exists blog_dispatch_task;
-- drop table if exists blog_dispatch_platform_role;
-- drop table if exists blog_dispatch_platform;
