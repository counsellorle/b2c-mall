package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.FrontOrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Cart;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.CartMapper;
import com.lxs.b2cmall.shop.mapper.OrderItemMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.impl.FrontOrderServiceImpl;
import com.lxs.b2cmall.shop.vo.FrontOrderVO;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.RebuyResult;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P5 C 端订单：分页装配、Tab 计数、详情、逻辑删除（终态限制 + 归属）、复购。
 * Mapper 全部 mock，真 SQL 由 OrderMapperP5Test 覆盖。
 */
class FrontOrderServiceImplTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private OrderStatusLogMapper orderStatusLogMapper;
    private ProductMapper productMapper;
    private CartMapper cartMapper;
    private FrontOrderServiceImpl service;

    private static final int USER = 7;

    @BeforeEach
    void setUp() {
        orderMapper = Mockito.mock(OrderMapper.class);
        orderItemMapper = Mockito.mock(OrderItemMapper.class);
        orderStatusLogMapper = Mockito.mock(OrderStatusLogMapper.class);
        productMapper = Mockito.mock(ProductMapper.class);
        cartMapper = Mockito.mock(CartMapper.class);
        service = new FrontOrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "orderItemMapper", orderItemMapper);
        ReflectionTestUtils.setField(service, "orderStatusLogMapper", orderStatusLogMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "cartMapper", cartMapper);
    }

    private Order order(int id, String status, String payType, String payTime) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNo("ORD-" + id);
        o.setUserId(USER);
        o.setShopId(1);
        o.setShopName("一号店");
        o.setStatus(status);
        o.setPayType(payType);
        o.setPayTime(payTime);
        o.setDeliveryType("普通快递");
        o.setShippingStatus("已签收");
        o.setTotalAmount(200.0);
        o.setPayAmount(199.0);
        o.setReceiverName("张三");
        o.setReceiverPhone("13800001234");
        o.setReceiverAddress("上海市浦东新区");
        o.setCreateTime("2026-09-20 10:00:00");
        return o;
    }

    private OrderItemVO item(int orderId, int productId, String name, double price, int qty) {
        OrderItemVO i = new OrderItemVO();
        i.setOrderId(orderId);
        i.setProductId(productId);
        i.setProductName(name);
        i.setPrice(price);
        i.setQuantity(qty);
        i.setSubtotal(price * qty);
        return i;
    }

    @Test
    void pageOnlyQueriesCurrentUserAndAssemblesCard() {
        when(orderMapper.countByUser(eq(USER), any(), any())).thenReturn(1L);
        when(orderMapper.selectPageByUser(eq(USER), any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(order(5, "交易成功", "alipay", "2026-09-20 10:05:00")));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(Arrays.asList(
                item(5, 101, "iPhone 15", 5999.0, 2),
                item(5, 102, "充电器", 99.0, 1)));

        PageVO<FrontOrderVO> page = service.page(USER, new FrontOrderQueryDTO());

        assertEquals(1, page.getTotal());
        assertEquals(10, page.getPageSize(), "默认每页 10 条");
        FrontOrderVO vo = page.getList().get(0);
        assertEquals("ORD-5", vo.getOrderNo());
        assertEquals("支付宝", vo.getPayTypeLabel());
        assertEquals(3, vo.getItemCount());
        assertEquals(2, vo.getItemKinds());
        assertEquals("138****1234", vo.getReceiverPhone(), "C 端手机号仍脱敏");
        assertTrue(vo.getCanDelete(), "终态订单可删除");
        assertTrue(vo.getCanRebuy(), "已支付订单可复购");
        verify(orderMapper).countByUser(eq(USER), isNull(), isNull());
    }

    @Test
    void pageReturnsEmptyWithoutQueryingItems() {
        when(orderMapper.countByUser(eq(USER), any(), any())).thenReturn(0L);

        PageVO<FrontOrderVO> page = service.page(USER, new FrontOrderQueryDTO());

        assertTrue(page.getList().isEmpty());
        verify(orderItemMapper, never()).selectVoByOrderIds(anyList());
    }

    @Test
    void statusCountKeepsFixedOrderAndFillsZero() {
        StatusCountVO pending = new StatusCountVO();
        pending.setStatus("待付款");
        pending.setCnt(2L);
        StatusCountVO done = new StatusCountVO();
        done.setStatus("交易成功");
        done.setCnt(1L);
        when(orderMapper.countGroupByStatusForUser(USER)).thenReturn(Arrays.asList(pending, done));

        Map<String, Long> counts = service.statusCount(USER);

        List<String> keys = Arrays.asList(counts.keySet().toArray(new String[0]));
        assertEquals("全部", keys.get(0));
        assertEquals(Arrays.asList("全部", "待付款", "待发货", "待收货", "交易成功", "交易失败", "待退款"), keys);
        assertEquals(3L, counts.get("全部"));
        assertEquals(2L, counts.get("待付款"));
        assertEquals(1L, counts.get("交易成功"));
        assertEquals(0L, counts.get("待收货"));
    }

    @Test
    void detailFillsStatusLogs() {
        when(orderMapper.findByIdForUser(5, USER)).thenReturn(order(5, "待收货", null, null));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(Collections.emptyList());
        when(orderStatusLogMapper.findByOrderId(5)).thenReturn(Collections.emptyList());

        FrontOrderVO vo = service.detail(USER, 5);

        assertNotNull(vo);
        assertEquals("待收货", vo.getStatus());
        assertFalse(vo.getCanDelete(), "进行中订单不可删除");
        verify(orderStatusLogMapper).findByOrderId(5);
    }

    @Test
    void detailRejectsOtherUsersOrder() {
        when(orderMapper.findByIdForUser(9, USER)).thenReturn(null);

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.detail(USER, 9));
        assertEquals("订单不存在或已删除", e.getMessage());
    }

    @Test
    void deleteRejectsInProgressOrder() {
        when(orderMapper.findByIdForUser(5, USER)).thenReturn(order(5, "待发货", null, "2026-09-20 10:05:00"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.delete(USER, 5));
        assertEquals("订单进行中，暂不支持删除", e.getMessage());
        verify(orderMapper, never()).logicalDelete(anyInt(), anyInt());
    }

    @Test
    void deleteMarksTerminalOrder() {
        when(orderMapper.findByIdForUser(5, USER)).thenReturn(order(5, "交易成功", null, "2026-09-20 10:05:00"));
        when(orderMapper.logicalDelete(5, USER)).thenReturn(1);

        service.delete(USER, 5);

        verify(orderMapper).logicalDelete(5, USER);
    }

    @Test
    void rebuySkipsOffSaleAndMergesCartQuantity() {
        when(orderMapper.findByIdForUser(5, USER)).thenReturn(order(5, "交易成功", null, "2026-09-20 10:05:00"));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(Arrays.asList(
                item(5, 101, "在售商品", 100.0, 2),
                item(5, 102, "已下架商品", 50.0, 1)));

        Product onSale = new Product();
        onSale.setId(101);
        onSale.setStatus("on_sale");
        onSale.setStock(10);
        Product offSale = new Product();
        offSale.setId(102);
        offSale.setStatus("off_sale");
        offSale.setStock(10);
        when(productMapper.findById(101)).thenReturn(onSale);
        when(productMapper.findById(102)).thenReturn(offSale);

        Cart existing = new Cart();
        existing.setId(88);
        existing.setQuantity(1);
        when(cartMapper.findByUserAndProduct(USER, 101)).thenReturn(existing);

        RebuyResult result = service.rebuy(USER, 5);

        assertEquals(1, result.getAddedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(Collections.singletonList("已下架商品"), result.getSkippedProducts());
        verify(cartMapper).updateQuantity(88, 3);
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void rebuyInsertsNewCartRowWhenAbsent() {
        when(orderMapper.findByIdForUser(5, USER)).thenReturn(order(5, "交易成功", null, "2026-09-20 10:05:00"));
        when(orderItemMapper.selectVoByOrderIds(anyList()))
                .thenReturn(Collections.singletonList(item(5, 101, "在售商品", 100.0, 2)));

        Product onSale = new Product();
        onSale.setId(101);
        onSale.setStatus("on_sale");
        onSale.setStock(10);
        when(productMapper.findById(101)).thenReturn(onSale);
        when(cartMapper.findByUserAndProduct(USER, 101)).thenReturn(null);

        RebuyResult result = service.rebuy(USER, 5);

        assertEquals(1, result.getAddedCount());
        verify(cartMapper).insert(any(Cart.class));
        verify(cartMapper, never()).updateQuantity(anyInt(), anyInt());
    }
}
