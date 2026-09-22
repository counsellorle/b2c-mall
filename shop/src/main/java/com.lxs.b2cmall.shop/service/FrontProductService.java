package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.entity.Category;
import com.lxs.b2cmall.shop.entity.Product;

import java.util.List;

public interface FrontProductService {
    List<Product> listProducts(Integer categoryId, String keyword, String sort);
    List<Category> listCategories();
    Product getProduct(Integer id);
}