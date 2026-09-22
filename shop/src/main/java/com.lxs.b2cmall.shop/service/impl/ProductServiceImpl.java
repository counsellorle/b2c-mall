package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.service.ProductService;
import com.lxs.b2cmall.shop.template.AbstractProductPublishTemplate;
import com.lxs.b2cmall.shop.template.ProductPublishFactory;
import com.lxs.b2cmall.shop.vo.PublishResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductPublishFactory productPublishFactory;

    @Override
    public PublishResult publish(ProductDTO dto, Integer shopId) {
        AbstractProductPublishTemplate template = productPublishFactory.getStrategy(dto.getProductType());
        return template.publish(dto, shopId);
    }
}