package com.lxs.b2cmall.shop.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProductPublishFactory {

    private final Map<String, AbstractProductPublishTemplate> strategyMap = new HashMap<>();

    @Autowired
    public ProductPublishFactory(
            PhysicalProductPublishService physicalProductPublish,
            VirtualProductPublishService virtualProductPublish) {
        strategyMap.put("physical", physicalProductPublish);
        strategyMap.put("virtual", virtualProductPublish);
        strategyMap.put("combo", physicalProductPublish);    // 组合商品走实物逻辑
        strategyMap.put("card", virtualProductPublish);      // 电子卡券走虚拟逻辑
    }

    public AbstractProductPublishTemplate getStrategy(String productType) {
        AbstractProductPublishTemplate template = strategyMap.get(productType);
        if (template == null) {
            throw new RuntimeException("不支持的商品类型: " + productType);
        }
        return template;
    }
}