package com.lxs.b2cmall.shop.vo;

import lombok.Data;

import java.util.List;

/**
 * 订单组：订单头 + 商品行列表，店铺订单查询页的一行
 */
@Data
public class OrderGroupVO {
    private Integer id;
    private String orderNo;
    private Integer shopId;
    private String shopName;

    private String createTime;
    private String payTime;
    private String shipTime;

    private String status;
    private String payType;
    private String payTypeLabel;
    private String afterSaleStatus;
    private String deliveryType;
    private String shippingStatus;

    private Double totalAmount;
    private Double payAmount;
    /** 商品总件数（各行 quantity 之和） */
    private Integer itemCount;
    /** 商品种类数（商品行条数） */
    private Integer itemKinds;

    private String receiverName;
    /** 脱敏手机号：138****8000 */
    private String receiverPhone;
    private String receiverAddress;

    private List<OrderItemVO> items;
}
