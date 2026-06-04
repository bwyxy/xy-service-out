-- 漫画表
create table sys_comic (
    comic_id bigint primary key auto_increment comment '漫画ID',
    title varchar(255) not null comment '漫画名称',
    cover_url varchar(255) default '' not null comment '封面图',
    description text comment '简介',
    author_id varchar(100) default '' comment '作者Id',
    category_id bigint comment '分类Id',
    status varchar(1) default '0' comment '连载状态：0-连载中 1-已完结 2-暂停',
    total_views bigint default 0 comment '总浏览量',
    total_favorites int default 0 comment '总收藏数',
    last_chapter_id bigint default null comment '最新章节ID',
    last_chapter_name varchar(255) default null comment '最新章节名称',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    key idx_category (category_id),
    key idx_status_update (status, update_time),
    key idx_total_favorites (total_favorites DESC),
    constraint fk_comic_article foreign key  (author_id) references sys_user(id),
    constraint fk_comic_category foreign key (category_id) references sys_category(category_id) on delete restrict
) engine=innodb comment = '漫画表';