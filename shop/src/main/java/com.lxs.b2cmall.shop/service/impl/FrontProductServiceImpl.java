package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.entity.Category;
import com.lxs.b2cmall.shop.entity.Product;
import com.lxs.b2cmall.shop.mapper.CategoryMapper;
import com.lxs.b2cmall.shop.mapper.ProductMapper;
import com.lxs.b2cmall.shop.service.FrontProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FrontProductServiceImpl implements FrontProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<Product> listProducts(Integer categoryId, String keyword, String sort) {
        // 构建缓存 key
        String cacheKey = "front:products:" + categoryId + ":" + keyword + ":" + sort;
        List<Product> cached = (List<Product>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Product> products = productMapper.findByCondition(categoryId, keyword, sort);
        redisTemplate.opsForValue().set(cacheKey, products, 60, TimeUnit.SECONDS);
        return products;
    }

    @Override
    public List<Category> listCategories() {
        String cacheKey = "front:categories";
        List<Category> cached = (List<Category>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Category> categories = categoryMapper.findAll();
        redisTemplate.opsForValue().set(cacheKey, categories, 300, TimeUnit.SECONDS);
        return categories;
    }

    @Override
    public Product getProduct(Integer id) {
        String cacheKey = "front:product:" + id;
        Product cached = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 直接用 findByCondition 不合适，新增一个按 id 查的方法
        // 这里直接查数据库
        List<Product> all = productMapper.findByCondition(null, null, "default");
        for (Product p : all) {
            if (p.getId().equals(id)) {
                redisTemplate.opsForValue().set(cacheKey, p, 60, TimeUnit.SECONDS);
                return p;
            }
        }
        return null;
    }
}