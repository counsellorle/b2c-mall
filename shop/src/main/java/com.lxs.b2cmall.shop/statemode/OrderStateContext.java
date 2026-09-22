package com.lxs.b2cmall.shop.statemode;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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