package com.lxs.b2cmall.shop.statemachine;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    /**
     * 执行状态流转，返回目标状态
     */
    public String fire(String currentStatus, OrderEvent event) {
        Map<OrderEvent, String> transitions = transitionTable.get(currentStatus);
        if (transitions == null || !transitions.containsKey(event)) {
            throw new RuntimeException("非法状态流转: 当前状态[" + currentStatus + "] 不允许执行 [" + event.name() + "]");
        }
        return transitions.get(event);
    }
}