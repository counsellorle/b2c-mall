package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Product {
    private Integer id;
    private Integer shopId;
    private String name;
    private String keyword;
    private String sellingPoint;
    private Integer categoryId;
    private Integer shopCategoryId;
    private String productType;
    private String mainImage;
    private String video;
    private String brand;
    private Double price;
    private Double marketPrice;
    private Integer stock;
    private Integer stockWarn;
    private String productCode;
    private String status;
    private Integer sort;
    private String complianceInfo;
    private String createTime;
    private String updateTime;
}