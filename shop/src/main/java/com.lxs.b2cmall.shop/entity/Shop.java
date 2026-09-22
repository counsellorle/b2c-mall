package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Shop {
    private Integer id;
    private String shopName;
    private String adminAccount;
    private String adminPassword;
    private String logoUrl;
    private Integer status;
    private String createdAt;
    private String updatedAt;
}