package com.lxs.b2cmall.shop.vo;

import com.lxs.b2cmall.shop.entity.Product;
import lombok.Data;

@Data
public class PublishResult {
    private boolean success;
    private String message;
    private Product product;

    public static PublishResult ok(Product product) {
        PublishResult r = new PublishResult();
        r.setSuccess(true);
        r.setMessage("发布成功");
        r.setProduct(product);
        return r;
    }

    public static PublishResult fail(String message) {
        PublishResult r = new PublishResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }
}