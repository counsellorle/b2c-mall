# B2C 商城项目核心功能详解

## 一、项目整体架构

```
用户请求
  ↓
Gateway（Spring Cloud Gateway，端口 8081，Nacos 服务发现）
  ↓
Controller（接收请求、参数校验）
  ↓
Service（业务逻辑）
  ↓
Mapper（MyBatis-Plus → SQLite）
  ↓
返回结果
```

### 技术栈

| 组件 | 技术选型 |
|------|----------|
| 网关 | Spring Cloud Gateway + Nacos |
| 框架 | Spring Boot 2.7.18 |
| 注册中心 | Nacos 1.4.7 |
| 数据库 | SQLite（通过 MyBatis 操作） |
| 缓存 | Redis |
| Java 版本 | JDK 8 |

### 模块划分

```
b2c_mall/
├── gateway/          ← 网关模块（路由转发、负载均衡）
│   └── 端口 8081
└── shop/             ← 业务模块（核心逻辑）
    └── 端口 8082
```

### 项目中穿插的 5 种设计模式

| 设计模式 | 解决什么问题 | 用在哪里 |
|----------|------------|----------|
| 观察者模式 | 一件事触发多个动作，且互不耦合 | 登录、注册 |
| 模板方法模式 | 流程固定但某一步骤不同 | 商品发布 |
| 状态机 | 状态不能乱跳，必须按规则流转 | 订单发货 |
| 策略模式 | 同一动作有多种算法，运行时切换 | 支付 |
| 状态模式 | 不同状态下可执行的动作不同 | 支付成功后的订单流转 |

---

## 二、观察者模式 —— 登录 & 注册

### 问题

登录成功后要做两件事：记录审计日志、更新用户登录次数。注册成功后也要做两件事：初始化账号、发欢迎通知。这些动作不应该写死在登录/注册代码里，否则以后加新动作就要改主流程代码。

### 解决方案

```
登录成功
  ↓
eventPublisher.publishEvent(new LoginEvent(...))   ← 主流程只发一个事件
  ↓（Spring 自动通知所有监听者）
  ├── AuditLogObserver.onLoginEvent()    → 写登录日志
  └── UserStatusObserver.onLoginEvent()  → 更新登录次数
```

### 代码解析

#### 1. 事件类（LoginEvent）—— 定义"发生了什么事"

```java
// shop/src/main/java/com.lxs.b2cmall.shop/event/LoginEvent.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent {
    private Integer userId;       // 谁登录的
    private String loginIp;       // 从哪个 IP
    private String loginDevice;   // 用什么设备
    private String userAgent;     // 浏览器信息
    private boolean success;      // 登录是否成功
}
```

#### 2. 主流程（EmployeeServiceImpl.login）—— 只负责核心业务

```java
// shop/src/main/java/com.lxs.b2cmall.shop/service/impl/EmployeeServiceImpl.java
@Override
public Employee login(LoginDTO loginDTO, HttpServletRequest request) {
    String ip = getClientIp(request);
    String ua = request.getHeader("User-Agent");

    Employee employee = employeeMapper.findByUsername(loginDTO.getUsername());
    if (employee == null) {
        eventPublisher.publishEvent(new LoginEvent(null, ip, ua, ua, false));
        throw new RuntimeException("账号不存在");
    }

    if (!BCrypt.checkpw(loginDTO.getPassword(), employee.getPassword())) {
        eventPublisher.publishEvent(new LoginEvent(employee.getId(), ip, ua, ua, false));
        throw new RuntimeException("密码错误");
    }

    if (employee.getStatus() != 1) {
        throw new RuntimeException("账号已被禁用");
    }

    // 校验通过，发布登录成功事件
    eventPublisher.publishEvent(new LoginEvent(employee.getId(), ip, ua, ua, true));

    employee.setPassword(null);
    return employee;
}
```

#### 3. 观察者（AuditLogObserver）—— 独立处理审计日志

