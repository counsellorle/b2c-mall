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
    private String createTime;
    private String payTime;
    private String shipTime;
}