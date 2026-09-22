package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Category {
    private Integer id;
    private Integer parentId;
    private String name;
    private Integer level;
    private String type;
    private Integer sort;
    private String createdAt;
}