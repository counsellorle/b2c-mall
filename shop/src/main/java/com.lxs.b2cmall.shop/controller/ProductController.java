package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.ProductDTO;
import com.lxs.b2cmall.shop.service.ProductService;
import com.lxs.b2cmall.shop.vo.PublishResult;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/publish")
    public Result<PublishResult> publish(@Valid @RequestBody ProductDTO dto,
                                         @RequestParam Integer shopId) {
        try {
            PublishResult result = productService.publish(dto, shopId);
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}