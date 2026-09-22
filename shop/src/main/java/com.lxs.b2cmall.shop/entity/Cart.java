package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Cart {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer quantity;
    private String createTime;
}