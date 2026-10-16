# 用户登录功能 — TDD 任务拆解

> 生成依据：`tdd_task-prompt.md`
> 目标项目：`b2c_mall`（Spring Cloud 微服务）
> 拆解日期：2026-10-13

---

## 需求概述

电商 B2C 商城的店铺管理员使用「用户名 + 密码」在登录页提交凭证，服务端校验通过后允许进入系统后台。失败时给出明确且不泄露内部信息的提示，并对每一次登录尝试留痕。

本次拆解覆盖三个层次：**shop-server 的登录主流程**、**gateway 的登录路由与凭证校验**、**登录行为产生的副作用（审计日志 / 登录统计）的正确性**。

服务端校验链的既定顺序（现状代码，改动需在任务项层面显式声明）：

```
取客户端 IP / UA  →  按 username 查库  →  BCrypt 校验密码  →  status == 1 判定启用  →  清空返回密码  →  发布登录事件
```

---

## 项目架构快照

拆解「功能上下文」之前必须先固定架构事实，否则上下文划分就是臆测。

| 维度 | 事实 |
|---|---|
| 架构模式 | Spring Cloud 微服务（非 MVC 单体），Spring Boot 2.7.18 / Spring Cloud 2021.0.5 / Nacos 2021.0.4.0 |
| 模块 | `gateway`（8081，Spring Cloud Gateway + LoadBalancer + Nacos 发现）、`shop`（8082，业务服务） |
| `shop` 内部分层 | Controller → Service(接口 + impl) → MyBatis **注解式** Mapper → Entity / DTO / VO |
| 横切机制 | Spring 事件 + 观察者模式：`LoginEvent` → `AuditLogObserver`、`UserStatusObserver`；`RegisterEvent` → `AccountInitObserver`、`WelcomeNotifyObserver` |
| 持久化 | SQLite，`jdbc:sqlite:F:/ssd/b2c_mall/shop/shop.db?foreign_keys=on`（注意 `foreign_keys=on` 是打开的） |
| 缓存 | Redis（阿里云 `47.100.22.158:6379`），`RedisConfig` 已定义 `RedisTemplate` Bean，但**全项目零引用** |
| 配置来源 | `bootstrap.yml` + `application.yml`，另有 Nacos 远程配置（`namespace: zhudole`）可能覆盖本地 |
| View 层 | **不存在**。这是纯 REST 后端，无模板渲染。因此「View 层上下文」在本项目中的等价物是 **DTO 参数校验边界 + 响应契约** |
| 测试现状 | **无 `src/test` 目录，pom 中无 `spring-boot-starter-test`** —— 测试脚手架需要从零搭建 |
| 网关自定义逻辑 | `GatewayApplication` 仅有 `@EnableDiscoveryClient`，**无任何自定义 Filter**；路由规则疑似由 Nacos 远程配置下发 |

**为什么这个快照决定了上下文怎么切：**

1. `gateway` 与 `shop` 的测试手段完全不同（路由/过滤器测试用 `WebTestClient` 或过滤器直调，业务逻辑测试用 `MockMvc` / Mockito），所以它们必须是两套独立的功能上下文，不能混在一起。
2. 本项目**用 Spring 事件替代了直接的 Service 调用**来做审计和统计更新，这意味着「登录成功」这件事的后果分散在两个观察者里。副作用不拆成独立上下文，就会漏测。
3. **没有 View 层** → 模板里「View 层 - 登录界面」这一层在本项目中不成立，等价物是 Controller 的入参校验与响应结构。

---

## 功能点 1：用户输入正确的用户名和密码可以登录成功

### 功能上下文 1.1：Gateway 层 — 登录请求放行与转发

- **任务项 1.1.1**：`/employee/login` 必须在网关鉴权白名单内，未携带凭证也能访问
  - *测试思路*：构造无 Authorization 头的 `POST /employee/login`，断言网关未返回 401/403，而是正常转发到 `shop-server`。边界在于：白名单之外的接口在同样条件下**必须**返回 401
- **任务项 1.1.2**：转发时必须原样保留请求体、`X-Forwarded-For`、`User-Agent`
  - *测试思路*：断言下游收到的 body 与客户端发出的 JSON 字节一致，且 `X-Forwarded-For` / `User-Agent` 与客户端发送值相同。这三个字段直接决定审计日志里写的是真实 IP 还是网关内网 IP
- **任务项 1.1.3**：登录成功响应必须原样透传，不被网关包装或改写
  - *测试思路*：断言经网关返回的响应体与直连 `shop-server` 的响应体结构完全一致（`code` / `message` / `data` 三字段不变形）

### 功能上下文 1.2：Controller 层 — `LoginController.login`

- **任务项 1.2.1**：`POST /employee/login` 接收 JSON 并绑定到 `LoginDTO`
  - *测试思路*：MockMvc `post("/employee/login")` 发送 `{"username":"shop01","password":"123456"}`，断言 HTTP 200 且响应 `code == 200`
- **任务项 1.2.2**：Controller 把请求原样委托给 `EmployeeService.login(dto, request)`
  - *测试思路*：Mockito `verify(employeeService, times(1)).login(same(dto), any())`，断言调用次数为 1 且传入的是同一个 DTO 实例，没有被重建
