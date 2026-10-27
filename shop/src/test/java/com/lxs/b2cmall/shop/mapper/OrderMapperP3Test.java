package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.OrderItem;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.entity.Shop;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P3 订单查询 Mapper：11 个筛选项、分页、状态计数、商品行联表，全部跑在真 SQLite 上，
 * 确保动态 SQL 的写法与生产（SQLite）一致。
 */
class OrderMapperP3Test {

    private Connection conn;
    private SqlSession session;
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;

    private int orderId1;
    private int orderId2;
    private int orderId3;
    private int physicalProductId;
    private int virtualProductId;

    @BeforeEach
    void setUp() {
        conn = InMemorySqlite.open();
        SqlSessionFactory factory = SqliteMapperSupport.factory(conn,
                OrderMapper.class, OrderItemMapper.class, ShopMapper.class, ProductMapper.class);
        session = factory.openSession(true);
        orderMapper = session.getMapper(OrderMapper.class);
        orderItemMapper = session.getMapper(OrderItemMapper.class);
        ShopMapper shopMapper = session.getMapper(ShopMapper.class);
        ProductMapper productMapper = session.getMapper(ProductMapper.class);

        shopMapper.insert(shop(1, "一号店"));
        shopMapper.insert(shop(2, "二号店"));

        physicalProductId = insertProduct(productMapper, 1, "实物手机", "physical", "实物商品", 1999.0);
        virtualProductId = insertProduct(productMapper, 1, "在线课程", "virtual", "虚拟商品", 99.0);

        orderId1 = insertOrder(orderMapper, "ORD-1001", 1, "待发货", "alipay",
                "张三", "13800001234", "2026-09-15 10:00:00", "普通快递");
        orderId2 = insertOrder(orderMapper, "ORD-1002", 1, "待收货", "wechat",
                "李四", "13900005678", "2026-09-16 11:00:00", "同城配送");
        orderId3 = insertOrder(orderMapper, "ORD-2001", 2, "交易成功", "paypal",
                "王五", "13700009999", "2026-09-20 12:00:00", "普通快递");

        orderItemMapper.insert(item(orderId1, physicalProductId, "实物手机", 1999.0, 2));
        orderItemMapper.insert(item(orderId2, virtualProductId, "在线课程", 99.0, 1));
        orderItemMapper.insert(item(orderId3, physicalProductId, "实物手机", 1999.0, 1));
        // 商品已被删除的历史订单行：联表为空但行不能丢
        orderItemMapper.insert(item(orderId3, 999, "已删除商品", 10.0, 3));
    }

    @AfterEach
    void tearDown() throws Exception {
        session.close();
        conn.close();
    }

