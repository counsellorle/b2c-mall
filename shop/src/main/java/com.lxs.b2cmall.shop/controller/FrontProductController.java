package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.entity.Category;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.service.FrontProductService;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/front")
public class FrontProductController {

    @Autowired
    private FrontProductService frontProductService;

    @GetMapping("/products")
    public Result<List<Product>> list(@RequestParam(required = false) Integer categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "default") String sort) {
        return Result.success(frontProductService.listProducts(categoryId, keyword, sort));
    }

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.success(frontProductService.listCategories());
    }

    @GetMapping("/product/{id}")
    public Result<Product> detail(@PathVariable Integer id) {
        Product product = frontProductService.getProduct(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }
}