- **任务项 1.2.3**：Service 正常返回时，Controller 用 `Result.success` 包装
  - *测试思路*：`given(service.login(...)).willReturn(employee)`，断言 `data.id` / `data.username` 正确，且 `message == "success"`
- **任务项 1.2.4**：响应体绝不能包含 `password` 字段
  - *测试思路*：断言响应 JSON 中不存在 `data.password` 键，或其为 `null`。这是安全断言，边界在于**任何**登录响应（含异常响应）都不得回显密码或密码 hash

### 功能上下文 1.3：Service 层 — `EmployeeServiceImpl.login` 校验链

- **任务项 1.3.1**：按 `username` 精确查询员工记录
  - *测试思路*：`verify(employeeMapper).findByUsername("shop01")` 恰好调用 1 次，且参数等于传入的用户名
- **任务项 1.3.2**：用 `BCrypt.checkpw(明文, 库中hash)` 校验密码
  - *测试思路*：库中储存 `BCrypt.hashpw("123456", gensalt())`，断言传 `"123456"` 通过。边界用例：`"123456 "`（尾随空格）、`"123456"` 大小写变形、空串、超长串（> 72 字节，BCrypt 的静默截断边界）均应失败
- **任务项 1.3.3**：校验通过后把返回对象中的密码置空
  - *测试思路*：断言 `employee.getPassword() == null`，同时断言 `id` / `username` / `shopId` 等非敏感字段未被清掉
- **任务项 1.3.4**：`status == 1` 判定为启用
  - *测试思路*：参数化测试 `status = 1` 返回成功；`status = 0`、`status = 2`、`status = null` 全部拒绝。**注意 `status` 是 `Integer`，`null` 用例会命中拆箱 NPE，需要单独断言**
- **任务项 1.3.5**：登录成功后返回的 `Employee` 是持久化实体的直接引用，状态被就地修改
  - *测试思路*：断言 `employeeMapper.findByUsername` 返回的对象在 `login` 返回后 `password` 已被改写。这条测试的目的是**锁定副作用范围**——后续重构为不可变 VO 时，这条测试必须显式修改，而不是静默通过

### 功能上下文 1.4：数据层 — `EmployeeMapper` 与 SQLite

- **任务项 1.4.1**：`findByUsername` 的下划线→驼峰映射正确
  - *测试思路*：插入一条 `shop_id = 7` 的记录，断言查出的 `Employee.getShopId() == 7`（验证 `@Results` 注解是否覆盖了全部字段，漏掉一个就会静默为 `null`）
- **任务项 1.4.2**：用户名不存在时返回 `null`，不抛异常
  - *测试思路*：查询 `"__not_exist__"`，断言返回 `null` 且无异常抛出
- **任务项 1.4.3**：密码列以 BCrypt hash 形态存储，存储长度不超过 255
  - *测试思路*：断言库中 `password` 以 `$2a$10$` 开头且长度 60，绝不等于明文
- **任务项 1.4.4**：`username` 唯一性约束
  - *测试思路*：连续插入两条相同 `username`，断言第二条失败。**当前 `tb_employee` 没有唯一索引，此用例必然失败** —— 该任务项对应缺陷 B10

### 功能上下文 1.5：事件与观察者 — 登录成功产生的副作用

- **任务项 1.5.1**：成功后发布 `success = true` 的 `LoginEvent`，且携带真实 `userId`
  - *测试思路*：`ArgumentCaptor<LoginEvent>` 捕获事件，断言 `isSuccess() == true` 且 `getUserId()` 等于登录用户的 `id`
- **任务项 1.5.2**：`UserStatusObserver` 更新 `login_count` 与 `last_login_time`
  - *测试思路*：连续登录两次，断言 `login_count` 由 N 递增到 N+2，`last_login_time` 非空且单调不减。**边界：必须是 `+1` 而不是 `+2`，观察者被重复注册就会翻倍**
- **任务项 1.5.3**：`AuditLogObserver` 写入一条 `status = 1` 的登录日志
  - *测试思路*：断言 `tb_login_logs` 新增 1 行，`status == 1`、`user_id` 等于登录用户 id、`login_time` 非空
- **任务项 1.5.4**：登录成功不得触发注册链路的副作用
  - *测试思路*：断言 `tb_messages` 行数不变（`WelcomeNotifyObserver` 只应响应 `RegisterEvent`）。边界：跨事件串扰是最典型的观察者模式回归缺陷
- **任务项 1.5.5**：`loginDevice` 应承载真实设备信息，而非与 `userAgent` 重复
  - *测试思路*：断言 `event.getLoginDevice() != event.getUserAgent()`（或 `loginDevice` 为解析后的结构化值）。**当前实现传的是同一个值，此用例必然失败** —— 对应缺陷 B4

---

## 功能点 2：用户输入错误的密码不能登录

### 功能上下文 2.1：Service 层 — 密码不匹配的处理

- **任务项 2.1.1**：密码不匹配时抛出 `RuntimeException`，消息为「密码错误」
  - *测试思路*：`assertThrows(RuntimeException.class, ...)` 并断言 `getMessage()` 精确等于 `"密码错误"`。边界：不能是 `NullPointerException` 之类的其他异常类型
- **任务项 2.1.2**：密码不匹配时不得改写 `login_count` / `last_login_time`
  - *测试思路*：`verify(employeeMapper, never()).updateLoginInfo(anyInt())`，并用数据库真实行断言计数未变
