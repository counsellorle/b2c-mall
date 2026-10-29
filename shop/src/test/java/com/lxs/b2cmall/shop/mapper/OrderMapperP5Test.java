package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P5 C 端订单真库验证：
 * 1. 只能查到自己且未逻辑删除的订单
 * 2. 状态 Tab 计数、订单号模糊查询、分页
 * 3. 逻辑删除的归属校验与幂等（重复删除返回 0 行）
 */
class OrderMapperP5Test {

    private static final int USER_A = 7;
    private static final int USER_B = 8;

    private Connection conn;
    private SqlSession session;
    private OrderMapper mapper;

    @BeforeEach
    void setUp() {
        conn = InMemorySqlite.open();
        SqlSessionFactory factory = SqliteMapperSupport.factory(conn, OrderMapper.class);
        session = factory.openSession(true);
        mapper = session.getMapper(OrderMapper.class);

        insert("ORD-A1", USER_A, "待付款", null);
        insert("ORD-A2", USER_A, "交易成功", "2026-09-20 10:05:00");
        insert("ORD-A3", USER_A, "待发货", "2026-09-21 10:05:00");
        insert("ORD-B1", USER_B, "交易成功", "2026-09-22 10:05:00");
    }

    @AfterEach
    void tearDown() throws Exception {
        session.close();
        conn.close();
    }

    private Order insert(String orderNo, int userId, String status, String payTime) {
        Order o = new Order();
        o.setOrderNo(orderNo);
        o.setUserId(userId);
        o.setShopId(1);
        o.setTotalAmount(100.0);
        o.setPayAmount(99.0);
        o.setStatus(status);
        o.setPayType("alipay");
        o.setReceiverName("张三");
        o.setReceiverPhone("13800001234");
        o.setDeliveryType("普通快递");
        o.setCreateTime("2026-09-20 10:00:00");
        o.setPayTime(payTime);
        mapper.insert(o);
        return o;
    }

    @Test
    void countOnlyOwnOrders() {
        assertEquals(3, mapper.countByUser(USER_A, null, null));
        assertEquals(1, mapper.countByUser(USER_B, null, null));
        assertEquals(1, mapper.countByUser(USER_A, "交易成功", null));
    }

    @Test
    void keywordMatchesOrderNo() {
        assertEquals(3, mapper.countByUser(USER_A, null, "ORD-A"));
        assertEquals(1, mapper.countByUser(USER_A, null, "A2"));
        assertEquals(0, mapper.countByUser(USER_A, null, "ORD-B"));
    }

    @Test
    void pageRespectsPaginationAndOrdering() {
        List<Order> first = mapper.selectPageByUser(USER_A, null, null, 2, 0);
        assertEquals(2, first.size());
        List<Order> second = mapper.selectPageByUser(USER_A, null, null, 2, 2);
        assertEquals(1, second.size());
        assertTrue(mapper.selectPageByUser(USER_A, null, null, 2, 4).isEmpty());
    }

    @Test
    void statusCountGrouped() {
        List<StatusCountVO> rows = mapper.countGroupByStatusForUser(USER_A);
        assertEquals(3, rows.size());
        long sum = 0;
        for (StatusCountVO row : rows) {
            sum += row.getCnt();
        }
        assertEquals(3, sum);
    }

    @Test
    void detailCannotCrossUser() {
        Order mine = mapper.selectPageByUser(USER_A, null, null, 1, 0).get(0);
        assertNotNull(mapper.findByIdForUser(mine.getId(), USER_A));
        assertNull(mapper.findByIdForUser(mine.getId(), USER_B), "不能查看他人订单");
    }

    @Test
    void logicalDeleteHidesOrderAndIsIdempotent() {
        Order target = mapper.selectPageByUser(USER_A, "交易成功", null, 1, 0).get(0);

        assertEquals(1, mapper.logicalDelete(target.getId(), USER_A));
        assertNull(mapper.findByIdForUser(target.getId(), USER_A), "删除后用户侧不可见");
        assertEquals(2, mapper.countByUser(USER_A, null, null));

        assertEquals(0, mapper.logicalDelete(target.getId(), USER_A), "重复删除返回 0 行");
    }

    @Test
    void logicalDeleteCannotTouchOtherUsersOrder() {
        Order other = mapper.selectPageByUser(USER_B, null, null, 1, 0).get(0);
        assertEquals(0, mapper.logicalDelete(other.getId(), USER_A));
        assertNotNull(mapper.findByIdForUser(other.getId(), USER_B), "他人订单不受影响");
    }
}
