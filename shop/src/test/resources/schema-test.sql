-- 测试用 SQLite 表结构。
--
-- 重要：这里的约束必须与生产库 shop.db 保持一致，否则测不出真实缺陷。
-- 其中 tb_login_logs.user_id 的 NOT NULL 是刻意保留的 —— 它正是缺陷 B1 的成因：
-- 账号不存在时 LoginEvent 的 userId 为 null，观察者插库会抛
-- "NOT NULL constraint failed: tb_login_logs.user_id"。
-- 详见 .tdd/task/login-tdd-tasks.md 功能点 3 / 缺陷清单 B1。

CREATE TABLE IF NOT EXISTS tb_shop (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_name      VARCHAR(100) NOT NULL,
    admin_account  VARCHAR(50)  NOT NULL,
    admin_password VARCHAR(255) NOT NULL,
    logo_url       VARCHAR(500),
    status         INTEGER DEFAULT 1,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_employee (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id         INTEGER NOT NULL,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(500) DEFAULT '/avatars/default.png',
    last_login_time DATETIME,
    login_count     INTEGER DEFAULT 0,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- user_id NOT NULL：与生产一致，用于复现缺陷 B1。
-- 若采纳「待决策项 D4」的方案 ②，这里同步改为允许 NULL 并新增 username 列。
CREATE TABLE IF NOT EXISTS tb_login_logs (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id        INTEGER NOT NULL,
    login_ip       VARCHAR(50),
    login_device   VARCHAR(255),
    login_location VARCHAR(255),
    login_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_agent     VARCHAR(500),
    status         TINYINT DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES tb_employee (id)
);

CREATE TABLE IF NOT EXISTS tb_messages (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id    INTEGER NOT NULL,
    sender_id  INTEGER,
    title      VARCHAR(200),
    content    VARCHAR(1000),
    msg_type   TINYINT DEFAULT 1,
    is_read    TINYINT DEFAULT 0,
    read_time  DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