- **任务项 2.1.3**：密码不匹配时仍需发布 `success = false` 的 `LoginEvent`，且 `userId` 为**真实用户 id**（不是 `null`）
  - *测试思路*：捕获事件断言 `isSuccess() == false` 且 `getUserId() != null`。边界的意义在于：只有能定位到具体账号，才能做「针对该账号的失败次数锁定」；`userId` 为 `null` 会让风控失效

### 功能上下文 2.2：审计侧 — 失败尝试的留痕

- **任务项 2.2.1**：写入一条 `status = 0` 的失败日志
  - *测试思路*：断言 `tb_login_logs` 新增 1 行且 `status == 0`
- **任务项 2.2.2**：失败日志的 `user_id` 必须是真实用户 id，不允许为 `null`
  - *测试思路*：断言 `user_id` 非空且与外键 `tb_employee(id)` 对应记录存在。若此处为 `null`，因 `user_id INTEGER NOT NULL` + `foreign_keys=on`，插入会直接抛 `IntegrityError`
- **任务项 2.2.3**：日志只写一行，不重复
  - *测试思路*：断言行数恰好 `+1`。多次调用 `publishEvent` 或观察者被注册两次都会导致多写

### 功能上下文 2.3：Controller 层 — 失败响应契约

- **任务项 2.3.1**：返回 `Result.error`，`message` 为「密码错误」
  - *测试思路*：`given(service.login(...)).willThrow(new RuntimeException("密码错误"))`，断言响应 `code != 200` 且 `message == "密码错误"`
- **任务项 2.3.2**：错误响应不得泄露内部细节（SQL 语句、密码 hash、堆栈）
  - *测试思路*：断言 `message` 中不出现 `SQL`、`sqlite`、`Exception`、`$2a$` 等特征串
- **任务项 2.3.3**：HTTP 状态码与业务错误码的语义约定
  - *测试思路*：明确断言失败时 HTTP 状态码到底是 200 + `code=500`，还是 401/400。**当前实现是「恒 200 + code=500」，这是一个契约决策点，必须在测试里固化下来** —— 对应缺陷 B5

---

## 功能点 3：账号不存在的登录尝试产生正确的提示与留痕

> 本功能点包含一个**已确认的现存缺陷**，是本轮 TDD 最该优先修复的路径。

### 功能上下文 3.1：Service 层 — 用户名查不到时的分支

- **任务项 3.1.1**：查不到记录时抛出 `RuntimeException`，消息为「账号不存在」
  - *测试思路*：`given(employeeMapper.findByUsername(any())).willReturn(null)`，断言抛出的异常 `getMessage()` **精确等于** `"账号不存在"`。
    **当前该用例会失败**：因为观察者会拿 `userId = null` 去插 `tb_login_logs`，抛出的实际是 `NOT NULL constraint failed: tb_login_logs.user_id`，被 Controller 捕获后回显给前端的是数据库错误 —— 缺陷 B1（已通过直接对 `shop.db` 执行 INSERT 复现验证）
- **任务项 3.1.2**：账号不存在时仍发布 `LoginEvent`（`userId` 允许为 `null`），保留审计意图
  - *测试思路*：断言事件被发布且 `isSuccess() == false`。这条测试的作用是**锁住"要审计"这个意图**，至于怎么落库由 3.2 决定

### 功能上下文 3.2：审计侧 — 无主账号的日志落库策略

- **任务项 3.2.1**：`userId == null` 的事件不得导致主流程中断
  - *测试思路*：给定 `userId = null` 的 `LoginEvent`，断言调用 `login()` 抛出的是**业务语义异常**（`"账号不存在"`），而不是 `org.sqlite.SQLiteException` / `DataIntegrityViolationException`。等价断言：断言异常消息中不含 `constraint`
  - *可选修复方向*（需团队定夺，见「待决策项 D4」）：① 观察者在 `userId == null` 时跳过落库并降级为日志输出；② 重建 `tb_login_logs` 表使 `user_id` 允许 `NULL` 并新增 `username` 列；③ 事件中携带 `username`，审计表以 `username` 为主键位
- **任务项 3.2.2**：任何事件监听器抛异常都不得污染业务异常
  - *测试思路*：让 `loginLogMapper.insert` 抛 `RuntimeException`，断言 `login()` 抛出/返回的仍是业务语义结果。**当前实现不满足此条**：Spring 事件默认是**同步**发布，监听器异常会沿 `publishEvent` 冒泡
  - *修复方向*：观察者内部 try-catch 兜底，或改用 `@TransactionalEventListener(phase = AFTER_COMMIT)` + 异步执行

### 功能上下文 3.3：Controller 层 — 提示语是否防账号枚举

- **任务项 3.3.1**：账号不存在的对外提示语
  - *测试思路*：断言 `message` 为既定文案。**存在两个互斥方案，必须择一并在测试中固化**：保留「账号不存在」（当前行为，便于运营排查）vs 统一为「用户名或密码错误」（防账号枚举，安全性更好）—— 见「待决策项 D3」

---

## 功能点 4：账号被禁用时不能登录

### 功能上下文 4.1：Service 层 — 状态校验

