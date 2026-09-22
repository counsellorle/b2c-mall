package com.lxs.b2cmall.shop.statemode;

import com.lxs.b2cmall.shop.entity.Order;

public interface OrderState {
    void paySuccess(Order order);
    void ship(Order order);
    void confirm(Order order);
    void cancel(Order order);
    String getStatusName();
}