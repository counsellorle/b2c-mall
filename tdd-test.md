# B2C 商城 · 登录模块 TDD 测试任务书

> 目标项目：`b2c_mall`（Spring Cloud 微服务）
> 覆盖范围：**登录**（功能点 1–8）
> 生成日期：2026-10-13（2026-10-16 收窄为仅登录）
> 配套文档：`.tdd/task/login-tdd-tasks.md`（登录专项详细拆解）
> 测试代码：`shop/src/test`
>
> **本期实施范围（2026-10-16 确认）：只做「登录」的 JUnit 测试**。
> 注册侧测试与文档章节已移除（功能点 9–11、缺陷 B13–B21、决策项 D6）；
> 功能点 6（凭证签发）、功能点 7（失败锁定）、网关（功能点 1.1 / 6.3）**本期不实现**，
> 但保留章节作为后续参考。

---

## 0. 怎么用这份文档

1. 按「§13 执行阶段」从上往下推进，**不要跳阶段**。阶段 2 是价值最高的一段（修的是当前已经坏掉的功能）。
2. 每个任务项都是 `- [ ]` 复选框（见 §13）：动手时改成 `- [x]`，并补一句实测结果。
3. 状态列含义：

| 标记 | 含义 |
|---|---|
| ✅ 绿 | 对应测试已存在且通过，是重构安全网 |
| ⏸ 红 | 测试已写成 `@Disabled` 占位，实现后启用即可看到红灯→转绿 |
| ❌ 无 | 还没有测试代码，需要先写 |

4. 严格 TDD 节奏：**先写/启用测试 → 跑出红灯 → 改生产代码 → 转绿 → 提交**。一次只动一个任务项。
5. 所有会因现存缺陷而失败的断言，都被放在 `@Disabled` 占位里 —— **基线全绿不等于没有缺陷**。

---

## 1. 架构事实快照

拆解「功能上下文」之前必须先固定架构事实，否则上下文划分就是臆测。

| 维度 | 事实 |
|---|---|
| 架构模式 | Spring Cloud 微服务（非 MVC 单体），Spring Boot 2.7.18 / Spring Cloud 2021.0.5 / Nacos 2021.0.4.0 |
| 模块 | `gateway`（8081，Spring Cloud Gateway + LoadBalancer + Nacos 发现）、`shop`（8082，业务服务） |
| `shop` 内部分层 | Controller → Service(接口 + impl) → MyBatis **注解式** Mapper → Entity / DTO / VO |
| 横切机制 | Spring 事件 + 观察者模式：`LoginEvent` → `AuditLogObserver`（写审计日志）、`UserStatusObserver`（更新登录统计） |
| 持久化 | SQLite，`jdbc:sqlite:F:/ssd/b2c_mall/shop/shop.db?foreign_keys=on`（**`foreign_keys=on` 是打开的**，约束会真实生效） |
| 缓存 | Redis（阿里云 `47.100.22.158:6379`），`RedisConfig` 已定义 `RedisTemplate` Bean，但**全项目零引用** |
| 配置来源 | `bootstrap.yml` + `application.yml`，另有 Nacos 远程配置（`namespace: zhudole`）可能覆盖本地 |
| View 层 | **不存在**。纯 REST 后端，因此「View 层上下文」的等价物是 **DTO 参数校验边界 + 响应契约** |
| 网关自定义逻辑 | `GatewayApplication` 仅有 `@EnableDiscoveryClient`，**无任何自定义 Filter**；路由规则疑似由 Nacos 远程配置下发 |

**登录校验链（现状代码，改动必须在任务项层面显式声明）：**

```
取客户端 IP / UA → 按 username 查库 → BCrypt 校验密码 → status == 1 判定启用 → 清空返回密码 → 发布登录事件
```

---

## 2. 现状基线

### 2.1 已交付的测试骨架

```
shop/src/test/java/com/lxs/b2cmall/shop/
├── support/LoginFixtures.java                   # 造数据 + 反射注入测试替身
├── support/InMemorySqlite.java                  # 内存 SQLite 基座，供落库类任务项使用
├── support/SqliteMapperSupport.java             # 把 MyBatis Mapper 挂到内存 SQLite（真库断言用）
├── service/EmployeeServiceImplLoginTest.java    # 功能点 1~4、1.5/8，Mockito 单测
├── controller/LoginControllerTest.java          # 功能点 1.2 / 2.3 / 3.3 / 5.1，MockMvc 契约测试
├── mapper/EmployeeMapperTest.java               # 功能点 1.4，真库映射与约束
└── observer/LoginObserverSideEffectTest.java    # 功能点 1.5 / 2.2 / 4.2 / 8.1，事件→数据库终态

shop/src/test/resources/
├── schema-test.sql          # 测试表结构（刻意保留 user_id NOT NULL，用于复现缺陷 B1）
├── application-test.yml     # @ActiveProfiles("test") 用
└── bootstrap-test.yml       # 在该 profile 下关闭 Nacos
```

> 网关侧占位测试 `GatewayAuthSkeletonTest`（2026-10-16 移除）、注册侧测试
> `EmployeeServiceImplRegisterTest` / `RegisterControllerTest`（2026-10-16 按「只保留登录」移除）。