- **任务项 4.1.1**：`status != 1` 时抛出 `RuntimeException`，消息为「账号已被禁用」
  - *测试思路*：参数化 `status = 0 / 2 / -1`，断言异常消息一致
- **任务项 4.1.2**：状态校验必须发生在密码校验**之后**
  - *测试思路*：给定 `status = 0` **且密码错误**，断言抛出的是「密码错误」而非「账号已被禁用」。这条断言的意义是防止攻击者用任意密码探测某账号是否被禁用（状态泄露）
- **任务项 4.1.3**：禁用账号登录失败不得更新登录统计
  - *测试思路*：`verify(employeeMapper, never()).updateLoginInfo(anyInt())` 并断言数据库行未变

### 功能上下文 4.2：审计侧 — 禁用账号的留痕缺口

- **任务项 4.2.1**：禁用账号的登录尝试应写入一条失败日志
  - *测试思路*：断言 `status = 0` 的用户尝试登录后，`tb_login_logs` 新增 1 行且 `status = 0`。
    **当前该用例必然失败**：`status != 1` 分支直接 `throw`，没有 `publishEvent` —— 缺陷 B2。这类"漏记"在安全审计上属于高危项（攻击者可以持续探测被禁账号而不留痕）

---

## 功能点 5：入参校验（DTO 边界与响应契约）

### 功能上下文 5.1：Controller 层 — `@Valid` 校验边界

- **任务项 5.1.1**：`username` 为空或空白 → 拒绝，提示「账号不能为空」
  - *测试思路*：发送 `{"password":"x"}`、`{"username":"","password":"x"}`、`{"username":"   ","password":"x"}`，三者都应被拒。边界：`@NotBlank` 对全空白生效，而 `@NotNull` 不会 —— 断言失败是刻意的
- **任务项 5.1.2**：`password` 为空 → 拒绝，提示「密码不能为空」
  - *测试思路*：发送 `{"username":"shop01"}`，断言被拒且提示为密码相关文案
- **任务项 5.1.3**：校验失败的响应结构必须与其他错误路径一致（`Result` 结构）
  - *测试思路*：断言校验失败响应的 JSON 含 `code` / `message` / `data` 三字段。
    **当前该用例必然失败**：`@Valid` 失败抛的是 `MethodArgumentNotValidException`，而 `LoginController` 只 `catch (RuntimeException)`，实际返回的是 Spring 默认错误体 —— 缺陷 B6
- **任务项 5.1.4**：请求体格式非法（非 JSON / 空 body）的响应契约
  - *测试思路*：发送 `Content-Type: application/json` 但 body 为空串，断言返回结构一致而非 Spring 默认 400 页面
- **任务项 5.1.5**：`password` 长度上限策略
  - *测试思路*：构造 72 字节以上的密码，断言超长部分被拒绝或明确接受。BCrypt 只取前 72 字节，**超长密码的静默截断会让「前 72 字节相同的两个不同密码」互相可登录**，这是一个真实的安全边界 —— 见「待决策项 D5」

---

## 功能点 6：登录成功后维持登录状态（凭证签发与校验）

> 现状：登录成功后仅返回 `Employee`，**不签发任何凭证**。本功能点属于新增能力，方案未定 —— 见「待决策项 D1」。以下按**默认推荐方案（JWT + 网关校验）** 拆解，若改用 Session + Redis，任务项 6.1.1 / 6.4.x 需替换。

### 功能上下文 6.1：Service 层 — 签发令牌

- **任务项 6.1.1**：密码校验通过后签发 JWT，`payload` 至少含 `userId`、`shopId`，并设置过期时间
  - *测试思路*：断言返回的 token 可被解析，`userId` / `shopId` 与登录用户一致，`exp - iat` 等于配置的有效期
- **任务项 6.1.2**：token 中不得包含密码、密码 hash 或任何可直接复用为凭证的敏感字段
  - *测试思路*：解析 JWT payload，断言不含 `password` 键，且 base64 解码后的原文中不出现 `$2a$`
- **任务项 6.1.3**：签发密钥来自配置（Nacos / 环境变量），不硬编码
  - *测试思路*：断言密钥来自 `@Value` / `@ConfigurationProperties`，可通过测试属性覆盖。边界：测试环境与生产密钥必须可区分

### 功能上下文 6.2：Controller 层 — 凭证下发

- **任务项 6.2.1**：登录成功响应体中包含 token 字段
  - *测试思路*：断言响应 `data.token` 非空且为三段式 JWT 结构
- **任务项 6.2.2**：凭证下发方式约定（响应体 vs `Set-Cookie`）
  - *测试思路*：二选一并在测试中固化。若用 Cookie，断言 `HttpOnly` / `Secure` / `SameSite` 属性正确

### 功能上下文 6.3：Gateway 层 — 凭证校验

- **任务项 6.3.1**：受保护接口未携带 token → 401
  - *测试思路*：无 `Authorization` 头请求受保护路径，断言网关返回 401 且**未转发**给下游
- **任务项 6.3.2**：token 非法 / 签名错误 / 已过期 → 401
  - *测试思路*：三组参数化用例：篡改签名、篡改 payload、构造 `exp` 为过去时间。边界：过期与签名错误应返回不同的可区分错误码，便于前端决定是否静默续期