```java
// shop/src/main/java/com.lxs.b2cmall.shop/observer/AuditLogObserver.java
@Component
public class AuditLogObserver {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @EventListener  // 告诉 Spring：LoginEvent 发生时请调用我
    public void onLoginEvent(LoginEvent event) {
        LoginLog log = new LoginLog();
        log.setUserId(event.getUserId());
        log.setLoginIp(event.getLoginIp());
        log.setLoginDevice(event.getLoginDevice());
        log.setLoginLocation("待解析");
        log.setLoginTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.setUserAgent(event.getUserAgent());
        log.setStatus(event.isSuccess() ? 1 : 0);
        loginLogMapper.insert(log);
    }
}
```

#### 4. 观察者（UserStatusObserver）—— 更新登录次数

```java
// shop/src/main/java/com.lxs.b2cmall.shop/observer/UserStatusObserver.java
@Component
public class UserStatusObserver {

    @Autowired
    private EmployeeMapper employeeMapper;

    @EventListener
    public void onLoginEvent(LoginEvent event) {
        if (event.isSuccess() && event.getUserId() != null) {
            employeeMapper.updateLoginInfo(event.getUserId());
        }
    }
}
```

#### 5. 注册事件同理

`RegisterEvent` 发出后，`AccountInitObserver` 负责初始化账号，`WelcomeNotifyObserver` 负责发站内信：

```java
// shop/src/main/java/com.lxs.b2cmall.shop/observer/WelcomeNotifyObserver.java
@Component
public class WelcomeNotifyObserver {

    @Autowired
    private MessageMapper messageMapper;

    @EventListener
    public void onRegisterEvent(RegisterEvent event) {
        Message message = new Message();
        message.setShopId(event.getShopId());
        message.setSenderId(event.getEmployeeId());
        message.setTitle("欢迎加入");
        message.setContent("尊敬的 " + event.getShopName() + " 管理员 " +
                event.getUsername() + "，欢迎使用 B2C 商城系统！");
        message.setMsgType(1);
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        messageMapper.insert(message);
    }
}
```

### 核心价值

新增观察者不需要改任何现有代码。比如以后要加"发送短信通知"，只需新建一个类：

```java
@Component
public class SmsObserver {
    @EventListener
    public void onLoginEvent(LoginEvent event) {
        // 发短信
    }
}
```

加上 `@Component` 就自动生效，登录代码一个字都不用改。

---

## 三、模板方法模式 —— 商品发布

### 问题

发布商品的流程是固定的：参数校验 → 类目校验 → 价格库存校验 → 内容审核 → 保存 → 上架。但实物商品和虚拟商品的"内容审核"这一步不同：实物验库存，虚拟验合规。

### 解决方案

```
AbstractProductPublishTemplate（抽象模板类）
│
│   publish() ← final，定义固定流程，子类不能改
│   ├── 1. validateParams()          ← 通用：所有商品都做
│   ├── 2. validateCategory()        ← 通用
│   ├── 3. validatePriceAndStock()   ← 通用
│   ├── 4. auditContent()            ← 抽象方法 ⭐ 子类各自实现
│   ├── 5. save()                    ← 通用
│   ├── 6. onSale()                  ← 通用
│   └── 7. afterProcess()            ← 可选覆盖，默认空
│
├── PhysicalProductPublishService（实物商品）
│       auditContent() → 校验库存 > 0
│
└── VirtualProductPublishService（虚拟商品）
        auditContent() → 校验合规信息
```

### 代码解析

#### 1. 模板类（AbstractProductPublishTemplate）—— 定义流程骨架

```java
// shop/src/main/java/com.lxs.b2cmall.shop/template/AbstractProductPublishTemplate.java
public abstract class AbstractProductPublishTemplate {

    @Autowired
    protected CategoryMapper categoryMapper;

    @Autowired
    protected ProductMapper productMapper;

    // final 确保子类不能修改流程顺序
    public final PublishResult publish(ProductDTO dto, Integer shopId) {
        validateParams(dto);        // 1. 参数校验（通用）
        validateCategory(dto);      // 2. 类目校验（通用）
        validatePriceAndStock(dto); // 3. 价格库存校验（通用）
        auditContent(dto);          // 4. 内容审核 ← 子类实现 ⭐
        Product product = save(dto, shopId);  // 5. 保存（通用）
        onSale(product);            // 6. 上架（通用）
        afterProcess(product);      // 7. 后置处理（可选覆盖）
        return PublishResult.ok(product);
    }

    // 抽象方法：子类必须实现
    protected abstract void auditContent(ProductDTO dto);

    // 可选覆盖：默认空实现
    protected void afterProcess(Product product) { }
}
```

