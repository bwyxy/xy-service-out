CREATE TABLE IF NOT EXISTS t_user (
    u_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    password VARCHAR(500) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255) DEFAULT '',
    phone VARCHAR(11) DEFAULT '',
    email VARCHAR(50) DEFAULT '',
    status CHAR(1) DEFAULT '0',
    sex CHAR(1) DEFAULT '0',
    wx_open_id VARCHAR(255) DEFAULT '',
    is_bind_chat CHAR(1) DEFAULT '1',
    create_time DATETIME,
    update_time DATETIME
);

CREATE TABLE IF NOT EXISTS t_friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    u_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status CHAR(1) DEFAULT '0',
    create_time DATETIME,
    update_time DATETIME
);

CREATE TABLE IF NOT EXISTS t_friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_uid BIGINT NOT NULL,
    to_uid BIGINT NOT NULL,
    status TINYINT DEFAULT 0,
    apply_msg VARCHAR(255) DEFAULT '',
    reply_msg VARCHAR(255) DEFAULT '',
    create_time DATETIME,
    update_time DATETIME
);

CREATE TABLE IF NOT EXISTS t_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    u_id BIGINT NOT NULL,
    type TINYINT NOT NULL,
    title VARCHAR(100) DEFAULT '',
    content VARCHAR(500) DEFAULT '',
    related_id BIGINT,
    is_read TINYINT DEFAULT 0,
    extra VARCHAR(500),
    create_time DATETIME,
    update_time DATETIME
);

DELETE FROM t_user;
DELETE FROM t_friend;
DELETE FROM t_friend_request;
DELETE FROM t_notification;

INSERT INTO t_user (u_id, password, nickname, avatar, phone, email, status, sex, create_time, update_time) VALUES
(10001, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '用户A', '', '13800000001', 'a@test.com', '0', '1', NOW(), NOW()),
(10002, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '用户B', '', '13800000002', 'b@test.com', '0', '2', NOW(), NOW()),
(10003, '$2a$10$Bqq1b4dioOjYqdaFuZH3Au/zxA8UheNcQaSnUWy0UuUX7haYobVK2', '用户C', '', '13800000003', 'c@test.com', '0', '1', NOW(), NOW());