- **任务项 6.3.3**：合法 token → 解析出 `userId` 并通过请求头透传给下游
  - *测试思路*：断言下游收到的 `X-User-Id` 等头与 token 中的 `userId` 一致，且**不信任**客户端自带的同名头（客户端伪造 `X-User-Id` 必须被覆盖）
- **任务项 6.3.4**：白名单路径与受保护路径的判定互斥且无遗漏
  - *测试思路*：遍历白名单逐个断言放行，再遍历一遍受保护路径断言拦截。边界：路径前缀匹配要防止 `/employee/login/../orders` 这类绕过

### 功能上下文 6.4：Redis 层 — 凭证生命周期（仅 Session / 黑名单方案需要）

- **任务项 6.4.1**：`RedisTemplate` 的序列化配置能正确读写会话对象
  - *测试思路*：写入一个对象再读回，断言字段不丢失。**注意 `RedisConfig` 启用了 `activateDefaultTyping`，读回的对象可能带类型信息，断言要不依赖具体类名**
- **任务项 6.4.2**：登出 / 强制失效后，旧凭证不可再用
  - *测试思路*：登出后携带原 token 请求受保护接口，断言 401

---

## 功能点 7：登录失败次数限制与账号临时锁定

> 现状：**完全没有**任何限流或锁定，Redis 虽已配置但零引用。本功能点属于新增能力，范围未定 —— 见「待决策项 D2」。

### 功能上下文 7.1：Redis 层 — 失败计数器

- **任务项 7.1.1**：同一 `username` 连续失败达到 N 次后进入锁定
  - *测试思路*：连续 N 次错误密码，断言第 N 次时抛出/返回「账号已被锁定」；断言第 N+1 次即使**密码正确**也被拒绝
- **任务项 7.1.2**：计数的粒度与键设计
  - *测试思路*：断言 key 形如 `login:fail:{username}`；断言不同账号之间计数互不影响（隔离性边界）
- **任务项 7.1.3**：计数键必须带 TTL，过期自动释放
  - *测试思路*：断言 `TTL > 0` 且约等于配置的锁定时长。边界：`TTL == -1`（永不过期）是必须拦截的失败条件
- **任务项 7.1.4**：登录成功后立即清零计数
  - *测试思路*：失败 N-1 次 → 成功登录一次 → 再失败 N-1 次，断言仍未锁定
- **任务项 7.1.5**：锁定期间的正确密码不得重置计数
  - *测试思路*：达到锁定阈值后尝试正确密码，断言被拒且计数未被清零（否则攻击者可无限试探）
- **任务项 7.1.6**：Redis 不可用时的降级策略
  - *测试思路*：模拟 `RedisConnectionFailureException`，断言系统按既定策略运行 —— **「放行」保证可用性，「拒绝」保证安全性，必须择一并在测试中固化**，绝不允许静默吞异常

### 功能上下文 7.2：审计侧 — 锁定事件的可观测性

- **任务项 7.2.1**：触发锁定时应产生可识别的日志/事件
  - *测试思路*：断言锁定发生时有 `status = 0` 的日志写入，且可通过 `login_location` 或独立字段区分「密码错误」与「锁定拒绝」

---

## 功能点 8：登录留痕的完整性（横切一致性）

### 功能上下文 8.1：事件与持久化的整体一致性

- **任务项 8.1.1**：一次 `login()` 调用恰好产生 1 条日志，不多不少
  - *测试思路*：参数化覆盖四条路径（成功 / 密码错误 / 账号不存在 / 账号禁用），每条路径都断言 `tb_login_logs` 行数 `+1`。**这是回归防护的核心断言**，能同时抓住「漏记」（B2）与「重复记」（观察者重复注册）
- **任务项 8.1.2**：日志的 `login_ip` 解析优先级正确
  - *测试思路*：分别只带 `X-Forwarded-For`、只带 `X-Real-IP`、两者都无（回落到 `getRemoteAddr`）三种情况，断言取到预期值。边界：`X-Forwarded-For` 是多段逗号分隔时应取第一段
- **任务项 8.1.3**：`login_location` 的「待解析」占位必须有明确归属
  - *测试思路*：断言该字段是真实解析结果，或明确为 `NULL`。**当前硬编码 `"待解析"` 是伪数据**，会污染后续数据分析 —— 缺陷 B8
- **任务项 8.1.4**：日志写入失败不得影响登录主流程
  - *测试思路*：同 3.2.2，从审计侧再做一次断言

### 功能上下文 8.2：注册链路的顺带风险（相邻回归面）

- **任务项 8.2.1**：`register()` 的 `tb_shop` + `tb_employee` 双写必须原子
  - *测试思路*：让 `employeeMapper.insert` 抛异常，断言 `tb_shop` **不**留下孤儿记录。**当前无 `@Transactional`，此用例必然失败** —— 缺陷 B7

---

## 已知缺陷与风险清单

以下条目已通过阅读代码 + 直接操作 `shop.db` 验证得出，不是推测。建议按此顺序进入修复队列。