### 2.2 当前基线数字

| 模块 | 执行（含跳过） | 跳过（占位） | 实际跑绿 | 失败 |
|---|---|---|---|---|
| `shop` | 38 | 12 | 26 | 0 |
| **合计** | **38** | **12** | **26** | **0** |

### 2.3 代码层测试方法清单（38 个）

**`EmployeeServiceImplLoginTest`（17 个：14 绿 / 3 红）**

| 分区 | 方法 | 状态 |
|---|---|---|
| 密码正确 | clearsPasswordAndReturnsUser | ✅ |
| 密码正确 | queriesRepositoryExactlyOnce | ✅ |
| 密码正确 | publishesSuccessEvent | ✅ |
| 密码正确 | statusOneIsEnabled | ✅ |
| 密码错误 | throwsPasswordMismatch | ✅ |
| 密码错误 | publishesFailureEventWithRealUserId | ✅ |
| 密码错误 | doesNotTouchLoginStatsOnWrongPassword | ✅ |
| 密码错误 | trailingSpaceIsRejected | ✅ |
| 账号不存在 | throwsAccountNotFound | ✅ |
| 账号不存在 | publishesFailureEventWithNullUserId | ✅ |
| 账号被禁用 | throwsAccountDisabled | ✅ |
| 账号被禁用 | passwordCheckedBeforeStatus | ✅ |
| 账号被禁用 | doesNotTouchLoginStatsOnDisabledAccount | ✅ |
| 账号被禁用 | nullStatusIsNotNpe | ⏸ B11 |
| 账号被禁用 | disabledAttemptMustBeAudited | ⏸ B2 |
| 事件内容 | deviceShouldNotDuplicateUserAgent | ⏸ B4 |
| 事件内容 | carriesClientIp | ✅ |

**`LoginControllerTest`（10 个：4 绿 / 6 红）**

| 方法 | 状态 |
|---|---|
| loginReturnsSuccessResult | ✅ |
| wrongPasswordReturnsErrorResult | ✅ |
| accountNotFoundReturnsErrorResult | ✅ |
| errorMessageHasNoInternalDetail | ✅ |
| blankUsernameReturnsResultShape | ⏸ B6 |
| responseMustNotCarryPassword | ⏸ |
| failureCodesAreDistinguishable | ⏸ B5 |
| blankUsernameIsRejectedWithMessage | ⏸ B6 |
| blankPasswordIsRejectedWithMessage | ⏸ B6 |
| emptyBodyStillReturnsResultShape | ⏸ B6 |

**`EmployeeMapperTest`（6 个：5 绿 / 1 红）**：`mapsUnderscoreColumnsToCamelCase`✅、`returnsNullForUnknownUsername`✅、`passwordColumnHoldsBcryptHash`✅、`updateLoginInfoIncrementsCountAndStampsTime`✅、`duplicateUsernameCurrentlyAllowedAndBreaksLookup`✅、`usernameMustBeUnique`⏸B10

**`LoginObserverSideEffectTest`（5 个：3 绿 / 2 红）**：`successfulLoginUpdatesStatsAndWritesOneLog`✅、`wrongPasswordWritesSingleFailureLogWithRealUserId`✅、`loginLocationIsCurrentlyHardcoded`✅、`disabledAccountAttemptMustBeAudited`⏸B2、`accountNotFoundSurvivesRealListener`⏸B1

### 2.4 尚未落地的测试（只有思路，无代码）

`1.2.2` 委托调用断言、`1.3.5` 副作用范围锁定、`1.5.2` 观察者重复注册防护、`1.5.5` 设备信息、`5.1.5` 密码长度上限、`6.x` 全部（凭证）、`7.x` 全部（失败锁定）、`8.1.2` IP 解析优先级、`8.1.4` 审计失败隔离。

---

## 3. 功能点 1：输入正确的用户名和密码可以登录成功

### 功能上下文 1.1：Gateway 层 — 登录请求放行与转发

| 编号 | 任务项 | 测试思路（断言 + 边界） | 状态 |
|---|---|---|---|
| 1.1.1 | `/employee/login` 必须在鉴权白名单内 | 无 `Authorization` 头 `POST /employee/login`，断言未返回 401/403 而是转发；**边界**：白名单外接口同样条件必须 401 | ⏸ |
| 1.1.2 | 转发必须原样保留 body、`X-Forwarded-For`、`User-Agent` | 断言下游收到的 body 与客户端 JSON 字节一致，两个头值相同。**这三个字段决定审计日志写的是真实 IP 还是网关内网 IP** | ⏸ |
| 1.1.3 | 登录响应必须原样透传 | 断言经网关的响应体与直连 `shop` 的结构完全一致（`code`/`message`/`data` 不变形） | ⏸ |