#### 2. 实物商品子类 —— 只关注库存校验

```java
// shop/src/main/java/com.lxs.b2cmall.shop/template/PhysicalProductPublishService.java
@Component("physicalProductPublish")
public class PhysicalProductPublishService extends AbstractProductPublishTemplate {

    @Override
    protected void auditContent(ProductDTO dto) {
        // 实物商品：校验库存
        if (dto.getStock() == null || dto.getStock() <= 0) {
            throw new RuntimeException("实物商品库存必须大于0");
        }
        if (dto.getStockWarn() != null && dto.getStockWarn() >= dto.getStock()) {
            throw new RuntimeException("库存预警值不能大于等于实际库存");
        }
    }
}
```

#### 3. 虚拟商品子类 —— 只关注合规校验

```java
// shop/src/main/java/com.lxs.b2cmall.shop/template/VirtualProductPublishService.java
@Component("virtualProductPublish")
public class VirtualProductPublishService extends AbstractProductPublishTemplate {

    @Override
    protected void auditContent(ProductDTO dto) {
        // 虚拟商品：校验合规信息
        if (dto.getComplianceInfo() == null || dto.getComplianceInfo().trim().isEmpty()) {
            throw new RuntimeException("虚拟商品必须提供合规信息");
        }
    }
}
```

#### 4. 工厂（ProductPublishFactory）—— 按商品类型选择子类

```java
// shop/src/main/java/com.lxs.b2cmall.shop/template/ProductPublishFactory.java
@Component
public class ProductPublishFactory {

    private final Map<String, AbstractProductPublishTemplate> strategyMap = new HashMap<>();

    @Autowired
    public ProductPublishFactory(
            PhysicalProductPublishService physicalProductPublish,
            VirtualProductPublishService virtualProductPublish) {
        strategyMap.put("physical", physicalProductPublish);
        strategyMap.put("virtual", virtualProductPublish);
        strategyMap.put("combo", physicalProductPublish);    // 组合商品走实物逻辑
        strategyMap.put("card", virtualProductPublish);      // 电子卡券走虚拟逻辑
    }

    public AbstractProductPublishTemplate getStrategy(String productType) {
        AbstractProductPublishTemplate template = strategyMap.get(productType);
        if (template == null) {
            throw new RuntimeException("不支持的商品类型: " + productType);
        }
        return template;
    }
}
```

### 核心价值

流程固定在 `publish()` 方法里，保证步骤顺序不会被改乱。新增商品类型只需加一个子类，流程代码不用改。

---

## 四、状态机 —— 订单发货

### 问题

订单状态不能随便改。"待付款"的订单不能发货，"已发货"的不能重复发货。如果在每个 Service 方法里写 `if (status.equals("待发货"))` 这种判断，状态多了就乱了。

### 解决方案

```
状态流转表（定义规则）
  待付款 ──PAY_SUCCESS──→ 待发货 ──SHIP──→ 待收货 ──CONFIRM──→ 交易成功
    │
    └──CANCEL──→ 交易失败

OrderStateMachine.fire(当前状态, 事件) → 返回目标状态
  如果规则表里没有这条流转 → 抛异常
```

### 代码解析

#### 1. 状态和事件（枚举）

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemachine/OrderStatus.java
public enum OrderStatus {
    PENDING_PAYMENT("待付款"),
    PENDING_SHIPMENT("待发货"),
    SHIPPED("待收货"),
    COMPLETED("交易成功"),
    FAILED("交易失败"),
    PENDING_REFUND("待退款");

    private final String description;
    OrderStatus(String description) { this.description = description; }
    public String getDescription() { return description; }
}