| 编号 | 位置 | 缺陷 | 影响 | 对应任务项 |
|---|---|---|---|---|
| **B1** | `EmployeeServiceImpl.login` + `AuditLogObserver` | 账号不存在时发布 `userId = null` 的事件，而 `tb_login_logs.user_id` 为 `NOT NULL`，插库抛 `IntegrityError` | **该路径完全不可用**，前端收到"数据库错误"而非"账号不存在"。已实测复现 | 3.1.1 / 3.2.1 |
| **B2** | `EmployeeServiceImpl.login` | `status != 1` 分支直接 `throw`，**未发布事件** | 被禁账号的登录尝试零留痕，安全审计高危缺口 | 4.2.1 |
| **B3** | Spring 事件默认同步 | 监听器异常沿 `publishEvent` 冒泡，污染业务异常 | 审计逻辑故障会直接让登录接口报错 | 3.2.2 / 8.1.4 |
| **B4** | `new LoginEvent(x, ip, ua, ua, false)` | `loginDevice` 与 `userAgent` 传入同一个值 | `login_device` 列无信息量，设备维度分析失效 | 1.5.5 |
| **B5** | `LoginController` + `Result` | 所有失败统一 `code = 500`，HTTP 状态码恒为 200 | 前端无法区分"账号不存在/密码错误/账号禁用"，只能匹配中文字符串 | 2.3.3 |
| **B6** | `LoginController` | `@Valid` 失败抛 `MethodArgumentNotValidException`，只 `catch (RuntimeException)` 抓不到 | 校验失败返回 Spring 默认错误结构，**与 `Result` 契约不一致** | 5.1.3 |
| **B7** | `EmployeeServiceImpl.register` | 无 `@Transactional`，双写 `tb_shop` + `tb_employee` | 第二步失败会留下孤儿店铺记录 | 8.2.1 |
| **B8** | `AuditLogObserver` | `login_location` 硬编码 `"待解析"` | 伪数据污染后续地域分析 | 8.1.3 |
| **B9** | `gateway` 模块 | 无任何自定义 Filter，无鉴权能力 | 登录态无法维持，受保护接口实际裸奔 | 6.3.x |
| **B10** | `tb_employee` 表 | `username` 无唯一索引 | 并发注册可产生重复账号，`findByUsername` 只取第一条，行为不确定 | 1.4.4 |
| **B11** | `EmployeeServiceImpl.login` | `employee.getStatus() != 1` 对 `Integer` 拆箱 | 库中 `status` 为 `NULL` 时抛 NPE 而非业务异常 | 1.3.4 |
| **B12** | `RedisConfig` | `RedisTemplate` Bean 已定义但**全项目零引用** | 未完成能力 / 死代码，误导后续开发者 | 7.1.x / 6.4.x |

---

## 待决策项（需团队确认后才能收敛任务项）

| 编号 | 决策点 | 默认方案（本次拆解采用） | 备选 |
|---|---|---|---|
| **D1** | 登录凭证方案 | **JWT**：登录签发，网关统一校验。与 Spring Cloud Gateway 架构最匹配，无服务端状态 | Session + Redis（`RedisConfig` 已就绪，需处理网关会话透传）；或本期不实现（仅校验并返回用户信息） |
| **D2** | 安全增强范围 | **失败 5 次锁定 15 分钟**（Redis 计数），不做验证码 | 仅完善审计日志不做限制；或 锁定 + 图形验证码（需前端联调） |
| **D3** | 账号不存在的提示语 | **保留「账号不存在」**（不改变现有行为） | 统一为「用户名或密码错误」以防账号枚举 —— 安全性更好，但牺牲运营排查便利 |
| **D4** | `userId = null` 的日志落库策略 | **观察者跳过落库**（改动最小，不重建表） | 重建 `tb_login_logs` 使 `user_id` 允许 `NULL` 并新增 `username` 列（审计更完整，需迁移脚本） |
| **D5** | `password` 长度上限 | **限制 72 字节**（BCrypt 有效长度） | 不限长（需接受前 72 字节碰撞风险） |

---

## 架构与重构思考

### 组件间关系分析

登录这条链路上，请求实际穿越了 **6 个组件边界**，每一处都藏着耦合点：

```
客户端 → Gateway(无Filter) → LoginController → EmployeeService → EmployeeMapper → SQLite
                    ↓                              ↓
              (鉴权缺失 B9)              ApplicationEventPublisher(同步)
                                                    ↓
                                    AuditLogObserver / UserStatusObserver → SQLite
```

**耦合点 1：Controller 承担了本不该它做的错误翻译。**
`catch (RuntimeException e) → Result.error(e.getMessage())` 把「业务语义异常」和「数据库异常」压成了同一个 `code = 500`。结果是 B1 这类故障对前端完全不可辨。而且这条 `catch` 还会**吞掉真实故障的堆栈**——生产环境排查时只能看到一句中文提示。
→ 重构方向：引入 `ResultCode` 枚举 + `@RestControllerAdvice` 全局异常处理，业务层抛领域异常（如 `LoginFailedException(ErrorCode.PASSWORD_MISMATCH)`），由全局处理器翻译成响应。

**耦合点 2：Service 层依赖 `HttpServletRequest`。**
`EmployeeService.login(LoginDTO, HttpServletRequest)` 把 Web 容器类型泄露进了业务接口。后果有二：业务逻辑无法脱离 Servlet 容器测试（这正是当前没有测试的原因之一）；`getClientIp` 这种 HTTP 关注点混在登录逻辑里。
→ 重构方向：抽出值对象 `LoginContext { username, clientIp, userAgent, device }`，由 Controller 负责组装。Service 接口退化为 `login(LoginContext)`，可纯内存测试。

