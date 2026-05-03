
-- 文章分类表
create table sys_category (
    category_id bigint primary key auto_increment comment '分类Id',
    name varchar(24) not null comment '分类名称',
    slug varchar(100) default '' comment 'URL别名',
    description varchar(255) default '' comment '描述',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    index idx_name(name),
    index idx_slug(slug)
) engine=innodb comment = '文章分类表';

-- 文章标签表
create table sys_tag(
    tag_id bigint primary key auto_increment comment '标签Id',
    name varchar(24) not null comment '标签名称',
    slug varchar(100) default '' comment 'URL别名',
    description varchar(255) default '' comment '描述',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    index idx_name(name),
    index idx_slug(slug)
) engine=innodb comment = '文章分类表';

-- 文章表
create table sys_article(
    article_id bigint primary key auto_increment comment '文章Id',
    title varchar(255) not null comment '标题',
    slug varchar(100) default '' comment 'URL别名',
    summary varchar(500) default '' COMMENT '摘要',
    content longtext default null comment '正文',
    cover_image varchar(255) default '' comment '封面',
    status tinyint not null DEFAULT 1 COMMENT '状态：0草稿 1已发布 2私密',
    view_count int not null DEFAULT 0 COMMENT '浏览次数',
    like_count int not null default 0 comment '点赞数',
    comment_count int not null default 0 comment '评论数',
    author_id bigint not null comment '作者Id',
    category_id bigint not null comment '分类Id',
    published_time datetime default null comment '发布时间',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    unique key uk_slug(slug),
    key idx_author(article_id),
    key idx_category(category_id),
    key idx_status_published(status, published_time),
    constraint fk_article_author foreign key (article_id) references sys_user(id) on delete cascade,
    constraint fk_article_category foreign key (category_id) references sys_category(category_id) on delete restrict
) engine=innodb comment = '文章表';

-- 文章标签关联表
create table sys_article_tag(
    article_id bigint comment '文章Id',
    tag_id bigint comment '标签Id',
    primary key (article_id, tag_id),
    constraint fk_article_tag_article foreign key (article_id) references sys_article(article_id) on delete cascade,
    constraint fk_article_tag_tag foreign key (tag_id) references sys_tag(tag_id) on delete cascade
) engine=innodb comment = '文章标签关联表';

-- 博客系统配置表
create table sys_blog_config (
    id bigint primary key auto_increment comment '配置ID',
    config_key varchar(48) not null comment '配置标识',
    config_value varchar(255) default '' comment '值',
    description varchar(255) default '' comment '描述',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间',
    index idx_config_key(config_key)
) engine=innodb comment = '博客系统配置表';