package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.OrderCreateDTO;
import com.lxs.b2cmall.shop.dto.PayRequest;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.OrderItem;
import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.OrderItemMapper;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.PayService;
import com.lxs.b2cmall.shop.statemode.OrderState;
import com.lxs.b2cmall.shop.statemode.OrderStateContext;
import com.lxs.b2cmall.shop.strategy.PayStrategy;
import com.lxs.b2cmall.shop.strategy.PayStrategyFactory;
import com.lxs.b2cmall.shop.vo.PayResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private PayStrategyFactory payStrategyFactory;

    @Autowired
    private OrderStateContext orderStateContext;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public Order createOrder(OrderCreateDTO dto) {
        String now = LocalDateTime.now().format(FMT);
        String orderNo = "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        double totalAmount = 0;

        // 1. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(dto.getUserId());
        order.setShopId(dto.getShopId());
        order.setPayType(dto.getPayType());
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddress(dto.getReceiverAddress());
        order.setCreateTime(now);
        order.setStatus("待付款");

        // 2. 计算总金额并创建订单商品
        for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
            Product product = productMapper.findById(itemDTO.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + itemDTO.getProductId());
            }
            if (product.getStock() < itemDTO.getQuantity()) {
                throw new RuntimeException("商品 " + product.getName() + " 库存不足");
            }
            totalAmount += product.getPrice() * itemDTO.getQuantity();
        }

        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        orderMapper.insert(order);

        // 3. 保存订单商品明细并扣减库存
        for (OrderCreateDTO.OrderItemDTO itemDTO : dto.getItems()) {
            Product product = productMapper.findById(itemDTO.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setQuantity(itemDTO.getQuantity());
            item.setSubtotal(product.getPrice() * itemDTO.getQuantity());
            orderItemMapper.insert(item);

            // 扣减库存
            product.setStock(product.getStock() - itemDTO.getQuantity());
            product.setUpdateTime(now);
            productMapper.updateStock(product.getId(), product.getStock(), now);
        }

        // 4. 记录状态日志
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(null);
        log.setToStatus("待付款");
        log.setOperator("系统");
        log.setRemark("创建订单");
        log.setCreateTime(now);
        orderStatusLogMapper.insert(log);

        return order;
    }

    @Override
    @Transactional
    public PayResult pay(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            return PayResult.fail("订单不存在");
        }

        // 1. 状态模式：校验当前状态是否允许支付
        OrderState currentState = orderStateContext.getState(order.getStatus());

        // 2. 策略模式：执行支付
        PayRequest payRequest = new PayRequest();
        payRequest.setOrderId(order.getId());
        payRequest.setOrderNo(order.getOrderNo());
        payRequest.setAmount(order.getPayAmount());
        payRequest.setPayType(order.getPayType());

        PayStrategy strategy = payStrategyFactory.getStrategy(order.getPayType());
        PayResult payResult = strategy.pay(payRequest);

        if (!payResult.isSuccess()) {
            return payResult;
        }

        // 3. 状态模式：支付成功后流转订单状态
        currentState.paySuccess(order);

        String now = LocalDateTime.now().format(FMT);
        order.setPayTime(now);
        orderMapper.updateStatus(order.getId(), order.getStatus(), now);
        orderMapper.updatePayTime(order.getId(), now);

        // 4. 记录状态日志
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus("待付款");
        log.setToStatus(order.getStatus());
        log.setOperator("系统");
        log.setRemark("支付成功，方式: " + order.getPayType());
        log.setCreateTime(now);
        orderStatusLogMapper.insert(log);

        return payResult;
    }
}