**耦合点 3：副作用走事件总线，但事件总线是同步的且无隔离。**
这个设计本身是好的（审计与统计解耦），但**没有做故障隔离**：`AuditLogObserver` 里一个 `NOT NULL` 约束违规，就能让整个登录接口报错（B1 的真实成因）。事件驱动如果不能让生产者免于消费者故障，就失去了主要价值。
→ 重构方向：① 观察者内部 try-catch + 上报（快速止血）；② 改 `@TransactionalEventListener(phase = AFTER_COMMIT)`，让审计只在主事务提交后执行，避免"审计失败导致业务回滚"这种更糟的情况；③ 远期可换消息队列。

**耦合点 4：数据一致性没有事务边界。**
`register()` 双写两张表却无 `@Transactional`（B7）；`login()` 的"更新登录统计"是通过观察者在主流程之外做的，也没进同一个事务。
→ 重构方向：`register()` 加 `@Transactional`；`updateLoginInfo` 这类幂等副作用可保留在事件里，但需明确"统计失败不影响登录成功"这一语义并写进测试。

**耦合点 5：两个模块各自独立，「登录态」这条主线是断的。**
`gateway` 不做任何鉴权（B9），`shop` 签发不出凭证。整个系统目前只有"验证密码"这一个动作，没有"维持会话"。
→ 重构方向：先定 D1，再在网关侧加统一鉴权过滤器 + 白名单配置，把"哪些路径免登录"变成配置项而非代码分支。

### 架构对上下文划分的影响

**为什么必须分 Controller / Service / Mapper 三层做上下文？**
不是形式主义，而是三层的**失败模式完全不同**：Mapper 层的典型故障是字段映射错位（下划线 vs 驼峰漏映射会静默变 `null`，不报错）；Service 层的典型故障是分支顺序与边界条件（比如状态校验放在密码校验之前就会泄露账号状态）；Controller 层的典型故障是契约不一致（校验失败走 Spring 默认错误体，B6）。用同一套测试去覆盖这三类问题必然顾此失彼——Mapper 要真库，Service 要 Mock，Controller 要 MockMvc。

**为什么把「事件与观察者」单独切成一个上下文（1.5 / 2.2 / 4.2 / 8.1）？**
这是本项目最容易被漏测的地方，也是缺陷最密集的地方（B1/B2/B3/B8 全在这里）。观察者模式的测试困难和它的优点同源：调用方看不到副作用。如果只测 `login()` 的返回值，`AuditLogObserver` 一行都不会被执行到。因此必须把"副作用的正确性"当作**一等公民**单独拆上下文，测试手段也要换成"捕获事件 + 断言数据库终态"。

**为什么 `gateway` 必须与 `shop` 分开，不能合并成一个上下文？**
技术上它们是两个 JVM 进程、两套依赖、两套启动方式。测试手段毫无重叠：网关侧测路由与过滤器（`WebTestClient` 或过滤器直调，不需要数据库），业务侧测逻辑与持久化（Mockito + SQLite）。混在一起写会得到一个既不敢改、又跑不快的测试集。代价是跨模块的端到端场景（如"带 token 请求受保护接口能拿到数据"）需要一个额外的集成层来兜底——这部分建议放在 `gateway` 模块用 `WebTestClient` 做，而不是在 `shop` 里模拟网关。

**为什么本项目没有「View 层上下文」？**
模板示例里的 `View 层 - 登录界面` 假定的是服务端渲染的 MVC 单体。本项目是纯 REST 后端，前端不在本仓库内。强行套用会产出一批测不到的"界面交互"任务项。因此把这一层等价替换为 **DTO 参数校验边界（5.x）** 与 **响应契约（2.3 / 3.3）**——这才是这个架构里真正存在、且真正会出问题的那一层。

### 测试通过后的重构方向

按「投入产出比 × 风险」排序：

1. **抽出 `PasswordEncoder` 抽象，隔离 BCrypt**（低风险 / 高收益）
   现在 `BCrypt` 是静态调用，无法替换也无法验证"是否真的用了哈希"。抽成接口后，测试可用 `NoOpEncoder` 换取速度，生产保留 BCrypt，也能顺便落实 D5 的长度策略。
2. **引入 `LoginContext` 替换 `HttpServletRequest` 参数**（低风险 / 中收益）
   这是让 Service 层可测的前提。做完之后 1.3 / 2.1 / 3.1 / 4.1 这批任务项都可以退化成纯内存测试，不再依赖 Spring 上下文。
3. **引入 `ResultCode` 枚举 + `@RestControllerAdvice`**（中风险 / 高收益）
   一次性解决 B5 与 B6。改动面在 Controller 层，回归风险可控，且有 2.3 / 5.1 的任务项保护。
4. **审计改为 `@TransactionalEventListener(AFTER_COMMIT)` + 观察者内部兜底**（中风险 / 高收益）
   这是修复 B1/B3 的根因方案。需要先有 8.1.1 那条"恰好 1 条日志"的断言作为安全网。
5. **`register()` 补 `@Transactional`**（低风险 / 中收益）
   独立改动，风险极低，建议随手做掉（B7）。