### 功能上下文 1.2：Controller 层 — `LoginController.login`

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 1.2.1 | `POST /employee/login` 绑定 `LoginDTO` | MockMvc 发 `{"username":"shop01","password":"123456"}`，断言 200 且 `code == 200` | ✅ |
| 1.2.2 | 原样委托给 `EmployeeService.login(dto, request)` | `verify(employeeService, times(1)).login(same(dto), any())`，未被重建 | ❌ |
| 1.2.3 | Service 返回时用 `Result.success` 包装 | 断言 `data.id` / `data.username` 正确、`message == "success"` | ✅ |
| 1.2.4 | 响应体绝不含 `password` | 断言响应 JSON 无 `data.password` 键或为 `null`；**边界**：任何响应（含异常响应）都不得回显密码或 hash | ⏸ |

### 功能上下文 1.3：Service 层 — 校验链

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 1.3.1 | 按 `username` 精确查询 | `verify(employeeMapper).findByUsername("shop01")` 恰好 1 次 | ✅ |
| 1.3.2 | `BCrypt.checkpw` 校验密码 | 存 `BCrypt.hashpw`，断言 `"123456"` 通过；**边界**：尾随空格、大小写变形、空串、>72 字节（BCrypt 静默截断）均应失败 | ⏸ 部分 |
| 1.3.3 | 校验通过后密码置空 | 断言 `password == null` 且 `id`/`username`/`shopId` 未被清掉 | ✅ |
| 1.3.4 | `status == 1` 判定启用 | 参数化 `1` 通过；`0`/`2`/`null` 拒绝。**注意 `status` 是 `Integer`，`null` 会命中拆箱 NPE（B11）** | ⏸ B11 |
| 1.3.5 | 返回的是持久化实体直接引用（就地改状态） | 断言 `findByUsername` 返回对象在 `login` 后被改写。作用是**锁定副作用范围**，重构为不可变 VO 时此测试必须显式修改 | ❌ |

### 功能上下文 1.4：数据层 — `EmployeeMapper` 与 SQLite

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 1.4.1 | 下划线→驼峰映射正确 | 插入 `shop_id = 7`，断言 `getShopId() == 7`（`@Results` 漏字段会静默为 `null`） | ✅ |
| 1.4.2 | 用户名不存在返回 `null` 不抛异常 | 查 `"__not_exist__"`，断言 `null` 且无异常 | ✅ |
| 1.4.3 | 密码列存 BCrypt hash，长度 ≤ 255 | 断言以 `$2a$10$` 开头、长度 60，绝不等于明文 | ✅ |
| 1.4.4 | `username` 唯一性约束 | 连插两条同名，第二条必须失败。**实测：两条都插进去了，且 `findByUsername` 会直接抛 `TooManyResultsException`** —— 重复账号会让该用户名的登录彻底报错（B10） | ⏸ B10 |

### 功能上下文 1.5：事件与观察者 — 登录成功的副作用

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 1.5.1 | 发布 `success = true` 的事件且带真实 `userId` | `ArgumentCaptor<LoginEvent>` 捕获，断言 `isSuccess()` 与 `getUserId()` | ✅ |
| 1.5.2 | `UserStatusObserver` 更新 `login_count` / `last_login_time` | 连登两次，计数 +2、时间单调不减。**边界：必须 +1/+2 而不是翻倍**（观察者重复注册会翻倍） | ✅ |
| 1.5.3 | `AuditLogObserver` 写一条 `status = 1` 的日志 | 断言 `tb_login_logs` +1，`user_id` 正确、`login_time` 非空 | ✅ |
| 1.5.4 | 登录不得触发注册链路副作用 | 断言 `tb_messages` 行数不变（跨事件串扰是最典型的观察者回归缺陷）。**依赖注册链路，本期不做** | ❌ |
| 1.5.5 | `loginDevice` 应是真实设备信息 | 断言 `getLoginDevice() != getUserAgent()`。**当前传同一个值，必然失败（B4）** | ⏸ B4 |

---

## 4. 功能点 2：输入错误的密码不能登录

### 功能上下文 2.1：Service 层 — 密码不匹配的处理

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 2.1.1 | 抛出 `RuntimeException`，消息「密码错误」 | `assertThrows` + `getMessage()` 精确等于 `"密码错误"`；**边界**：不能是 NPE 等其他类型 | ✅ |
| 2.1.2 | 不得改写 `login_count` / `last_login_time` | `verify(employeeMapper, never()).updateLoginInfo(anyInt())` + 数据库真实行断言 | ✅ |
| 2.1.3 | 仍发布 `success = false` 且 `userId` 为**真实 id** | 断言 `userId != null`。**意义**：能定位到账号才能做该账号的失败锁定；`null` 会让风控失效 | ✅ |

### 功能上下文 2.2：审计侧 — 失败尝试的留痕

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 2.2.1 | 写一条 `status = 0` 的失败日志 | 断言 `tb_login_logs` +1 且 `status == 0` | ✅ |
| 2.2.2 | 失败日志 `user_id` 必须为真实 id | 断言非空且外键存在。**为 `null` 时因 NOT NULL + `foreign_keys=on` 会直接抛 `IntegrityError`** | ✅ |
| 2.2.3 | 日志只写一行不重复 | 断言行数恰好 +1（多次 publish 或观察者注册两次会多写） | ✅ |

