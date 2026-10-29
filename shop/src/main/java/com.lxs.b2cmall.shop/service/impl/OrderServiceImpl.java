package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.BatchShipDTO;
import com.lxs.b2cmall.shop.dto.OrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import com.lxs.b2cmall.shop.mapper.OrderItemMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.service.OrderService;
import com.lxs.b2cmall.shop.statemachine.OrderEvent;
import com.lxs.b2cmall.shop.statemachine.OrderStateMachine;
import com.lxs.b2cmall.shop.support.OrderLabelSupport;
import com.lxs.b2cmall.shop.vo.BatchShipResult;
import com.lxs.b2cmall.shop.vo.OrderDetailVO;
import com.lxs.b2cmall.shop.vo.OrderGroupVO;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private OrderStateMachine orderStateMachine;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 状态 Tab 固定顺序（与需求页面一致） */
    private static final List<String> TAB_STATUS = Arrays.asList(
            "待付款", "待发货", "待收货", "交易成功", "交易失败", "待退款");

    private static final List<String> EXCEL_HEADERS = Arrays.asList(
            "订单编号", "下单时间", "门店", "商品明细", "件数", "实付金额",
            "支付方式", "收货人", "手机号", "订单状态", "售后状态", "物流类型", "发货时间");

    @Override
    public List<Order> listOrders() {
        return orderMapper.findAll();
    }

    @Override
    public Order getOrder(Integer id) {
        return orderMapper.findById(id);
    }

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
        saveLog(orderId, fromStatus, toStatus, operator, "后台发货", now);

        order.setStatus(toStatus);
        order.setShipTime(now);
        return order;
    }

    // ==================== P3 店铺订单查询 ====================

    @Override
    public PageVO<OrderGroupVO> page(OrderQueryDTO query) {
        long total = orderMapper.countByQuery(query);
        List<OrderGroupVO> list = total == 0
                ? Collections.emptyList()
                : assemble(orderMapper.selectPageByQuery(query));
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        return PageVO.of(total, page, pageSize, list);
    }

    @Override
    public Map<String, Long> statusCount(Integer shopId) {
        Map<String, Long> raw = new HashMap<>();
        long all = 0;
        for (StatusCountVO vo : orderMapper.countGroupByStatus(shopId)) {
            long cnt = vo.getCnt() == null ? 0 : vo.getCnt();
            raw.put(vo.getStatus(), cnt);
            all += cnt;
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("全部", all);
        for (String status : TAB_STATUS) {
            result.put(status, raw.getOrDefault(status, 0L));
        }
        // 库里若出现未预置的状态值也一并返回，避免前端看不到
        for (Map.Entry<String, Long> e : raw.entrySet()) {
            if (!result.containsKey(e.getKey())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public BatchShipResult batchShip(BatchShipDTO dto) {
        if (dto == null || dto.getOrderIds() == null || dto.getOrderIds().isEmpty()) {
            throw new RuntimeException("批量发货必须指定订单 id 列表");
        }
        String operator = StringUtils.hasText(dto.getOperator()) ? dto.getOperator() : "admin";
        String now = LocalDateTime.now().format(FMT);

        BatchShipResult result = new BatchShipResult();
        result.setTotal(dto.getOrderIds().size());
        for (Integer orderId : dto.getOrderIds()) {
            try {
                Order order = orderMapper.findById(orderId);
                if (order == null) {
                    throw new RuntimeException("订单不存在");
                }
                String fromStatus = order.getStatus();
                // 状态机校验：非「待发货」会被拒绝，保证批量发货不会越级流转
                String toStatus = orderStateMachine.fire(fromStatus, OrderEvent.SHIP);
                orderMapper.updateShip(orderId, toStatus, now, dto.getDeliveryType());
                saveLog(orderId, fromStatus, toStatus, operator, "批量发货", now);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (RuntimeException e) {
                result.getFailures().add("订单 " + orderId + "：" + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public void export(OrderQueryDTO query, OutputStream out) {
        List<Order> orders = orderMapper.selectAllByQuery(query);
        List<OrderGroupVO> groups = assemble(orders);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("订单列表");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXCEL_HEADERS.size(); i++) {
                header.createCell(i).setCellValue(EXCEL_HEADERS.get(i));
            }
            int r = 1;
            for (OrderGroupVO g : groups) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nvl(g.getOrderNo()));
                row.createCell(1).setCellValue(nvl(g.getCreateTime()));
                row.createCell(2).setCellValue(nvl(g.getShopName()));
                row.createCell(3).setCellValue(itemsText(g.getItems()));
                row.createCell(4).setCellValue(g.getItemCount() == null ? 0 : g.getItemCount());
                row.createCell(5).setCellValue(g.getPayAmount() == null ? 0 : g.getPayAmount());
                row.createCell(6).setCellValue(nvl(g.getPayTypeLabel()));
                row.createCell(7).setCellValue(nvl(g.getReceiverName()));
                row.createCell(8).setCellValue(nvl(g.getReceiverPhone()));
                row.createCell(9).setCellValue(nvl(g.getStatus()));
                row.createCell(10).setCellValue(nvl(g.getAfterSaleStatus()));
                row.createCell(11).setCellValue(nvl(g.getDeliveryType()));
                row.createCell(12).setCellValue(nvl(g.getShipTime()));
            }
            wb.write(out);
        } catch (Exception e) {
            throw new RuntimeException("导出订单 Excel 失败：" + e.getMessage(), e);
        }
    }

    @Override
    public OrderDetailVO detail(Integer id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(assemble(Collections.singletonList(order)).get(0));
        vo.setStatusLogs(orderStatusLogMapper.findByOrderId(id));
        return vo;
    }

    // ==================== 内部装配 ====================

    /** 订单列表 → 订单组：一次性取回全部商品行，避免逐单查询 */
    private List<OrderGroupVO> assemble(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> orderIds = new ArrayList<>();
        for (Order o : orders) {
            orderIds.add(o.getId());
        }
        List<OrderItemVO> items = orderItemMapper.selectVoByOrderIds(orderIds);
        Map<Integer, List<OrderItemVO>> grouped = new HashMap<>();
        if (items != null) {
            for (OrderItemVO item : items) {
                item.setProductTypeLabel(productTypeLabel(item));
                grouped.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }
        List<OrderGroupVO> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItemVO> rows = grouped.get(o.getId());
            if (rows == null) {
                rows = Collections.emptyList();
            }
            result.add(toGroup(o, rows));
        }
        return result;
    }

    private OrderGroupVO toGroup(Order o, List<OrderItemVO> items) {
        OrderGroupVO g = new OrderGroupVO();
        g.setId(o.getId());
        g.setOrderNo(o.getOrderNo());
        g.setShopId(o.getShopId());
        g.setShopName(o.getShopName());
        g.setCreateTime(o.getCreateTime());
        g.setPayTime(o.getPayTime());
        g.setShipTime(o.getShipTime());
        g.setStatus(o.getStatus());
        g.setPayType(o.getPayType());
        g.setPayTypeLabel(payTypeLabel(o.getPayType()));
        g.setAfterSaleStatus(o.getAfterSaleStatus());
        g.setDeliveryType(o.getDeliveryType());
        g.setShippingStatus(o.getShippingStatus());
        g.setTotalAmount(o.getTotalAmount());
        g.setPayAmount(o.getPayAmount());
        g.setReceiverName(o.getReceiverName());
        g.setReceiverPhone(maskPhone(o.getReceiverPhone()));
        g.setReceiverAddress(o.getReceiverAddress());
        g.setItems(items);
        int count = 0;
        for (OrderItemVO item : items) {
            count += item.getQuantity() == null ? 0 : item.getQuantity();
        }
        g.setItemCount(count);
        g.setItemKinds(items.size());
        return g;
    }

    private void saveLog(Integer orderId, String fromStatus, String toStatus,
                         String operator, String remark, String createTime) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperator(StringUtils.hasText(operator) ? operator : "admin");
        log.setRemark(remark);
        log.setCreateTime(createTime);
        orderStatusLogMapper.insert(log);
    }

    /** 手机号脱敏：138****8000 */
    private String maskPhone(String phone) {
        return OrderLabelSupport.maskPhone(phone);
    }

    private String payTypeLabel(String payType) {
        return OrderLabelSupport.payTypeLabel(payType);
    }

    private String productTypeLabel(OrderItemVO item) {
        return OrderLabelSupport.productTypeLabel(item);
    }

    private String itemsText(List<OrderItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderItemVO item : items) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(nvl(item.getProductName())).append("×")
                    .append(item.getQuantity() == null ? 0 : item.getQuantity());
        }
        return sb.toString();
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
