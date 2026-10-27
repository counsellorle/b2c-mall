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
    order_no   VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- C 端用户表（与生产库 shop.db 保持一致，2026-10-23 P0 变更新增）
CREATE TABLE IF NOT EXISTS tb_user (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(100),
    phone      VARCHAR(20),
    status     TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 订单表（与生产库保持一致，供 Dashboard / 订单模块测试使用）
CREATE TABLE IF NOT EXISTS tb_order (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no          TEXT UNIQUE,
    user_id           INTEGER,
    shop_id           INTEGER,
    total_amount      REAL,
    pay_amount        REAL,
    status            TEXT,
    pay_type          TEXT,
    receiver_name     TEXT,
    receiver_phone    TEXT,
    receiver_address  TEXT,
    shipping_status   TEXT,
    after_sale_status TEXT,
    is_deleted        TINYINT DEFAULT 0,
    delivery_type     VARCHAR(50) DEFAULT '普通快递',
    create_time       TEXT,
    pay_time          TEXT,
    ship_time         TEXT
);

-- 商品表（与生产库保持一致，2026-10-23 P0 变更新增销量/双库存/配送方式等字段）
CREATE TABLE IF NOT EXISTS tb_product (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    shop_id          INTEGER NOT NULL,
    name             VARCHAR(200) NOT NULL,
    keyword          VARCHAR(200),
    selling_point    VARCHAR(500),
    category_id      INTEGER,
    shop_category_id INTEGER,
    product_type     VARCHAR(20) NOT NULL,
    main_image       VARCHAR(500),
    video            VARCHAR(500),
    brand            VARCHAR(100),
    price            REAL NOT NULL,
    market_price     REAL,
    stock            INTEGER DEFAULT 0,
    stock_warn       INTEGER DEFAULT 10,
    real_sales       INTEGER DEFAULT 0,
    fake_sales       INTEGER DEFAULT 0,
    warehouse_stock  INTEGER DEFAULT 0,
    store_stock      INTEGER DEFAULT 0,
    delivery_methods VARCHAR(200),
    product_category VARCHAR(20),
    product_code     VARCHAR(100),
    status           VARCHAR(20) DEFAULT 'draft',
    sort             INTEGER DEFAULT 0,
    compliance_info  VARCHAR(500),
    create_time      TEXT,
    update_time      TEXT
);

-- 订单商品行表
CREATE TABLE IF NOT EXISTS tb_order_item (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id      INTEGER,
    product_id    INTEGER,
    product_name  TEXT,
    product_image TEXT,
    price         REAL,
    quantity      INTEGER,
    subtotal      REAL
);

-- 订单状态流转日志（P3 订单详情/退款金额统计依赖）
CREATE TABLE IF NOT EXISTS tb_order_status_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id    INTEGER,
    from_status TEXT,
    to_status   TEXT,
    operator    TEXT,
    remark      TEXT,
    create_time TEXT
);