### 功能上下文 2.3：Controller 层 — 失败响应契约

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 2.3.1 | 返回 `Result.error`，`message`「密码错误」 | 断言 `code != 200` 且 `message == "密码错误"` | ✅ |
| 2.3.2 | 错误响应不泄露内部细节 | 断言 `message` 不含 `SQL`/`sqlite`/`Exception`/`$2a$` | ✅ |
| 2.3.3 | HTTP 状态码与业务码的语义约定 | 明确断言失败是 `200 + code=500` 还是 401/400。**当前恒 200 + code=500，是契约决策点（B5）** | ⏸ B5 |

---

## 5. 功能点 3：账号不存在的登录尝试产生正确提示与留痕

> 本功能点包含一个**已确认的现存缺陷**，是本轮 TDD 最该优先修复的路径。

### 功能上下文 3.1：Service 层 — 用户名查不到时的分支

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 3.1.1 | 查不到时抛「账号不存在」 | 异常消息**精确等于** `"账号不存在"`。**当前会失败**：观察者拿 `userId = null` 插库抛 `NOT NULL constraint failed: tb_login_logs.user_id`（B1，已实测复现） | ✅（Mock 层面）/ ⏸（真实监听器） |
| 3.1.2 | 账号不存在时仍发布事件（`userId` 允许 `null`） | 断言事件被发布且 `isSuccess() == false`。作用是**锁住"要审计"这个意图**，如何落库由 3.2 决定 | ✅ |

### 功能上下文 3.2：审计侧 — 无主账号的日志落库策略

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 3.2.1 | `userId == null` 不得导致主流程中断 | 断言抛的是业务语义异常（`"账号不存在"`）而非 `SQLiteException`；等价断言：异常消息不含 `constraint`。**修复方向见 D4** | ⏸ B1 |
| 3.2.2 | 任何监听器异常不得污染业务异常 | 让 `loginLogMapper.insert` 抛异常，断言 `login()` 仍返回业务语义结果。**当前不满足**：Spring 事件默认**同步**，监听器异常沿 `publishEvent` 冒泡（B3） | ⏸ B3 |
| — | 修复方向 | ① 观察者内 try-catch 兜底；② `@TransactionalEventListener(phase = AFTER_COMMIT)` + 异步 | — |

### 功能上下文 3.3：Controller 层 — 提示语是否防账号枚举

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 3.3.1 | 账号不存在的对外提示语 | 断言 `message` 为既定文案。**两个互斥方案必须择一固化**：保留「账号不存在」（运营友好）vs 统一「用户名或密码错误」（防枚举）—— 见 D3 | ✅（按当前行为） |

---

## 6. 功能点 4：账号被禁用时不能登录

### 功能上下文 4.1：Service 层 — 状态校验

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 4.1.1 | `status != 1` 抛「账号已被禁用」 | 参数化 `0 / 2 / -1`，异常消息一致 | ✅ |
| 4.1.2 | 状态校验必须晚于密码校验 | 给定 `status = 0` 且密码错误，断言抛「密码错误」而非「账号已被禁用」。**防止攻击者用任意密码探测账号是否被禁用（状态泄露）** | ✅ |
| 4.1.3 | 禁用账号失败不得更新登录统计 | `verify(never()).updateLoginInfo` + 数据库行未变 | ✅ |

### 功能上下文 4.2：审计侧 — 禁用账号的留痕缺口

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 4.2.1 | 禁用账号的登录尝试应写一条失败日志 | **当前必然失败**：`status != 1` 分支直接 `throw`，**没有 `publishEvent`**（B2）。漏记在安全审计上属高危（可持续探测被禁账号而不留痕） | ⏸ B2 |

---

## 7. 功能点 5：入参校验（DTO 边界与响应契约）

### 功能上下文 5.1：Controller 层 — `@Valid` 校验边界

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 5.1.1 | `username` 空/空白 → 拒绝「账号不能为空」 | 发 `{"password":"x"}`、`{"username":""}`、`{"username":"   "}` 三者都应被拒。**边界**：`@NotBlank` 对全空白生效，`@NotNull` 不会 | ⏸ B6 |
| 5.1.2 | `password` 空 → 拒绝「密码不能为空」 | 发 `{"username":"shop01"}`，断言被拒且文案为密码相关 | ⏸ B6 |
| 5.1.3 | 校验失败响应结构与其他错误一致 | 断言 JSON 含 `code`/`message`/`data`。**当前必然失败**：`@Valid` 抛 `MethodArgumentNotValidException`，而 Controller 只 `catch (RuntimeException)`，返回的是 Spring 默认错误体（B6） | ⏸ B6 |
| 5.1.4 | 请求体非法（非 JSON / 空 body）的响应契约 | 发 `Content-Type: application/json` 但空 body，断言结构一致而非 Spring 默认 400 页 | ⏸ B6 |
| 5.1.5 | `password` 长度上限策略 | 构造 >72 字节密码，断言超长被拒或明确接受。**BCrypt 只取前 72 字节，静默截断会让「前 72 字节相同的两个密码」互相可登录** —— 见 D5 | ⏸ |

---

