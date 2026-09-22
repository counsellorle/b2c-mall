package com.lxs.b2cmall.shop.statemode;

import com.lxs.b2cmall.shop.entity.Order;

public class ShippedState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        throw new RuntimeException("订单已支付");
    }

    @Override
    public void ship(Order order) {
        throw new RuntimeException("订单已发货，不能重复发货");
    }

    @Override
    public void confirm(Order order) {
        order.setStatus("交易成功");
    }

    @Override
    public void cancel(Order order) {
        throw new RuntimeException("已发货订单不能取消");
    }

    @Override
    public String getStatusName() {
        return "待收货";
    }
}