    @Test
    void shouldFilterByOrderNoStatusPayTypeShopAndReceiver() {
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setOrderNo("1001"))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setStatus("待发货"))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setPayType("wechat"))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setShopId(2))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setReceiverPhone("138000"))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setReceiverName("张"))));
        assertEquals(0, orderMapper.countByQuery(query(q -> q.setReceiverName("赵"))));
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setDeliveryType("同城配送"))));
        assertEquals(3, orderMapper.countByQuery(new OrderQueryDTO()));
    }

    @Test
    void shouldFilterByDateRangeWithDateOnlyBoundary() {
        // 只传日期时，右端点应补到 23:59:59，否则 16 日 11:00 的订单会被漏掉
        OrderQueryDTO q = query(dto -> {
            dto.setStartTime("2026-09-15");
            dto.setEndTime("2026-09-16");
        });
        assertEquals(2, orderMapper.countByQuery(q));

        OrderQueryDTO onlyDay = query(dto -> {
            dto.setStartTime("2026-09-16");
            dto.setEndTime("2026-09-16");
        });
        assertEquals(1, orderMapper.countByQuery(onlyDay));
    }

    @Test
    void shouldFilterByOrderTypeAndCategoryViaProductAttributes() {
        assertEquals(1, orderMapper.countByQuery(query(q -> q.setOrderType("virtual"))));
        // 实物商品：订单 1 与订单 3 都含实物商品
        assertEquals(2, orderMapper.countByQuery(query(q -> q.setOrderCategory("实物商品"))));
        assertEquals(0, orderMapper.countByQuery(query(q -> q.setOrderCategory("卡券"))));
    }

    @Test
    void shouldPageWithDescendingCreateTimeAndShopName() {
        OrderQueryDTO q = new OrderQueryDTO();
        q.setPage(1);
        q.setPageSize(2);
        List<Order> first = orderMapper.selectPageByQuery(q);
        assertEquals(2, first.size());
        assertEquals("ORD-2001", first.get(0).getOrderNo());
        assertEquals("ORD-1002", first.get(1).getOrderNo());
        assertEquals("二号店", first.get(0).getShopName());

        q.setPage(2);
        List<Order> second = orderMapper.selectPageByQuery(q);
        assertEquals(1, second.size());
        assertEquals("ORD-1001", second.get(0).getOrderNo());

        q.setPage(3);
        assertTrue(orderMapper.selectPageByQuery(q).isEmpty());
    }

    @Test
    void shouldReturnFixedStatusTabsWithCounts() {
        List<StatusCountVO> shop1 = orderMapper.countGroupByStatus(1);
        assertEquals(2, shop1.size());
        long total = 0;
        for (StatusCountVO vo : shop1) {
            total += vo.getCnt();
        }
        assertEquals(2, total);

        List<StatusCountVO> all = orderMapper.countGroupByStatus(null);
        long allTotal = 0;
        for (StatusCountVO vo : all) {
            allTotal += vo.getCnt();
        }
        assertEquals(3, allTotal);
    }

    @Test
    void shouldJoinProductTypeForOrderItemsAndKeepOrphanRows() {
        List<OrderItemVO> rows = orderItemMapper.selectVoByOrderIds(Arrays.asList(orderId1, orderId3));
        assertEquals(3, rows.size());

        OrderItemVO physical = rows.stream()
                .filter(r -> physicalProductId == r.getProductId())
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("physical", physical.getProductType());
        assertEquals("实物商品", physical.getProductCategory());

        OrderItemVO orphan = rows.stream()
                .filter(r -> Integer.valueOf(999).equals(r.getProductId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertNull(orphan.getProductType());
        assertEquals("已删除商品", orphan.getProductName());
        assertEquals(3, orphan.getQuantity());

        assertTrue(orderItemMapper.selectVoByOrderIds(Collections.singletonList(-1)).isEmpty());
    }

    @Test
    void shouldUpdateShipStatusAndDeliveryType() {
        int updated = orderMapper.updateShip(orderId1, "待收货", "2026-10-27 10:00:00", "同城配送");
        assertEquals(1, updated);
        Order reloaded = orderMapper.findById(orderId1);
        assertEquals("待收货", reloaded.getStatus());
        assertEquals("2026-10-27 10:00:00", reloaded.getShipTime());
        assertEquals("同城配送", reloaded.getDeliveryType());
        assertEquals("一号店", reloaded.getShopName());
    }

    // ==================== fixtures ====================

    private interface Customizer {
        void apply(OrderQueryDTO dto);
    }

    private OrderQueryDTO query(Customizer c) {
        OrderQueryDTO q = new OrderQueryDTO();
        c.apply(q);
        return q;
    }

    private Shop shop(int id, String name) {
        Shop s = new Shop();
        s.setId(id);
        s.setShopName(name);
        s.setAdminAccount("admin" + id);
        s.setAdminPassword("pwd");
        s.setStatus(1);
        s.setCreatedAt("2026-09-01 00:00:00");
        s.setUpdatedAt("2026-09-01 00:00:00");
        return s;
    }

    private int insertProduct(ProductMapper mapper, int shopId, String name,
                              String type, String category, double price) {
        Product p = new Product();
        p.setShopId(shopId);
        p.setName(name);
        p.setProductType(type);
        p.setProductCategory(category);
        p.setPrice(price);
        p.setStock(10);
        p.setWarehouseStock(10);
        p.setStoreStock(0);
        p.setRealSales(0);
        p.setFakeSales(0);
        p.setStatus("on_sale");
        p.setSort(0);
        p.setCreateTime("2026-09-01 00:00:00");
        p.setUpdateTime("2026-09-01 00:00:00");
        mapper.insert(p);
        return p.getId();
    }

    private int insertOrder(OrderMapper mapper, String orderNo, int shopId, String status,
                            String payType, String receiver, String phone,
                            String createTime, String deliveryType) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setUserId(1);
        o.setShopId(shopId);
        o.setTotalAmount(100.0);
        o.setPayAmount(100.0);
        o.setStatus(status);
        o.setPayType(payType);
        o.setReceiverName(receiver);
        o.setReceiverPhone(phone);
        o.setReceiverAddress("测试地址");
        o.setDeliveryType(deliveryType);
        o.setIsDeleted(0);
        o.setCreateTime(createTime);
        mapper.insert(o);
        return o.getId();
    }

    private OrderItem item(int orderId, int productId, String name, double price, int quantity) {
        OrderItem it = new OrderItem();
        it.setOrderId(orderId);
        it.setProductId(productId);
        it.setProductName(name);
        it.setProductImage("/img.png");
        it.setPrice(price);
        it.setQuantity(quantity);
        it.setSubtotal(price * quantity);
        return it;
    }
}
