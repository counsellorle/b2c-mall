package com.lxs.b2cmall.shop.vo;

import lombok.Data;

/**
 * 订单商品行（含商品类型标签，供订单组展示）
 */
@Data
public class OrderItemVO {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private String productName;
    private String productImage;
    private Double price;
    private Integer quantity;
    private Double subtotal;
    /** 商品类型：physical / virtual / seckill（联 tb_product 得到，可能为 null） */
    private String productType;
    /** 商品类别：实物商品 / 虚拟商品 / 卡券 */
    private String productCategory;
    /** 展示用类型标签（服务层填充） */
    private String productTypeLabel;
}