// shop/src/main/java/com.lxs.b2cmall.shop/statemachine/OrderEvent.java
public enum OrderEvent {
    PAY_SUCCESS,    // 支付成功
    SHIP,           // 发货
    CONFIRM,        // 确认收货
    CANCEL,         // 取消
    APPLY_REFUND    // 申请退款
}
```

#### 2. 状态机（OrderStateMachine）—— 核心：定义流转规则表

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemachine/OrderStateMachine.java
@Component
public class OrderStateMachine {

    private final Map<String, Map<OrderEvent, String>> transitionTable = new HashMap<>();

    public OrderStateMachine() {
        // 待付款 → 支付成功 → 待发货
        addTransition(OrderStatus.PENDING_PAYMENT, OrderEvent.PAY_SUCCESS, OrderStatus.PENDING_SHIPMENT);
        // 待付款 → 取消 → 交易失败
        addTransition(OrderStatus.PENDING_PAYMENT, OrderEvent.CANCEL, OrderStatus.FAILED);
        // 待发货 → 发货 → 待收货
        addTransition(OrderStatus.PENDING_SHIPMENT, OrderEvent.SHIP, OrderStatus.SHIPPED);
        // 待收货 → 确认收货 → 交易成功
        addTransition(OrderStatus.SHIPPED, OrderEvent.CONFIRM, OrderStatus.COMPLETED);
        // 任意状态 → 申请退款 → 待退款
        addTransition(OrderStatus.PENDING_PAYMENT, OrderEvent.APPLY_REFUND, OrderStatus.PENDING_REFUND);
        addTransition(OrderStatus.PENDING_SHIPMENT, OrderEvent.APPLY_REFUND, OrderStatus.PENDING_REFUND);
        addTransition(OrderStatus.SHIPPED, OrderEvent.APPLY_REFUND, OrderStatus.PENDING_REFUND);
    }

    private void addTransition(OrderStatus from, OrderEvent event, OrderStatus to) {
        transitionTable
                .computeIfAbsent(from.getDescription(), k -> new HashMap<>())
                .put(event, to.getDescription());
    }

    // 执行流转：查表，不在表里就拒绝
    public String fire(String currentStatus, OrderEvent event) {
        Map<OrderEvent, String> transitions = transitionTable.get(currentStatus);
        if (transitions == null || !transitions.containsKey(event)) {
            throw new RuntimeException("非法状态流转: 当前状态[" + currentStatus + "] 不允许执行 [" + event.name() + "]");
        }
        return transitions.get(event);
    }
}
```

#### 3. 发货时使用

```java
// shop/src/main/java/com.lxs.b2cmall.shop/service/impl/OrderServiceImpl.java
@Override
public Order ship(Integer orderId, String operator) {
    Order order = orderMapper.findById(orderId);
    if (order == null) {
        throw new RuntimeException("订单不存在");
    }

    // 通过状态机执行状态流转
    String fromStatus = order.getStatus();
    String toStatus = orderStateMachine.fire(fromStatus, OrderEvent.SHIP);

    // 更新订单状态
    String now = LocalDateTime.now().format(FMT);
    orderMapper.updateStatus(orderId, toStatus, now);

    // 记录状态流转日志
    OrderStatusLog log = new OrderStatusLog();
    log.setOrderId(orderId);
    log.setFromStatus(fromStatus);
    log.setToStatus(toStatus);
    log.setOperator(operator);
    log.setRemark("后台发货");
    log.setCreateTime(now);
    orderStatusLogMapper.insert(log);

    order.setStatus(toStatus);
    order.setShipTime(now);
    return order;
}
```

### 核心价值

所有流转规则集中在一张表里，一目了然。非法操作被自动拦截，不需要到处写 `if` 判断。

---

## 五、策略模式 —— 支付

### 问题

用户可能选支付宝、微信、PayPal 或其他方式支付。每种支付的内部逻辑不同，但接口一样：传入金额，返回结果。如果写 `if (type.equals("alipay"))` 就是硬编码，加新支付方式就要改代码。

### 解决方案

```
PayStrategy（策略接口）
├── AlipayStrategy    → 走支付宝
├── WechatPayStrategy → 走微信
├── PaypalStrategy    → 走 PayPal
└── OtherPayStrategy  → 走其他

PayStrategyFactory → Map<payType, PayStrategy>
  用户选 "alipay" → 取出 AlipayStrategy 执行
  用户选 "wechat" → 取出 WechatPayStrategy 执行
```

### 代码解析

#### 1. 策略接口 —— 统一规范

