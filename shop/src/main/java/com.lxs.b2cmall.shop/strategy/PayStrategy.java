package com.lxs.b2cmall.shop.strategy;

import com.lxs.b2cmall.shop.dto.PayRequest;
import com.lxs.b2cmall.shop.vo.PayResult;

public interface PayStrategy {
    String getPayType();
    PayResult pay(PayRequest request);
}