# B2C 商城系统

基于 Spring Cloud 微服务架构的 B2C 电商平台，集成 5 种经典设计模式，涵盖用户管理、商品发布、购物车、订单、支付等核心业务模块。

## 技术栈

| 组件 | 技术 |
|------|------|
| 基础框架 | Spring Boot 2.7.18 + JDK 8 |
| 微服务 | Spring Cloud 2021.0.5 + Spring Cloud Alibaba 2021.0.4.0 |
| 网关 | Spring Cloud Gateway |
| 注册/配置中心 | Nacos 1.4.7 |
| 数据库 | SQLite（MyBatis） |
| 缓存 | Redis |
| 密码加密 | BCrypt |

## 项目结构

```
b2c_mall/
├── pom.xml                              ← 父 POM
├── gateway/                             ← 网关模块（端口 8081）
│   ├── pom.xml
│   └── src/main/resources/application.yml
└── shop/                                ← 业务模块（端口 8082）
    ├── pom.xml
    ├── shop.db                          ← SQLite 数据库
    └── src/main/java/com.lxs.b2cmall.shop/
        ├── ShopApplication.java
        ├── config/                      ← Redis 配置
        ├── controller/                  ← 7 个 Controller
        ├── dto/                         ← 数据传输对象
        ├── entity/                      ← 实体类
        ├── event/                       ← 事件类
        ├── mapper/                      ← MyBatis Mapper
        ├── observer/                    ← 观察者
        ├── service/                     ← 业务接口 + 实现
        ├── statemachine/                ← 状态机
        ├── statemode/                   ← 状态模式
        ├── strategy/                    ← 策略模式
        ├── template/                    ← 模板方法
        └── vo/                          ← 视图对象
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Nacos Server（运行在 `47.100.22.158:8848`，namespace: `zhuduole`）
- Redis（运行在 `47.100.22.158:6379`）

### 启动步骤

1. **克隆项目**

```bash
git clone https://github.com/counsellorle/b2c-mall.git
cd b2c-mall
```

2. **启动网关模块**

```bash
cd gateway
mvn spring-boot:run
```

3. **启动业务模块**

```bash
cd shop
mvn spring-boot:run
```

4. **访问服务**

- 网关：`http://localhost:8081`
- 业务服务：`http://localhost:8082`

## API 接口

所有接口通过网关（8081）或直接访问业务服务（8082）。

### 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/employee/login` | 员工登录 |
| POST | `/employee/register` | 员工注册 |

### 商品模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/product/publish?shopId={id}` | 发布商品（后台） |
| GET | `/front/products` | 商品列表（前台） |
| GET | `/front/categories` | 类目列表（前台） |
| GET | `/front/product/{id}` | 商品详情（前台） |

### 购物车模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/cart/add` | 加入购物车 |
| GET | `/cart/list?userId={id}` | 购物车列表 |
| DELETE | `/cart/remove?cartId={id}&userId={id}` | 移除购物车 |

### 订单模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/pay/createOrder` | 创建订单 |
| POST | `/pay/pay?orderId={id}` | 支付订单 |
| GET | `/order/list` | 订单列表 |
| GET | `/order/{id}` | 订单详情 |
| POST | `/order/ship?orderId={id}` | 订单发货 |

### 仪表盘模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/summary` | 数据概览 |
| GET | `/dashboard/trend` | 趋势数据 |
| GET | `/dashboard/messages` | 消息列表 |

## 设计模式

本项目综合运用了 5 种设计模式，详细说明见 [docs/B2C-商城项目核心功能详解.md](docs/B2C-商城项目核心功能详解.md)。

| 设计模式 | 应用场景 | 核心类 |
|----------|---------|--------|
| **观察者模式** | 登录/注册事件的多动作触发 | `LoginEvent`、`AuditLogObserver`、`UserStatusObserver` |
| **模板方法模式** | 商品发布的固定流程 | `AbstractProductPublishTemplate`、`PhysicalProductPublishService`、`VirtualProductPublishService` |
| **状态机** | 订单状态的规则化流转 | `OrderStateMachine`、`OrderStatus`、`OrderEvent` |
| **策略模式** | 多种支付方式的运行时切换 | `PayStrategy`、`AlipayStrategy`、`WechatPayStrategy`、`PayStrategyFactory` |
| **状态模式** | 不同订单状态下的行为差异 | `OrderState`、`PendingPayState`、`PendingShipState`、`ShippedState`、`CompletedState` |

## 数据库

项目使用 SQLite 嵌入式数据库，数据文件位于 `shop/shop.db`，包含以下核心表：

- `tb_employee` — 员工表
- `tb_shop` — 店铺表
- `tb_product` — 商品表
- `tb_category` — 类目表
- `tb_order` — 订单表
- `tb_order_item` — 订单明细表
- `tb_cart` — 购物车表
- `tb_login_log` — 登录日志表
- `tb_message` — 站内消息表
- `tb_order_status_log` — 订单状态流转日志表

## 许可证

MIT License
