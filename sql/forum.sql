-- ==================== 论坛系统数据表 ====================

-- 板块表
CREATE TABLE t_forum_board (
   board_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '板块ID',
   parent_id BIGINT DEFAULT 0 COMMENT '父板块ID，0-顶级',
   name VARCHAR(64) NOT NULL COMMENT '板块名称',
   slug VARCHAR(100) DEFAULT '' COMMENT 'URL别名',
   icon VARCHAR(255) DEFAULT '' COMMENT '图标',
   description VARCHAR(500) DEFAULT '' COMMENT '描述',
   sort_order INT DEFAULT 0 COMMENT '排序',
   status TINYINT DEFAULT 1 COMMENT '0-禁用 1-正常',
   post_count BIGINT DEFAULT 0 COMMENT '帖子数',
   follow_count BIGINT DEFAULT 0 COMMENT '关注数',
   create_time DATETIME COMMENT '创建时间',
   update_time DATETIME COMMENT '更新时间',
   UNIQUE INDEX uk_slug (slug),
   INDEX idx_parent_status (parent_id, status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛板块表';

-- 帖子表
CREATE TABLE t_forum_post (
  post_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
  author_id BIGINT NOT NULL COMMENT '作者u_id',
  board_id BIGINT NOT NULL COMMENT '所属板块',
  title VARCHAR(255) NOT NULL COMMENT '标题',
  summary VARCHAR(500) DEFAULT '' COMMENT '摘要',
  content LONGTEXT COMMENT '正文',
  content_type TINYINT DEFAULT 1 COMMENT '1-Markdown 2-富文本',
  cover_image VARCHAR(255) DEFAULT '' COMMENT '封面图',
  status TINYINT DEFAULT 0 COMMENT '0-草稿 1-审核中 2-已发布 3-拒绝 4-下架',
  visibility TINYINT DEFAULT 1 COMMENT '1-公开 2-好友可见 3-私密',
  is_top TINYINT DEFAULT 0 COMMENT '0-普通 1-置顶',
  is_essence TINYINT DEFAULT 0 COMMENT '0-普通 1-精华',
  view_count BIGINT DEFAULT 0 COMMENT '浏览数',
  like_count BIGINT DEFAULT 0 COMMENT '点赞数',
  comment_count BIGINT DEFAULT 0 COMMENT '评论数',
  favorite_count BIGINT DEFAULT 0 COMMENT '收藏数',
  share_count BIGINT DEFAULT 0 COMMENT '分享数',
  route_key BIGINT DEFAULT 0 COMMENT '分表路由键',
  published_time DATETIME COMMENT '发布时间',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间',
  ip varchar(64) DEFAULT '' COMMENT 'ip地址',
  ip_addr varchar(64) DEFAULT '' COMMENT 'ip所属地',
  INDEX idx_author_status (author_id, status, published_time),
  INDEX idx_board_status (board_id, status, is_top, published_time),
  INDEX idx_status_published (status, published_time),
  INDEX idx_route (route_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛帖子表';

insert into t_forum_post (author_id, board_id, title, summary, content, content_type, cover_image, status, visibility, is_top, is_essence, view_count, like_count, comment_count, favorite_count, share_count, route_key, published_time, create_time, update_time)
    values (10000000, 4, '这是第一篇帖子', '', '', 2, '', 2, 1, 1, 1, 0, 0, 0, 0, 0, 0, now(), now(), now());

-- 评论表
CREATE TABLE t_forum_comment (
 comment_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
 post_id BIGINT NOT NULL COMMENT '帖子ID',
 author_id BIGINT NOT NULL COMMENT '评论者u_id',
 parent_id BIGINT DEFAULT 0 COMMENT '父评论ID，0-根评论',
 root_id BIGINT DEFAULT 0 COMMENT '根评论ID',
 reply_to_uid BIGINT DEFAULT NULL COMMENT '被回复者u_id',
 content TEXT NOT NULL COMMENT '内容',
 like_count BIGINT DEFAULT 0 COMMENT '点赞数',
 status TINYINT DEFAULT 1 COMMENT '0-待审核 1-显示 2-删除',
 ip varchar(64) DEFAULT '' COMMENT 'ip地址',
 ip_addr varchar(64) DEFAULT '' COMMENT 'ip所属地',
 create_time DATETIME COMMENT '创建时间',
 update_time DATETIME COMMENT '更新时间',
 INDEX idx_post_root (post_id, root_id, status, create_time),
 INDEX idx_parent (parent_id),
 INDEX idx_author (author_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛评论表';

-- 点赞表
CREATE TABLE t_forum_like (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                              target_type TINYINT NOT NULL COMMENT '1-帖子 2-评论',
                              target_id BIGINT NOT NULL COMMENT '帖子/评论ID',
                              u_id BIGINT NOT NULL COMMENT '用户ID',
                              status TINYINT DEFAULT 1 COMMENT '1-点赞 0-取消',
                              create_time DATETIME COMMENT '创建时间',
                              update_time DATETIME COMMENT '更新时间',
                              UNIQUE INDEX uk_user_target (u_id, target_type, target_id),
                              INDEX idx_target (target_type, target_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛点赞表';

-- 收藏表
CREATE TABLE t_forum_favorite (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                  post_id BIGINT NOT NULL COMMENT '帖子ID',
                                  u_id BIGINT NOT NULL COMMENT '用户ID',
                                  folder_id BIGINT DEFAULT 0 COMMENT '收藏夹ID',
                                  create_time DATETIME COMMENT '创建时间',
                                  update_time DATETIME COMMENT '更新时间',
                                  UNIQUE INDEX uk_user_post (u_id, post_id),
                                  INDEX idx_user_folder (u_id, folder_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛收藏表';

-- 话题表
CREATE TABLE t_forum_topic (
                               topic_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '话题ID',
                               name VARCHAR(64) NOT NULL COMMENT '话题名',
                               slug VARCHAR(100) DEFAULT '' COMMENT 'URL别名',
                               description VARCHAR(255) DEFAULT '' COMMENT '描述',
                               cover_image VARCHAR(255) DEFAULT '' COMMENT '封面',
                               post_count BIGINT DEFAULT 0 COMMENT '帖子数',
                               follow_count BIGINT DEFAULT 0 COMMENT '关注数',
                               status TINYINT DEFAULT 1 COMMENT '0-禁用 1-正常',
                               create_time DATETIME COMMENT '创建时间',
                               update_time DATETIME COMMENT '更新时间',
                               UNIQUE INDEX uk_name (name),
                               UNIQUE INDEX uk_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛话题表';

-- 帖子-话题关联表
CREATE TABLE t_forum_post_topic (
                                    post_id BIGINT NOT NULL COMMENT '帖子ID',
                                    topic_id BIGINT NOT NULL COMMENT '话题ID',
                                    PRIMARY KEY (post_id, topic_id),
                                    INDEX idx_topic (topic_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '帖子话题关联表';

-- 用户关注表
CREATE TABLE t_forum_follow (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                u_id BIGINT NOT NULL COMMENT '关注者u_id',
                                follow_type TINYINT NOT NULL COMMENT '1-用户 2-板块 3-话题',
                                target_id BIGINT NOT NULL COMMENT '被关注对象ID',
                                create_time DATETIME COMMENT '创建时间',
                                update_time DATETIME COMMENT '更新时间',
                                UNIQUE INDEX uk_user_target (u_id, follow_type, target_id),
                                INDEX idx_target (follow_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛关注表';

-- 版主表
CREATE TABLE t_forum_moderator (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                   board_id BIGINT NOT NULL COMMENT '板块ID',
                                   u_id BIGINT NOT NULL COMMENT '用户ID',
                                   role TINYINT DEFAULT 1 COMMENT '1-版主 2-超级版主',
                                   create_time DATETIME COMMENT '创建时间',
                                   update_time DATETIME COMMENT '更新时间',
                                   UNIQUE INDEX uk_board_user (board_id, u_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '版主表';

-- 举报表
CREATE TABLE t_forum_report (
                                report_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID',
                                reporter_id BIGINT NOT NULL COMMENT '举报人u_id',
                                target_type TINYINT NOT NULL COMMENT '1-帖子 2-评论 3-用户',
                                target_id BIGINT NOT NULL COMMENT '被举报对象ID',
                                reason VARCHAR(255) DEFAULT '' COMMENT '举报原因',
                                status TINYINT DEFAULT 0 COMMENT '0-待处理 1-已处理',
                                handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
                                result VARCHAR(500) DEFAULT '' COMMENT '处理结果',
                                create_time DATETIME COMMENT '创建时间',
                                update_time DATETIME COMMENT '更新时间',
                                INDEX idx_status (status, create_time),
                                INDEX idx_target (target_type, target_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛举报表';

-- 论坛用户扩展资料表
CREATE TABLE t_forum_user_profile (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                      u_id BIGINT NOT NULL COMMENT '用户ID',
                                      level INT DEFAULT 1 COMMENT '等级',
                                      signature VARCHAR(255) DEFAULT '' COMMENT '签名',
                                      post_count BIGINT DEFAULT 0 COMMENT '发帖数',
                                      comment_count BIGINT DEFAULT 0 COMMENT '评论数',
                                      like_received_count BIGINT DEFAULT 0 COMMENT '获赞数',
                                      score BIGINT DEFAULT 0 COMMENT '积分',
                                      create_time DATETIME COMMENT '创建时间',
                                      update_time DATETIME COMMENT '更新时间',
                                      UNIQUE INDEX uk_uid (u_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛用户扩展资料表';

-- 论坛操作日志表
CREATE TABLE t_forum_oper_log (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
                                  u_id BIGINT DEFAULT NULL COMMENT '操作人ID',
                                  oper_type VARCHAR(64) NOT NULL COMMENT '操作类型',
                                  target_type VARCHAR(64) DEFAULT '' COMMENT '对象类型',
                                  target_id BIGINT DEFAULT NULL COMMENT '对象ID',
                                  detail TEXT COMMENT '操作详情',
                                  ip VARCHAR(64) DEFAULT '' COMMENT 'IP地址',
                                  create_time DATETIME COMMENT '创建时间',
                                  INDEX idx_oper_type (oper_type, create_time),
                                  INDEX idx_uid (u_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '论坛操作日志表';

-- 初始化示例板块数据
INSERT INTO t_forum_board (parent_id, name, slug, icon, description, sort_order, status, post_count, follow_count, create_time, update_time) VALUES
                                                                                                                                                 (0, '综合讨论', 'general', '', '论坛综合交流板块', 1, 1, 0, 0, NOW(), NOW()),
                                                                                                                                                 (0, '技术分享', 'tech', '', '技术文章与经验分享', 2, 1, 0, 0, NOW(), NOW()),
                                                                                                                                                 (0, '生活闲聊', 'life', '', '日常生活、兴趣爱好交流', 3, 1, 0, 0, NOW(), NOW());

-- 初始化示例话题数据
INSERT INTO t_forum_topic (name, slug, description, status, post_count, follow_count, create_time, update_time) VALUES
                                                                                                                    ('SpringBoot', 'springboot', 'SpringBoot 技术话题', 1, 0, 0, NOW(), NOW()),
                                                                                                                    ('人工智能', 'ai', 'AI 技术与应用话题', 1, 0, 0, NOW(), NOW()),
                                                                                                                    ('生活随拍', 'daily-photo', '分享日常生活照片', 1, 0, 0, NOW(), NOW());
