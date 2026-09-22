package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import com.lxs.b2cmall.shop.mapper.OrderMapper;
import com.lxs.b2cmall.shop.mapper.OrderStatusLogMapper;
import com.lxs.b2cmall.shop.service.OrderService;
import com.lxs.b2cmall.shop.statemachine.OrderEvent;
import com.lxs.b2cmall.shop.statemachine.OrderStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private OrderStateMachine orderStateMachine;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
}