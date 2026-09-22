package com.lxs.b2cmall.shop.statemode;

import com.lxs.b2cmall.shop.entity.Order;

public class CompletedState implements OrderState {

    @Override
    public void paySuccess(Order order) {
        throw new RuntimeException("订单已完成");
    }

    @Override
    public void ship(Order order) {
        throw new RuntimeException("订单已完成，不能发货");
    }

    @Override
    public void confirm(Order order) {
        throw new RuntimeException("订单已完成");
    }

    @Override
    public void cancel(Order order) {
        throw new RuntimeException("订单已完成，不能取消");
    }

    @Override
    public String getStatusName() {
        return "交易成功";
    }
}