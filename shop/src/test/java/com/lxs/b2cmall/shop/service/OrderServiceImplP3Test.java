package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.BatchShipDTO;
import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import com.lxs.b2cmall.shop.mapper.OrderItemMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.service.impl.OrderServiceImpl;
import com.lxs.b2cmall.shop.statemachine.OrderStateMachine;
import com.lxs.b2cmall.shop.vo.BatchShipResult;
import com.lxs.b2cmall.shop.vo.OrderDetailVO;
import com.lxs.b2cmall.shop.vo.OrderGroupVO;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P3 订单查询服务层：订单组装配、手机号脱敏、状态 Tab 计数、批量发货与导出。
 * Mapper 全部 mock，关注编排逻辑（真 SQL 由 OrderMapperP3Test 覆盖）。
 */
class OrderServiceImplP3Test {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private OrderStatusLogMapper orderStatusLogMapper;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = Mockito.mock(OrderMapper.class);
        orderItemMapper = Mockito.mock(OrderItemMapper.class);
        orderStatusLogMapper = Mockito.mock(OrderStatusLogMapper.class);
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "orderItemMapper", orderItemMapper);
        ReflectionTestUtils.setField(service, "orderStatusLogMapper", orderStatusLogMapper);
        ReflectionTestUtils.setField(service, "orderStateMachine", new OrderStateMachine());
    }

    @Test
    void pageShouldAssembleOrderGroupsWithLabelsAndMaskedPhone() {
        Order o1 = order(1, "ORD-001", 1, "一号店", "待发货", "alipay", "13800001234");
        Order o2 = order(2, "ORD-002", 2, "二号店", "待收货", "wechat", null);
        when(orderMapper.countByQuery(any(OrderQueryDTO.class))).thenReturn(2L);
        when(orderMapper.selectPageByQuery(any(OrderQueryDTO.class))).thenReturn(Arrays.asList(o1, o2));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(Arrays.asList(
                item(1, 11, "实物手机", "physical", "实物商品", 1999.0, 2),
                item(1, 12, "老商品", null, null, 10.0, 1),
                item(2, 13, "在线课程", "virtual", "虚拟商品", 99.0, 3)));

        OrderQueryDTO q = new OrderQueryDTO();
        q.setPage(1);
        q.setPageSize(10);
        PageVO<OrderGroupVO> page = service.page(q);

        assertEquals(2, page.getTotal());
        assertEquals(2, page.getList().size());

        OrderGroupVO g1 = page.getList().get(0);
        assertEquals("ORD-001", g1.getOrderNo());
        assertEquals("一号店", g1.getShopName());
        assertEquals("138****1234", g1.getReceiverPhone());
        assertEquals("支付宝", g1.getPayTypeLabel());
        assertEquals(3, g1.getItemCount());   // 2 + 1 件
        assertEquals(2, g1.getItemKinds());   // 2 种商品
        assertEquals("实物商品", g1.getItems().get(0).getProductTypeLabel());
        assertEquals("", g1.getItems().get(1).getProductTypeLabel());  // 商品已删除，无类型

        OrderGroupVO g2 = page.getList().get(1);
        assertEquals("微信支付", g2.getPayTypeLabel());
        assertEquals(3, g2.getItemCount());
        assertEquals("虚拟商品", g2.getItems().get(0).getProductTypeLabel());
        assertNull(g2.getReceiverPhone());
    }

    @Test
    void pageShouldSkipQueryWhenNoMatch() {
        when(orderMapper.countByQuery(any(OrderQueryDTO.class))).thenReturn(0L);
        PageVO<OrderGroupVO> page = service.page(new OrderQueryDTO());
        assertEquals(0, page.getTotal());
        assertTrue(page.getList().isEmpty());
        verify(orderMapper, never()).selectPageByQuery(any(OrderQueryDTO.class));
    }

    @Test
    void statusCountShouldFillAllTabsInFixedOrder() {
        when(orderMapper.countGroupByStatus(1)).thenReturn(Arrays.asList(
                statusCount("待发货", 2L), statusCount("待收货", 1L), statusCount("已关闭", 4L)));

        Map<String, Long> result = service.statusCount(1);

        assertEquals(7L, result.get("全部"));   // 2 + 1 + 4
        assertEquals(0L, result.get("待付款"));
        assertEquals(2L, result.get("待发货"));
        assertEquals(1L, result.get("待收货"));
        assertEquals(0L, result.get("交易成功"));
        assertEquals(0L, result.get("交易失败"));
        assertEquals(0L, result.get("待退款"));
        assertEquals(4L, result.get("已关闭"));  // 未预置状态也要透出

        List<String> keys = Arrays.asList(result.keySet().toArray(new String[0]));
        assertEquals("全部", keys.get(0));
        assertEquals("待付款", keys.get(1));
    }

    @Test
    void batchShipShouldReportFailuresWithoutStopping() {
        when(orderMapper.findById(1)).thenReturn(order(1, "ORD-001", 1, "一号店", "待发货", "alipay", "13800001234"));
        when(orderMapper.findById(2)).thenReturn(order(2, "ORD-002", 1, "一号店", "待收货", "wechat", "13800001234"));
        when(orderMapper.findById(3)).thenReturn(null);

        BatchShipDTO dto = new BatchShipDTO();
        dto.setOrderIds(Arrays.asList(1, 2, 3));
        dto.setDeliveryType("同城配送");
        BatchShipResult result = service.batchShip(dto);

        assertEquals(3, result.getTotal());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getFailures().size());
        assertTrue(result.getFailures().get(0).contains("订单 2"));
        assertTrue(result.getFailures().get(1).contains("订单不存在"));

        verify(orderMapper).updateShip(eq(1), eq("待收货"), anyString(), eq("同城配送"));
        verify(orderMapper, never()).updateShip(eq(2), anyString(), anyString(), any());

        ArgumentCaptor<OrderStatusLog> captor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(captor.capture());
        assertEquals("待发货", captor.getValue().getFromStatus());
        assertEquals("待收货", captor.getValue().getToStatus());
        assertEquals("admin", captor.getValue().getOperator());  // 未传操作人时兜底
        assertEquals("批量发货", captor.getValue().getRemark());
    }

    @Test
    void batchShipShouldRejectEmptyOrderIds() {
        BatchShipDTO dto = new BatchShipDTO();
        dto.setOrderIds(Collections.emptyList());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.batchShip(dto));
        assertTrue(ex.getMessage().contains("必须指定订单"));
    }

    @Test
    void exportShouldWriteOrderRows() throws Exception {
        when(orderMapper.selectAllByQuery(any(OrderQueryDTO.class))).thenReturn(
                Collections.singletonList(order(1, "ORD-001", 1, "一号店", "待发货", "alipay", "13800001234")));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(Arrays.asList(
                item(1, 11, "实物手机", "physical", "实物商品", 1999.0, 2),
                item(1, 12, "手机壳", "physical", "实物商品", 20.0, 1)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.export(new OrderQueryDTO(), out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("订单编号", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("发货时间", sheet.getRow(0).getCell(12).getStringCellValue());

            assertEquals("ORD-001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("一号店", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("实物手机×2；手机壳×1", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals(3.0, sheet.getRow(1).getCell(4).getNumericCellValue(), 0.001);
            assertEquals(100.0, sheet.getRow(1).getCell(5).getNumericCellValue(), 0.001);
            assertEquals("支付宝", sheet.getRow(1).getCell(6).getStringCellValue());
            assertEquals("138****1234", sheet.getRow(1).getCell(8).getStringCellValue());
        }
    }

    @Test
    void detailShouldReturnOrderGroupWithStatusLogs() {
        when(orderMapper.findById(1)).thenReturn(order(1, "ORD-001", 1, "一号店", "待发货", "alipay", "13800001234"));
        when(orderItemMapper.selectVoByOrderIds(anyList())).thenReturn(
                Collections.singletonList(item(1, 11, "实物手机", "physical", "实物商品", 1999.0, 2)));
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(1);
        log.setToStatus("待发货");
        log.setRemark("支付成功");
        when(orderStatusLogMapper.findByOrderId(1)).thenReturn(Collections.singletonList(log));

        OrderDetailVO detail = service.detail(1);
        assertEquals("ORD-001", detail.getOrder().getOrderNo());
        assertEquals(1, detail.getStatusLogs().size());
        assertEquals("支付成功", detail.getStatusLogs().get(0).getRemark());
    }

    @Test
    void detailShouldRejectMissingOrder() {
        when(orderMapper.findById(99)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.detail(99));
        assertTrue(ex.getMessage().contains("订单不存在"));
    }

    // ==================== fixtures ====================

    private Order order(int id, String orderNo, int shopId, String shopName,
                        String status, String payType, String phone) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNo(orderNo);
        o.setShopId(shopId);
        o.setShopName(shopName);
        o.setStatus(status);
        o.setPayType(payType);
        o.setReceiverName("收货人");
        o.setReceiverPhone(phone);
        o.setReceiverAddress("测试地址");
        o.setDeliveryType("普通快递");
        o.setTotalAmount(100.0);
        o.setPayAmount(100.0);
        o.setCreateTime("2026-09-15 10:00:00");
        return o;
    }

    private OrderItemVO item(int orderId, int productId, String name,
                             String type, String category, double price, int quantity) {
        OrderItemVO it = new OrderItemVO();
        it.setOrderId(orderId);
        it.setProductId(productId);
        it.setProductName(name);
        it.setProductType(type);
        it.setProductCategory(category);
        it.setPrice(price);
        it.setQuantity(quantity);
        it.setSubtotal(price * quantity);
        return it;
    }

    private StatusCountVO statusCount(String status, Long cnt) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCnt(cnt);
        return vo;
    }
}
