package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.OrderCreateDTO;
import com.lxs.b2cmall.shop.entity.Order;
import com.lxs.b2cmall.shop.vo.PayResult;

public interface PayService {
    Order createOrder(OrderCreateDTO dto);
    PayResult pay(Integer orderId);
}