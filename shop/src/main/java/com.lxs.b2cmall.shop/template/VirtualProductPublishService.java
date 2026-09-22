package com.lxs.b2cmall.shop.template;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import org.springframework.stereotype.Component;

@Component("virtualProductPublish")
public class VirtualProductPublishService extends AbstractProductPublishTemplate {

    @Override
    protected void auditContent(ProductDTO dto) {
        // 虚拟商品：校验合规信息
        if (dto.getComplianceInfo() == null || dto.getComplianceInfo().trim().isEmpty()) {
            throw new RuntimeException("虚拟商品必须提供合规信息");
        }
    }
}