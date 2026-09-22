package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.entity.Order;
import java.util.List;

public interface OrderService {
    List<Order> listOrders();
    Order getOrder(Integer id);
    Order ship(Integer orderId, String operator);
}