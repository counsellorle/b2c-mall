package com.lxs.b2cmall.shop.template;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import org.springframework.stereotype.Component;

@Component("physicalProductPublish")
public class PhysicalProductPublishService extends AbstractProductPublishTemplate {

    @Override
    protected void auditContent(ProductDTO dto) {
        // 实物商品：校验库存
        if (dto.getStock() == null || dto.getStock() <= 0) {
            throw new RuntimeException("实物商品库存必须大于0");
        }
        if (dto.getStockWarn() != null && dto.getStockWarn() >= dto.getStock()) {
            throw new RuntimeException("库存预警值不能大于等于实际库存");
        }
    }
}