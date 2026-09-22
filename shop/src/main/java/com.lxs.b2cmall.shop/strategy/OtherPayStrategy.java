package com.lxs.b2cmall.shop.strategy;

import com.lxs.b2cmall.shop.dto.PayRequest;
import com.lxs.b2cmall.shop.vo.PayResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OtherPayStrategy implements PayStrategy {

    @Override
    public String getPayType() {
        return "other";
    }

    @Override
    public PayResult pay(PayRequest request) {
        String tradeNo = "OT" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.out.println("【其他支付】订单 " + request.getOrderNo() + " 支付 " + request.getAmount() + " 元");
        return PayResult.ok("other", tradeNo, request.getAmount());
    }
}