## 8. 功能点 6：登录成功后维持登录状态（凭证签发与校验）

> 现状：登录成功仅返回 `Employee`，**不签发任何凭证**。属新增能力，方案未定（D1）。以下按**默认方案 JWT + 网关校验**拆解；若改 Session + Redis，`6.1.1` / `6.4.x` 需替换。

### 功能上下文 6.1：Service 层 — 签发令牌

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 6.1.1 | 密码通过后签发 JWT（含 `userId`/`shopId` + 过期时间） | 断言 token 可解析、字段一致、`exp - iat` 等于配置有效期 | ❌ |
| 6.1.2 | token 不含密码/hash | 解析 payload 断言无 `password`，base64 解码后不出现 `$2a$` | ❌ |
| 6.1.3 | 签发密钥来自配置不硬编码 | 断言来自 `@Value`/`@ConfigurationProperties`，测试环境可覆盖 | ❌ |

### 功能上下文 6.2：Controller 层 — 凭证下发

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 6.2.1 | 成功响应含 token 字段 | 断言 `data.token` 为三段式 JWT | ❌ |
| 6.2.2 | 下发方式约定（body vs `Set-Cookie`） | 二选一固化；若 Cookie 断言 `HttpOnly`/`Secure`/`SameSite` | ❌ |

### 功能上下文 6.3：Gateway 层 — 凭证校验

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 6.3.1 | 受保护接口无 token → 401 | 断言 401 且**未转发**给下游 | ⏸ |
| 6.3.2 | token 非法/签名错/过期 → 401 | 三组参数化：篡改签名、篡改 payload、`exp` 过去。**边界**：过期与签名错宜返回可区分错误码 | ⏸ |
| 6.3.3 | 合法 token → 解析 `userId` 并透传 | 断言下游 `X-User-Id` 与 token 一致，且**不信任**客户端自带的同名头 | ⏸ |
| 6.3.4 | 白名单与受保护路径判定互斥无遗漏 | 遍历白名单断言放行、受保护路径断言拦截。**边界**：防 `/employee/login/../orders` 绕过 | ⏸ |

### 功能上下文 6.4：Redis 层 — 凭证生命周期（仅 Session / 黑名单方案需要）

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 6.4.1 | `RedisTemplate` 序列化能正确读写会话对象 | 写入再读回断言字段不丢。**注意 `RedisConfig` 启用了 `activateDefaultTyping`，断言不要依赖具体类名** | ❌ |
| 6.4.2 | 登出/强制失效后旧凭证不可用 | 登出后带原 token 请求受保护接口，断言 401 | ❌ |

---

## 9. 功能点 7：登录失败次数限制与账号临时锁定

> 现状：**完全没有**任何限流或锁定；Redis 虽已配置但零引用。属新增能力（D2）。

### 功能上下文 7.1：Redis 层 — 失败计数器

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 7.1.1 | 同一 `username` 连续失败 N 次后锁定 | 第 N 次应返回「账号已被锁定」；第 N+1 次即使**密码正确**也拒绝 | ❌ |
| 7.1.2 | 计数粒度与键设计 | 断言 key 形如 `login:fail:{username}`；不同账号计数互不影响 | ❌ |
| 7.1.3 | 计数键必须带 TTL | 断言 `TTL > 0` 且约等于锁定时长。**边界：`TTL == -1`（永不过期）必须拦截** | ❌ |
| 7.1.4 | 登录成功后立即清零 | 失败 N-1 次 → 成功一次 → 再失败 N-1 次，仍未锁定 | ❌ |
| 7.1.5 | 锁定期间的正确密码不得重置计数 | 达阈值后试正确密码，断言被拒且计数未清零 | ❌ |
| 7.1.6 | Redis 不可用时的降级策略 | 模拟 `RedisConnectionFailureException`，**「放行」保可用 /「拒绝」保安全必须择一并固化**，绝不允许静默吞异常 | ❌ |

### 功能上下文 7.2：审计侧 — 锁定事件的可观测性

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 7.2.1 | 锁定事件可观测 | 断言锁定时有 `status = 0` 日志，且可区分「密码错误」与「锁定拒绝」 | ❌ |

---

## 10. 功能点 8：登录留痕的完整性（横切一致性）

### 功能上下文 8.1：事件与持久化的整体一致性

| 编号 | 任务项 | 测试思路 | 状态 |
|---|---|---|---|
| 8.1.1 | 一次 `login()` 恰好产生 1 条日志 | 参数化四条路径（成功/密码错/账号不存在/禁用），各断言 `tb_login_logs` 恰好 +1。**回归防护核心断言**，同时抓「漏记」（B2）与「重复记」 | ✅ 成功/密码错路径；⏸ 账号不存在路径（B1） |
| 8.1.2 | `login_ip` 解析优先级 | 只带 `X-Forwarded-For` / 只带 `X-Real-IP` / 都没有（回落 `getRemoteAddr`）三种情况。**边界**：XFF 多段逗号分隔应取第一段 | ❌ |
| 8.1.3 | `login_location` 的占位归属 | 断言是真实解析结果或明确 `NULL`。**当前硬编码 `"待解析"` 是伪数据，会污染地域分析（B8）** | ✅ 现状证据 |
| 8.1.4 | 日志写入失败不影响登录主流程 | 同 3.2.2，从审计侧再断言一次 | ❌ |

