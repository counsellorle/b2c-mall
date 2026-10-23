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
    private Integer realSales;       // 真实成交销量
    private Integer fakeSales;       // 注水销量（虚拟销量，商家可配置）
    private Integer warehouseStock;  // 仓库（中心仓）库存
    private Integer storeStock;      // 门店可售库存
    private String deliveryMethods;  // 支持的配送方式，逗号分隔：同城配送,用户自提,快递配送
    private String productCategory;  // 商品类别：实物商品/虚拟商品/卡券
    private String productCode;
    private String status;
    private Integer sort;
    private String complianceInfo;
    private String createTime;
    private String updateTime;
}