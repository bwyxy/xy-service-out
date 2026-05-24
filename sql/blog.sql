
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
    constraint fk_article_author foreign key (author_id) references sys_user(id) on delete cascade,
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

-- 系统资源表
create table sys_resource (
    id bigint primary key auto_increment comment '资源Id',
    resource_name VARCHAR(255) not null comment '原始文件名',
    resource_key VARCHAR(255) NOT NULL COMMENT '存储唯一标识（七牛云key或OSS对象Key）',
    resource_url VARCHAR(500) NOT NULL COMMENT '访问URL（完整HTTP/HTTPS链接）',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(100) DEFAULT '' COMMENT 'MIME类型，如 image/jpeg',
    suffix VARCHAR(20) DEFAULT '' COMMENT '文件后缀，如 .jpg',
    duration INT DEFAULT 0 COMMENT '音视频时长（秒，非音视频为0）',
    create_time datetime comment '创建时间',
    update_time datetime comment '更新时间'
) engine=innodb comment = '系统资源表';

-- 评论表
create table sys_comment (
    id bigint primary key auto_increment comment '评论Id',
    article_id bigint default null comment '文章Id',
    user_id bigint default null comment '评论用户ID',
    nickname varchar(50) default '' comment '匿名昵称',
    email varchar(100) default '' comment '匿名邮箱',
    comment_type varchar(50) default 'article' comment '评论类型: article-文章 message-留言',
    content text not null comment '评论内容',
    parent_id bigint default null comment '父评论ID',
    root_id bigint default null comment '根评论ID',
    level int default 0 comment  '层级',
    like_count int default 0 comment '点赞数',
    status tinyint default 1 comment '状态: 0-待审核 1显示 2删除',
    create_time datetime default current_timestamp,
    update_time datetime default current_timestamp on update current_timestamp,
    key idx_article_root (article_id, root_id),
    key idx_parent (parent_id),
    key idx_create_time (create_time)
) engine=innodb comment = '评论表';

select st.tag_id, st.name, st.slug, st.color, st.level, st.description, st.create_time, st.update_time, count(sat.article_id) as article_count from sys_tag st
left join sys_article_tag sat on sat.tag_id = st.tag_id
left join sys_article sa on sa.article_id = sat.article_id and sa.status = '1'
group by st.tag_id, st.name