6. **数据库层加固**：`tb_employee.username` 加唯一索引；按 D4 决定 `tb_login_logs` 是否重建（B10 / B1）
   注意 SQLite **不支持 `ALTER COLUMN`**，修改 `NOT NULL` 约束需要「建新表 → 迁数据 → 改名」三步；而 `ADD COLUMN` 是支持的。这是 D4 选方案时必须考虑的成本。
7. **引入 Redis 承载登录风控与凭证**（高风险 / 视 D1/D2 而定）
   这是唯一会引入新中间件依赖的改动（虽然 Redis 连接已经在配置里了）。建议放在最后，且必须先确认 Redis 不可用时的降级策略（7.1.6），否则会把"登录不可用"的风险引入系统。

---

## 附：TDD 执行顺序建议

严格遵循 **先写失败测试 → 再实现 → 最后重构**，并按「修复现存缺陷」优先于「新增能力」排序：

| 阶段 | 范围 | 说明 |
|---|---|---|
| **阶段 0** | 测试脚手架 | 引入 `spring-boot-starter-test`，建立内存 SQLite 测试基座。**已完成，见本次交付** |
| **阶段 1** | 功能点 1 / 2 / 5 | 已能工作的路径先补上"安全网"，防止后续重构破坏现有行为 |
| **阶段 2** | 功能点 3 / 4 / 8 | 修复 B1 / B2 / B3 / B6 / B7 —— **这一阶段是真正的价值所在** |
| **阶段 3** | 功能点 6 | 凭证签发与网关校验（依赖 D1 决策） |
| **阶段 4** | 功能点 7 | 失败锁定（依赖 D2 决策） |
| **阶段 5** | 重构 | 执行上文"重构方向" 1→6 |

阶段 2 之所以优先级最高，是因为它修的是**当前已经无法正常工作的功能**（账号不存在路径）和**存在安全缺口的逻辑**（禁用账号零留痕）。

---

## 附：测试骨架说明

本次已交付一套**可直接运行**的测试骨架，作为「阶段 0」的产出。

### 依赖改动

| 文件 | 改动 |
|---|---|
| `pom.xml`（根） | `dependencyManagement` 中新增 `spring-boot-starter-test`（version 取自 `${spring-boot.version}`，scope `test`） |
| `shop/pom.xml` | 新增 `spring-boot-starter-test` 依赖；**删除了重复声明的 `lombok`**（原先第 61 行附近重复了一次，Maven 会报 `must be unique` 警告） |
| `gateway/pom.xml` | 新增 `spring-boot-starter-test` 依赖 |

### 新增文件

```
shop/src/test/java/com/lxs/b2cmall/shop/
├── support/LoginFixtures.java              # 造数据 + 反射注入测试替身
├── support/InMemorySqlite.java             # 内存 SQLite 基座，供落库类任务项使用
├── service/EmployeeServiceImplLoginTest.java   # 功能点 1~4，Mockito 单测
└── controller/LoginControllerTest.java     # 功能点 1.2 / 2.3 / 3.3 / 5.1，MockMvc 契约测试

shop/src/test/resources/
├── schema-test.sql          # 测试表结构（刻意保留 user_id NOT NULL，用于复现缺陷 B1）
├── application-test.yml     # @ActiveProfiles("test") 用
└── bootstrap-test.yml       # 在该 profile 下关闭 Nacos

gateway/src/test/java/com/lxs/b2cmall/gateway/GatewayAuthSkeletonTest.java
```

### 骨架的两种测试风格

- **单测（已启用，绿）**：纯 Mockito，不启动 Spring 上下文，不碰 Nacos / Redis / SQLite。覆盖当前**已经能正常工作**的路径，作为后续重构的安全网。
- **占位（`@Disabled`，红→待转绿）**：每个占位方法都对应一个明确的缺陷编号或任务项编号，方法体里写清了实现后该如何断言的 TODO。这批就是 TDD 的"红灯"队列。

### 运行方式（重要）

```bash
mvn -B test
```

**必须用 JDK 8 执行**，原因：项目依赖 `lombok 1.18.8`，它在 JDK 9+ 下访问 `com.sun.tools.javac.processing` 会被模块系统拦截，报：

```
java.lang.IllegalAccessError: class lombok.javac.apt.LombokProcessor ...
cannot access class com.sun.tools.javac.processing.JavacProcessingEnvironment
(in module jdk.compiler) because module jdk.compiler does not
export com.sun.tools.javac.processing to unnamed module
```

本机 `JAVA_HOME` 当前指向 `D:\java17`，而项目 target 是 Java 8，所以从命令行构建需要显式切换：

```bash
JAVA_HOME="C:/Program Files/Java/jdk1.8.0_261" mvn -B test
```

若后续要在 JDK 17 下构建，需要把 `lombok` 升到 `1.18.30+`（属独立改动，未包含在本次交付内）。

### 当前基线

| 模块 | 执行 | 跳过（占位） | 失败 |
|---|---|---|---|
| `gateway` | 4 | 4 | 0 |
| `shop` | 24 | 7 | 0 |
| **合计** | **28** | **11** | **0** |

基线为全绿，意味着骨架本身可用，且这批测试**不会**掩盖现存缺陷——因为所有会因缺陷而失败的断言都被放进了 `@Disabled` 占位里，等对应任务项动手时逐个启用即可看到红灯。

