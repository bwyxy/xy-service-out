-- ==========================================================================
-- 应用版本发布与客户端检查更新
-- ==========================================================================
-- 应用版本表：按「应用 + 平台」维度发布版本，客户端上报版本码后比对是否为最新
-- 版本大小比较只用 version_code（数字），version_name 仅用于展示
create table if not exists sys_app_version (
    id bigint primary key auto_increment comment '主键',
    app_key varchar(50) not null comment '应用标识 chat',
    platform varchar(20) not null comment '平台 web/windows/macos',
    version_name varchar(30) not null comment '展示版本号 1.2.0',
    version_code int not null comment '版本码 10200，比较大小只用它',
    file_name varchar(255) default '' comment '安装包文件名',
    file_size bigint default 0 comment '安装包字节数',
    download_url varchar(500) default '' comment '安装包下载地址（七牛外链，web 平台可留空）',
    changelog varchar(1000) default '' comment '更新说明',
    force_update char(1) default '0' comment '是否强制更新 0-可选 1-强制',
    status char(1) default '0' comment '状态 0-上架 1-下架',
    create_by varchar(64) default '' comment '创建者',
    update_by varchar(64) default '' comment '更新者',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    deleted tinyint default 0 comment '是否删除 0-否 1-是',
    unique index uk_app_platform_code (app_key, platform, version_code),
    index idx_app_platform (app_key, platform, status)
) engine = InnoDB default charset = utf8mb4 comment '应用版本表';

-- ==========================================================================
-- 后台菜单与权限
-- ==========================================================================
insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, deleted, remark)
values
(2200, '应用版本', 1, 7, 'appVersion', 'system/appVersion/index', '', '', 1, 0,
 'C', '0', '0', 'system:appVersion:list', 'download', 'admin', sysdate(), '', sysdate(), 0, '应用版本管理');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, deleted, remark)
values
(2201, '版本新增', 2200, 1, 'add', '', '', '', 1, 0, 'F', '0', '0', 'system:appVersion:add', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2202, '版本修改', 2200, 2, 'edit', '', '', '', 1, 0, 'F', '0', '0', 'system:appVersion:edit', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2203, '版本删除', 2200, 3, 'remove', '', '', '', 1, 0, 'F', '0', '0', 'system:appVersion:remove', '#', 'admin', sysdate(), '', sysdate(), 0, ''),
(2204, '安装包上传', 2200, 4, 'upload', '', '', '', 1, 0, 'F', '0', '0', 'system:appVersion:upload', '#', 'admin', sysdate(), '', sysdate(), 0, '');

-- 授予超级管理员角色（role_key = admin），普通角色按需在「角色管理」中勾选
insert into sys_role_menu (role_id, menu_id)
select r.id, m.menu_id
from sys_role r
         join sys_menu m on m.menu_id between 2200 and 2204
where r.role_key = 'admin';

-- 回滚脚本（默认注释，谨慎执行）
-- delete from sys_role_menu where menu_id between 2200 and 2204;
-- delete from sys_menu where menu_id between 2200 and 2204;
-- drop table if exists sys_app_version;