---

## 11. 缺陷与风险清单（12 条）

以下条目**已通过阅读代码 + 直接操作 `shop.db` 验证得出**，不是推测。建议按编号顺序进入修复队列。

### 11.1 缺陷清单（B1–B12，编号沿用历史）

| 编号 | 位置 | 缺陷 | 影响 | 对应任务项 |
|---|---|---|---|---|
| **B1** | `EmployeeServiceImpl.login` + `AuditLogObserver` | 账号不存在时发布 `userId = null` 的事件，而 `tb_login_logs.user_id` 为 `NOT NULL`，插库抛 `IntegrityError` | **该路径完全不可用**，前端收到"数据库错误"而非"账号不存在"。已实测复现 | 3.1.1 / 3.2.1 |
| **B2** | `EmployeeServiceImpl.login` | `status != 1` 分支直接 `throw`，**未发布事件** | 被禁账号登录尝试零留痕，安全审计高危缺口 | 4.2.1 |
| **B3** | Spring 事件默认同步 | 监听器异常沿 `publishEvent` 冒泡，污染业务异常 | 审计逻辑故障会直接让登录接口报错 | 3.2.2 / 8.1.4 |
| **B4** | `new LoginEvent(x, ip, ua, ua, false)` | `loginDevice` 与 `userAgent` 传同一个值 | `login_device` 无信息量，设备维度分析失效 | 1.5.5 |
| **B5** | `LoginController` + `Result` | 所有失败统一 `code = 500`，HTTP 恒 200 | 前端无法区分三类失败，只能匹配中文字符串 | 2.3.3 |
| **B6** | `LoginController` | `@Valid` 失败抛 `MethodArgumentNotValidException`，只 `catch (RuntimeException)` 抓不到 | 校验失败返回 Spring 默认错误结构，**与 `Result` 契约不一致** | 5.1.3 |
| **B7** | `EmployeeServiceImpl.register` | 无 `@Transactional`，双写 `tb_shop` + `tb_employee` | 第二步失败留下孤儿店铺记录（注册侧，本期不做，编号保留） | — |
| **B8** | `AuditLogObserver` | `login_location` 硬编码 `"待解析"` | 伪数据污染地域分析 | 8.1.3 |
| **B9** | `gateway` 模块 | 无任何自定义 Filter，无鉴权能力 | 登录态无法维持，受保护接口实际裸奔（网关侧，本期不做） | — |
| **B10** | `tb_employee` 表 | `username` 无唯一索引 | 一旦出现重名账号，`findByUsername` 直接抛 `TooManyResultsException`，该用户名彻底无法登录（已实测） | 1.4.4 |
| **B11** | `EmployeeServiceImpl.login` | `employee.getStatus() != 1` 对 `Integer` 拆箱 | 库中 `status` 为 `NULL` 时抛 NPE 而非业务异常 | 1.3.4 |
| **B12** | `RedisConfig` | `RedisTemplate` Bean 已定义但**全项目零引用** | 未完成能力 / 死代码（本期不做） | — |

> B7 / B9 / B12 分属注册、网关、Redis 范围，本期不做；编号保留，便于与既有测试注释中的引用对应。

---

## 12. 待决策项（需团队确认后才能收敛任务项）

| 编号 | 决策点 | 默认方案（本文档采用） | 备选 |
|---|---|---|---|
| **D1** | 登录凭证方案 | **JWT**：登录签发、网关统一校验，无服务端状态 | Session + Redis（`RedisConfig` 已就绪，需处理网关会话透传）；或本期不实现 |
| **D2** | 安全增强范围 | **失败 5 次锁定 15 分钟**（Redis 计数），不做验证码 | 仅完善审计日志；或 锁定 + 图形验证码（需前端联调） |
| **D3** | 账号不存在的提示语 | **保留「账号不存在」** | 统一为「用户名或密码错误」防账号枚举 |
| **D4** | `userId = null` 的日志落库策略 | **观察者跳过落库**（改动最小，不重建表） | 重建 `tb_login_logs` 使 `user_id` 允许 `NULL` 并新增 `username` 列 |
| **D5** | `password` 长度上限 | **限制 72 字节**（BCrypt 有效长度） | 不限长（需接受前 72 字节碰撞风险） |

---

## 13. 执行阶段（可勾选）

严格遵循 **先写失败测试 → 再实现 → 最后重构**，并按「修复现存缺陷」优先于「新增能力」排序。

### 阶段 0：测试脚手架 ✅ 已完成

- [x] 引入 `spring-boot-starter-test`（根 `pom.xml` 的 `dependencyManagement` + 两模块声明）
- [x] 建立内存 SQLite 测试基座（`InMemorySqlite` + `schema-test.sql`）
- [x] 登录侧首批 28 个测试方法（17 绿 / 11 占位）
- [x] 清理 `shop/pom.xml` 中重复声明的 `lombok`（Maven `must be unique` 警告已消失）
- **验收**：当时 `mvn -B test` 输出 28 执行 / 11 跳过 / 0 失败 → **收窄为仅登录后为 38 执行 / 12 跳过 / 0 失败**（见 §2.2）

