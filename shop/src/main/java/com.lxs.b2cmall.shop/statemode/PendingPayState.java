package com.lxs.b2cmall.shop.statemode;

import com.lxs.b2cmall.shop.entity.Order;

public class PendingPayState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        order.setStatus("待发货");
    }

    @Override
    public void ship(Order order) {
        throw new RuntimeException("待付款订单不能发货");
    }

    @Override
    public void confirm(Order order) {
        throw new RuntimeException("待付款订单不能确认收货");
    }

    @Override
    public void cancel(Order order) {
        order.setStatus("交易失败");
    }

    @Override
    public String getStatusName() {
        return "待付款";
    }
}