```java
// shop/src/main/java/com.lxs.b2cmall.shop/strategy/PayStrategy.java
public interface PayStrategy {
    String getPayType();               // 返回支付类型标识
    PayResult pay(PayRequest request); // 执行支付
}
```

#### 2. 具体策略 —— 各自实现

```java
// shop/src/main/java/com.lxs.b2cmall.shop/strategy/AlipayStrategy.java
@Component
public class AlipayStrategy implements PayStrategy {

    @Override
    public String getPayType() { return "alipay"; }

    @Override
    public PayResult pay(PayRequest request) {
        String tradeNo = "ALI" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.out.println("【支付宝】订单 " + request.getOrderNo() + " 支付 " + request.getAmount() + " 元");
        return PayResult.ok("alipay", tradeNo, request.getAmount());
    }
}

// shop/src/main/java/com.lxs.b2cmall.shop/strategy/WechatPayStrategy.java
@Component
public class WechatPayStrategy implements PayStrategy {

    @Override
    public String getPayType() { return "wechat"; }

    @Override
    public PayResult pay(PayRequest request) {
        String tradeNo = "WX" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.out.println("【微信支付】订单 " + request.getOrderNo() + " 支付 " + request.getAmount() + " 元");
        return PayResult.ok("wechat", tradeNo, request.getAmount());
    }
}
```

#### 3. 策略工厂 —— 自动收集所有策略

```java
// shop/src/main/java/com.lxs.b2cmall.shop/strategy/PayStrategyFactory.java
@Component
public class PayStrategyFactory {

    private final Map<String, PayStrategy> strategyMap = new HashMap<>();

    // Spring 自动注入所有 PayStrategy 实现类到 List
    @Autowired
    public PayStrategyFactory(List<PayStrategy> strategies) {
        for (PayStrategy strategy : strategies) {
            strategyMap.put(strategy.getPayType(), strategy);
        }
    }

    public PayStrategy getStrategy(String payType) {
        PayStrategy strategy = strategyMap.get(payType);
        if (strategy == null) {
            throw new RuntimeException("不支持的支付方式: " + payType);
        }
        return strategy;
    }
}
```

#### 4. 使用

```java
// shop/src/main/java/com.lxs.b2cmall.shop/service/impl/PayServiceImpl.java
PayStrategy strategy = payStrategyFactory.getStrategy(order.getPayType());
PayResult result = strategy.pay(payRequest);
```

### 核心价值

零 `if-else`。新增支付方式（比如"银联支付"）只需加一个类实现 `PayStrategy` 接口加上 `@Component`，工厂自动收集，现有代码完全不用改。

---

## 六、状态模式 —— 支付成功后的订单流转

### 问题

和状态机类似，但状态模式更进一步：每个状态是一个对象，不同状态下可执行的操作封装在各自的类里。非法操作直接抛异常，而不是查表。

### 解决方案

```
OrderState（状态接口）
├── PendingPayState   → 待付款状态：允许 paySuccess、cancel
├── PendingShipState  → 待发货状态：允许 ship
├── ShippedState      → 待收货状态：允许 confirm
└── CompletedState    → 交易成功：终态，什么都不允许

OrderStateContext → 根据当前状态字符串，返回对应状态对象
```

### 代码解析

#### 1. 状态接口 —— 定义所有操作

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemode/OrderState.java
public interface OrderState {
    void paySuccess(Order order);  // 支付成功
    void ship(Order order);        // 发货
    void confirm(Order order);     // 确认收货
    void cancel(Order order);      // 取消
    String getStatusName();
}
```

#### 2. 待付款状态 —— 只允许支付和取消

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemode/PendingPayState.java
public class PendingPayState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        order.setStatus("待发货");  // 合法操作：状态变更
    }

    @Override
    public void ship(Order order) {
        throw new RuntimeException("待付款订单不能发货");  // 非法操作：拒绝
    }

    @Override
    public void confirm(Order order) {
        throw new RuntimeException("待付款订单不能确认收货");
    }

    @Override
    public void cancel(Order order) {
        order.setStatus("交易失败");  // 合法操作
    }

    @Override
    public String getStatusName() { return "待付款"; }
}
```