### 阶段 1：给已能工作的路径补安全网（功能点 1 / 2 / 5）✅ 已完成

- [x] 1.4.1 / 1.4.2 / 1.4.3 Mapper 真库测试 → `EmployeeMapperTest`（新增 `SqliteMapperSupport` 基座）
- [x] 1.5.2 / 1.5.3 / 1.5.4 观察者副作用测试 → `LoginObserverSideEffectTest`
- [x] 2.2.1 / 2.2.2 / 2.2.3 失败日志落库 → 同上
- [x] 2.3.2 错误响应不泄露细节 → `LoginControllerTest#errorMessageHasNoInternalDetail`
- [x] 5.1.1 / 5.1.2 / 5.1.4 入参校验文案 → 写成 `@Disabled` 占位（依赖 B6 的全局异常处理）
- [ ] 1.2.2 委托调用断言（待补：需要 Mockito `same(dto)` 捕获）
- [ ] 1.3.5 副作用范围锁定（待补：断言 `findByUsername` 返回对象被就地改写）
- **验收**：全部绿灯，且**不修改任何生产代码** —— 已达成，26 个用例实际跑绿、0 失败

### 阶段 2：修复现存缺陷（功能点 3 / 4 / 8）★ 价值最高

红灯已就位，直接按顺序启用即可：

- [ ] 启用 `LoginObserverSideEffectTest#accountNotFoundSurvivesRealListener` → 看红灯 → 按 D4 修 **B1**
- [ ] 启用 `LoginObserverSideEffectTest#disabledAccountAttemptMustBeAudited` → 修 **B2**（禁用分支补发事件）
- [ ] 新增 3.2.2 用例 → 修 **B3**（观察者 try-catch 或 `@TransactionalEventListener`）
- [ ] 启用 `blankUsernameReturnsResultShape` / `blankUsernameIsRejectedWithMessage` / `blankPasswordIsRejectedWithMessage` / `emptyBodyStillReturnsResultShape` → 引入 `@RestControllerAdvice` 修 **B6**
- [ ] 启用 `nullStatusIsNotNpe` → 修 **B11**（`Integer` 拆箱判空）
- [ ] 启用 `deviceShouldNotDuplicateUserAgent` → 修 **B4**
- [ ] 启用 `EmployeeMapperTest#usernameMustBeUnique` → `tb_employee.username` 加唯一索引（**B10**）
- [ ] 8.1.1 补账号不存在 / 禁用两条路径的「恰好 1 条日志」断言（随 B1/B2 一起转绿）
- [ ] 8.1.2 IP 解析优先级（`X-Forwarded-For` → `X-Real-IP` → `getRemoteAddr`）
- [ ] 8.1.3 `login_location` 归属（**B8**）：现状证据已在 `loginLocationIsCurrentlyHardcoded`，实现后改写该用例
- **验收**：登录四条路径全部可用且有留痕；前端能区分三类失败

### 阶段 3：凭证签发与网关校验（功能点 6，依赖 D1）

- [ ] 6.1.x 签发 JWT
- [ ] 6.3.1–6.3.4 网关鉴权过滤器 + 白名单
- [ ] 6.4.x 仅在选择 Session 方案时需要
- **验收**：受保护接口无 token 返回 401；伪造 `X-User-Id` 被覆盖

### 阶段 4：失败锁定（功能点 7，依赖 D2）

- [ ] 7.1.1–7.1.6 + 7.2.1
- **验收**：包含 7.1.6 的 Redis 降级策略（**这一条必须先定，否则会把"登录不可用"的风险引入系统**）

### 阶段 5：重构

- [ ] 按 §14 的 1→6 顺序推进

---

## 14. 重构方向（按投入产出比 × 风险排序）

1. **抽出 `PasswordEncoder` 抽象，隔离 BCrypt**（低风险 / 高收益）
   现在 `BCrypt` 是静态调用，无法替换也无法验证"是否真的用了哈希"。抽成接口后测试可用 `NoOpEncoder` 换速度，生产保留 BCrypt，顺便落实 D5 长度策略。
2. **引入 `LoginContext` 替换 `HttpServletRequest` 参数**（低风险 / 中收益）
   `EmployeeService.login(LoginDTO, HttpServletRequest)` 把 Web 容器类型泄露进业务接口，导致业务逻辑无法脱离 Servlet 容器测试（这正是当前没有测试的原因之一）。改为值对象后，1.3 / 2.1 / 3.1 / 4.1 可退化成纯内存测试。
3. **引入 `ResultCode` 枚举 + `@RestControllerAdvice`**（中风险 / 高收益）
   一次性解决 B5 与 B6，且有 2.3 / 5.1 的任务项保护。
4. **审计改为 `@TransactionalEventListener(AFTER_COMMIT)` + 观察者内部兜底**（中风险 / 高收益）
   修复 B1 / B3 的根因方案。前提是先有 8.1.1 那条"恰好 1 条日志"的断言作为安全网。
