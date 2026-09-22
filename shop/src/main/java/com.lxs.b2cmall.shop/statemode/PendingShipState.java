package com.lxs.b2cmall.shop.statemode;

import com.lxs.b2cmall.shop.entity.Order;

public class PendingShipState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        throw new RuntimeException("订单已支付，不能重复支付");
    }

    @Override
    public void ship(Order order) {
        order.setStatus("待收货");
    }

    @Override
    public void confirm(Order order) {
        throw new RuntimeException("待发货订单不能确认收货");
    }

    @Override
    public void cancel(Order order) {
        throw new RuntimeException("已支付订单不能取消，请申请退款");
    }

    @Override
    public String getStatusName() {
        return "待发货";
    }
}