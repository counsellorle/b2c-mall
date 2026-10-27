package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class Order {
    private Integer id;
    private String orderNo;
    private Integer userId;
    private Integer shopId;
    private Double totalAmount;
    private Double payAmount;
    private String status;
    private String payType;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String shippingStatus;
    private String afterSaleStatus;
    private Integer isDeleted;      // 逻辑删除：0 正常，1 已删除（C 端删除仅隐藏）
    private String deliveryType;    // 物流类型：普通快递/同城配送等
    private String createTime;
    private String payTime;
    private String shipTime;
    /** 非表字段：联表 tb_shop 填充，供订单列表展示门店名 */
    private String shopName;
}