5. **数据库层加固**（低风险 / 高收益）
   `tb_employee.username` 加唯一索引（B10）；按 D4 决定 `tb_login_logs` 是否重建。**注意 SQLite 不支持 `ALTER COLUMN`**，修改 `NOT NULL` 需「建新表 → 迁数据 → 改名」三步；`ADD COLUMN` 则支持。
6. **引入 Redis 承载登录风控与凭证**（高风险 / 视 D1/D2 而定）
   唯一会引入新中间件依赖的改动。建议放最后，且必须先确认 Redis 不可用时的降级策略（7.1.6）。

**耦合点提示**：

- Controller 的 `catch (RuntimeException) → Result.error(e.getMessage())` 把「业务异常」和「数据库异常」压成同一个 `code = 500`，还**吞掉真实故障堆栈**（B1 对前端不可辨的直接成因）。
- 副作用走事件总线设计本身是好的（审计与统计解耦），但**没做故障隔离**：一个 `NOT NULL` 违规就能让整个登录接口报错。

---

## 15. 运行手册

### 15.1 执行测试

```bash
mvn -B test
```

**必须用 JDK 8 执行。** 项目依赖 `lombok 1.18.8`，它在 JDK 9+ 下访问 `com.sun.tools.javac.processing` 会被模块系统拦截：

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

若要在 JDK 17 下构建，需把 `lombok` 升到 `1.18.30+`（属独立改动）。

### 15.2 只跑某个测试类

```bash
JAVA_HOME="C:/Program Files/Java/jdk1.8.0_261" mvn -B test -pl shop -Dtest=EmployeeServiceImplLoginTest
```

### 15.3 把一个 `@Disabled` 占位转绿的标准动作

1. 删掉方法上的 `@Disabled(...)` 注解（注释里的缺陷编号不要删，挪到 `@DisplayName` 或方法上方注释里）。
2. 跑 `mvn -B test -pl shop -Dtest=xxxTest`，确认**确实是红的** —— 如果不是红的，说明测试没测到点上，先修测试。
3. 修改生产代码让红灯转绿。
4. 跑全量 `mvn -B test`，确认没有其他用例被带红（回归）。
5. 提交。

### 15.4 注意事项

- Maven 输出的中文可能是乱码（终端编码问题），**只看 `BUILD SUCCESS` 和 `Tests run / Failures / Errors`**。
- `schema-test.sql` 刻意保留了 `tb_login_logs.user_id NOT NULL`，这是复现 B1 的前提，**不要改成允许 NULL**（除非决定采纳 D4 的方案 ②）。
- `LoginFixtures.inject()` 用反射注入私有 `@Autowired` 字段，是**临时手段** —— 重构方向 2 落地（改构造注入）后应整体删除。
- 包路径 `shop/src/main/java/com.lxs.b2cmall.shop/`（把点号当目录名）与 `gateway` 的 `com/lxs/...` 写法不一致，Java 编译器接受但长期值得统一。

---

## 16. 附：文件清单

| 文件 | 作用 | 测试数 | 状态 |
|---|---|---|---|
| `.tdd/task/login-tdd-tasks.md` | 登录专项拆解（含完整测试思路与架构分析） | — | 已存在 |
| `tdd-test.md` | **本文件**，登录的可执行任务书（2026-10-16 收窄为仅登录） | — | 已交付 |
| `shop/src/test/java/.../support/LoginFixtures.java` | 造数据 + 反射注入 | — | 已存在 |
| `shop/src/test/java/.../support/InMemorySqlite.java` | 内存 SQLite 基座 | — | 已存在 |
| `shop/src/test/java/.../support/SqliteMapperSupport.java` | MyBatis Mapper 挂内存 SQLite（真库断言） | — | 已存在 |
| `shop/src/test/java/.../service/EmployeeServiceImplLoginTest.java` | 登录 Service 单测 | 17（14 绿 / 3 红） | 已存在 |
| `shop/src/test/java/.../controller/LoginControllerTest.java` | 登录接口契约测试 | 10（4 绿 / 6 红） | 已存在 |
| `shop/src/test/java/.../mapper/EmployeeMapperTest.java` | Mapper 真库映射与约束 | 6（5 绿 / 1 红） | 已存在 |
| `shop/src/test/java/.../observer/LoginObserverSideEffectTest.java` | 事件 → 数据库终态 | 5（3 绿 / 2 红） | 已存在 |
| ~~`.../service/EmployeeServiceImplRegisterTest.java`~~、~~`.../controller/RegisterControllerTest.java`~~ | 注册侧测试 | — | **已移除（2026-10-16 只保留登录）** |
| ~~`gateway/src/test/java/.../GatewayAuthSkeletonTest.java`~~ | 网关占位 | — | **已移除（本期不做）** |
| `shop/src/test/resources/schema-test.sql` | 测试表结构 | — | 已存在 |
| `shop/src/test/resources/application-test.yml` | 测试 profile | — | 已存在 |
| `shop/src/test/resources/bootstrap-test.yml` | 关 Nacos | — | 已存在 |
