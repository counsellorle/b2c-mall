package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Employee {
    private Integer id;
    private Integer shopId;
    private String username;
    private String password;
    private String avatarUrl;
    private String lastLoginTime;
    private Integer loginCount;
    private Integer status;
    private String createdAt;
    private String updatedAt;
}