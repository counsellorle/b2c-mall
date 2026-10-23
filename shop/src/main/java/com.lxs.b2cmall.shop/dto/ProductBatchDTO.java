package com.lxs.b2cmall.shop.dto;

import lombok.Data;

import java.util.List;

/**
 * 商品批量修改请求：批量修改状态、配送方式、分类、排序、库存等。
 * 仅非 null 字段会被更新。
 */
@Data
public class ProductBatchDTO {
    /** 目标商品 id 列表（必填） */
    private List<Integer> ids;

    /** 批量上架/下架：on_sale / off_sale */
    private String status;
    /** 批量修改配送方式，逗号分隔：同城配送,用户自提,快递配送 */
    private String deliveryMethods;
    /** 批量修改商品分类 */
    private Integer categoryId;
    /** 批量修改排序（数字越大前台展示越靠前） */
    private Integer sort;
    /** 批量修改库存（销售库存） */
    private Integer stock;
    /** 批量修改仓库库存 */
    private Integer warehouseStock;
    /** 批量修改门店库存 */
    private Integer storeStock;

    /** 更新时间（服务端填充） */
    private String updateTime;
}
