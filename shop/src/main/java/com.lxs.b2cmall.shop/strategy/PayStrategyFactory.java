package com.lxs.b2cmall.shop.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayStrategyFactory {

    private final Map<String, PayStrategy> strategyMap = new HashMap<>();

    @Autowired
    public PayStrategyFactory(List<PayStrategy> strategies) {
        for (PayStrategy strategy : strategies) {
            strategyMap.put(strategy.getPayType(), strategy);
        }
    }

    public PayStrategy getStrategy(String payType) {
        PayStrategy strategy = strategyMap.get(payType);
        if (strategy == null) {
            throw new RuntimeException("不支持的支付方式: " + payType);
        }
        return strategy;
    }
}