#### 3. 待发货状态 —— 只允许发货

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemode/PendingShipState.java
public class PendingShipState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        throw new RuntimeException("订单已支付，不能重复支付");
    }

    @Override
    public void ship(Order order) {
        order.setStatus("待收货");  // 合法操作
    }

    @Override
    public void confirm(Order order) {
        throw new RuntimeException("待发货订单不能确认收货");
    }

    @Override
    public void cancel(Order order) {
        throw new RuntimeException("已支付订单不能取消，请申请退款");
    }

    @Override
    public String getStatusName() { return "待发货"; }
}
```

#### 4. 上下文（OrderStateContext）—— 状态管理器

```java
// shop/src/main/java/com.lxs.b2cmall.shop/statemode/OrderStateContext.java
@Component
public class OrderStateContext {

    private final Map<String, OrderState> stateMap = new HashMap<>();

    public OrderStateContext() {
        stateMap.put("待付款", new PendingPayState());
        stateMap.put("待发货", new PendingShipState());
        stateMap.put("待收货", new ShippedState());
        stateMap.put("交易成功", new CompletedState());
    }

    public OrderState getState(String status) {
        OrderState state = stateMap.get(status);
        if (state == null) {
            throw new RuntimeException("未知的订单状态: " + status);
        }
        return state;
    }
}
```

#### 5. 支付时使用

```java
// shop/src/main/java/com.lxs.b2cmall.shop/service/impl/PayServiceImpl.java
OrderState currentState = orderStateContext.getState(order.getStatus());
currentState.paySuccess(order);  // 当前状态对象自己决定能不能支付
```

### 核心价值

每个状态类只关心自己允许什么操作，职责清晰。"待付款"状态的类里只有支付和取消的逻辑，不会出现发货的代码。状态多了也不会混乱。

---

## 七、5 种设计模式对比

| 维度 | 观察者 | 模板方法 | 状态机 | 策略 | 状态模式 |
|------|--------|---------|--------|------|---------|
| **核心思想** | 一对多通知 | 流程骨架 + 钩子 | 规则表查表 | 多种算法切换 | 状态即对象 |
| **解决的问题** | 解耦 | 流程复用 | 状态乱跳 | if-else 多 | 状态行为混乱 |
| **用在哪里** | 登录/注册 | 商品发布 | 订单发货 | 支付 | 支付后流转 |
| **扩展方式** | 加一个 Observer 类 | 加一个子类 | 加一行规则 | 加一个 Strategy 类 | 加一个 State 类 |
| **改现有代码？** | 不用 | 不用 | 加一行 | 不用 | 不用 |

---

## 八、项目结构总览

```
b2c_mall/
├── pom.xml                              ← 父 POM（统一依赖版本）
├── gateway/                             ← 网关模块
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../GatewayApplication.java
│       └── resources/application.yml    ← Nacos 配置
└── shop/                                ← 核心业务模块
    ├── pom.xml
    ├── shop.db                          ← SQLite 数据库文件
    └── src/main/
        ├── java/com.lxs.b2cmall.shop/
        │   ├── ShopApplication.java     ← 启动类
        │   ├── config/
        │   │   └── RedisConfig.java     ← Redis 配置
        │   ├── controller/              ← 5 个 Controller
        │   │   ├── CartController.java
        │   │   ├── DashboardController.java
        │   │   ├── FrontProductController.java
        │   │   ├── LoginController.java
        │   │   ├── OrderController.java
        │   │   ├── PayController.java
        │   │   └── ProductController.java
        │   ├── dto/                     ← 8 个数据传输对象
        │   ├── entity/                  ← 9 个实体类
        │   ├── event/                   ← 2 个事件（LoginEvent, RegisterEvent）
        │   ├── mapper/                  ← 10 个 MyBatis Mapper
        │   ├── observer/                ← 4 个观察者
        │   ├── service/                 ← 6 个 Service 接口
        │   │   └── impl/               ← 6 个 Service 实现
        │   ├── statemachine/            ← 状态机（3 个文件）
        │   ├── statemode/               ← 状态模式（6 个文件）
        │   ├── strategy/                ← 策略模式（6 个文件）
        │   ├── template/                ← 模板方法（4 个文件）
        │   └── vo/                      ← 4 个视图对象
        └── resources/
            ├── application.yml
            └── bootstrap.yml
```
