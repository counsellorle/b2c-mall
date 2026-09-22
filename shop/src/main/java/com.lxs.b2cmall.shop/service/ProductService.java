package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.vo.PublishResult;

public interface ProductService {
    PublishResult publish(ProductDTO dto, Integer shopId);
}