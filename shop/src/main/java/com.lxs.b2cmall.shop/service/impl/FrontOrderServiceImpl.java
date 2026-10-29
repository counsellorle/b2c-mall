package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.FrontOrderQueryDTO;
import com.lxs.b2cmall.shop.entity.Cart;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.CartMapper;
import com.lxs.b2cmall.shop.mapper.OrderItemMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.FrontOrderService;
import com.lxs.b2cmall.shop.support.OrderLabelSupport;
import com.lxs.b2cmall.shop.vo.FrontOrderVO;
import com.lxs.b2cmall.shop.vo.OrderItemVO;
import com.lxs.b2cmall.shop.vo.PageVO;
import com.lxs.b2cmall.shop.vo.RebuyResult;
import com.lxs.b2cmall.shop.vo.StatusCountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FrontOrderServiceImpl implements FrontOrderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Tab 固定顺序 */
    private static final List<String> TAB_STATUS = Arrays.asList(
            "待付款", "待发货", "待收货", "交易成功", "交易失败", "待退款");

    /** 终态订单才允许用户删除（进行中订单删掉会导致看不到物流与售后入口） */
    private static final Set<String> TERMINAL_STATUS = new HashSet<>(Arrays.asList(
            "交易成功", "交易失败", "已取消", "已关闭"));

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;

    @Override
    public PageVO<FrontOrderVO> page(Integer userId, FrontOrderQueryDTO query) {
        FrontOrderQueryDTO q = query == null ? new FrontOrderQueryDTO() : query;
        long total = orderMapper.countByUser(userId, q.getStatus(), q.getKeyword());
        List<FrontOrderVO> list = total == 0
                ? Collections.emptyList()
                : assemble(orderMapper.selectPageByUser(userId, q.getStatus(), q.getKeyword(),
                q.getLimit(), q.getOffset()));
        return PageVO.of(total, q.getPage(), q.getLimit(), list);
    }

    @Override
    public Map<String, Long> statusCount(Integer userId) {
        Map<String, Long> raw = new HashMap<>();
        long all = 0;
        List<StatusCountVO> rows = orderMapper.countGroupByStatusForUser(userId);
        if (rows != null) {
            for (StatusCountVO vo : rows) {
                long cnt = vo.getCnt() == null ? 0 : vo.getCnt();
                raw.put(vo.getStatus(), cnt);
                all += cnt;
            }
        }
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("全部", all);
        for (String status : TAB_STATUS) {
            result.put(status, raw.getOrDefault(status, 0L));
        }
        for (Map.Entry<String, Long> e : raw.entrySet()) {
            if (!result.containsKey(e.getKey())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public FrontOrderVO detail(Integer userId, Integer orderId) {
        Order order = orderMapper.findByIdForUser(orderId, userId);
        if (order == null) {
            throw new RuntimeException("订单不存在或已删除");
        }
        List<OrderItemVO> items = orderItemMapper.selectVoByOrderIds(
                Collections.singletonList(orderId));
        FrontOrderVO vo = toVO(order, items == null ? Collections.emptyList() : items);
        vo.setStatusLogs(orderStatusLogMapper.findByOrderId(orderId));
        return vo;
    }

    @Override
    public void delete(Integer userId, Integer orderId) {
        Order order = orderMapper.findByIdForUser(orderId, userId);
        if (order == null) {
            throw new RuntimeException("订单不存在或已删除");
        }
        if (!TERMINAL_STATUS.contains(order.getStatus())) {
            throw new RuntimeException("订单进行中，暂不支持删除");
        }
        int affected = orderMapper.logicalDelete(orderId, userId);
        if (affected == 0) {
            throw new RuntimeException("订单不存在或已删除");
        }
    }

    @Override
    public RebuyResult rebuy(Integer userId, Integer orderId) {
        Order order = orderMapper.findByIdForUser(orderId, userId);
        if (order == null) {
            throw new RuntimeException("订单不存在或已删除");
        }
        List<OrderItemVO> items = orderItemMapper.selectVoByOrderIds(
                Collections.singletonList(orderId));
        RebuyResult result = new RebuyResult();
        if (items == null || items.isEmpty()) {
            return result;
        }
        String now = LocalDateTime.now().format(FMT);
        for (OrderItemVO item : items) {
            Product product = item.getProductId() == null ? null
                    : productMapper.findById(item.getProductId());
            if (product == null || !"on_sale".equals(product.getStatus())) {
                result.getSkippedProducts().add(item.getProductName());
                continue;
            }
            int stock = product.getStock() == null ? 0 : product.getStock();
            if (stock <= 0) {
                result.getSkippedProducts().add(item.getProductName());
                continue;
            }
            int qty = item.getQuantity() == null || item.getQuantity() < 1 ? 1 : item.getQuantity();
            // 已在购物车则合并数量，避免同一商品出现多行
            Cart existing = cartMapper.findByUserAndProduct(userId, product.getId());
            if (existing != null) {
                int base = existing.getQuantity() == null ? 0 : existing.getQuantity();
                cartMapper.updateQuantity(existing.getId(), base + qty);
            } else {
                Cart cart = new Cart();
                cart.setUserId(userId);
                cart.setProductId(product.getId());
                cart.setQuantity(qty);
                cart.setCreateTime(now);
                cartMapper.insert(cart);
            }
            result.setAddedCount(result.getAddedCount() + 1);
        }
        result.setSkippedCount(result.getSkippedProducts().size());
        return result;
    }

    // ==================== 内部装配 ====================

    private List<FrontOrderVO> assemble(List<Order> orders) {
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
                grouped.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }
        List<FrontOrderVO> result = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItemVO> rows = grouped.get(o.getId());
            result.add(toVO(o, rows == null ? Collections.emptyList() : rows));
        }
        return result;
    }

    private FrontOrderVO toVO(Order o, List<OrderItemVO> items) {
        FrontOrderVO vo = new FrontOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setShopId(o.getShopId());
        vo.setShopName(o.getShopName());
        vo.setCreateTime(o.getCreateTime());
        vo.setPayTime(o.getPayTime());
        vo.setShipTime(o.getShipTime());
        vo.setStatus(o.getStatus());
        vo.setAfterSaleStatus(o.getAfterSaleStatus());
        vo.setPayType(o.getPayType());
        vo.setPayTypeLabel(OrderLabelSupport.payTypeLabel(o.getPayType()));
        vo.setDeliveryType(o.getDeliveryType());
        vo.setShippingStatus(o.getShippingStatus());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setPayAmount(o.getPayAmount());
        vo.setReceiverName(o.getReceiverName());
        // C 端看自己的订单，手机号仍做脱敏展示，避免截图外泄
        vo.setReceiverPhone(OrderLabelSupport.maskPhone(o.getReceiverPhone()));
        vo.setReceiverAddress(o.getReceiverAddress());

        int count = 0;
        for (OrderItemVO item : items) {
            item.setProductTypeLabel(OrderLabelSupport.productTypeLabel(item));
            count += item.getQuantity() == null ? 0 : item.getQuantity();
        }
        vo.setItems(items);
        vo.setItemCount(count);
        vo.setItemKinds(items.size());

        vo.setCanDelete(TERMINAL_STATUS.contains(o.getStatus()));
        vo.setCanRebuy(StringUtils.hasText(o.getPayTime()) && !items.isEmpty());
        return vo;
    }
}
