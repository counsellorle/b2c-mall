package com.lxs.b2cmall.shop.dto;

import lombok.Data;

import java.util.List;

/**
 * 店铺商品列表查询条件（P2 商品管理）
 */
@Data
public class ProductQueryDTO {
    /** 商品名称（模糊） */
    private String name;
    /** 商品类型：普通商品 physical / 秒杀商品 seckill 等 */
    private String productType;
    /** 商品类别：实物商品 / 虚拟商品 / 卡券 */
    private String productCategory;
    /** 状态：on_sale 上架 / off_sale 下架 */
    private String status;
    /** 配送方式多选组合：同城配送 / 用户自提 / 快递配送（命中任一即返回） */
    private List<String> deliveryMethods;
    /** 商品分类（类目 id） */
    private Integer categoryId;

    /** 页码，从 1 开始 */
    private Integer page = 1;
    /** 每页条数，默认 10 */
    private Integer pageSize = 10;

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
