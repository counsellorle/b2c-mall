package com.lxs.b2cmall.shop.strategy;

import com.lxs.b2cmall.shop.dto.PayRequest;
import com.lxs.b2cmall.shop.vo.PayResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AlipayStrategy implements PayStrategy {

    @Override
    public String getPayType() {
        return "alipay";
    }

    @Override
    public PayResult pay(PayRequest request) {
        // 模拟支付宝支付逻辑
        String tradeNo = "ALI" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.out.println("【支付宝】订单 " + request.getOrderNo() + " 支付 " + request.getAmount() + " 元");
        return PayResult.ok("alipay", tradeNo, request.getAmount());
    }
}