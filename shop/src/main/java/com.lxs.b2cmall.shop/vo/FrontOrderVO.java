package com.lxs.b2cmall.shop.vo;

import lombok.Data;

import java.util.List;

/**
 * C 端订单卡片（订单组）：一单一行，内含商品行。
 */
@Data
public class FrontOrderVO {

    private Integer id;
    private String orderNo;
    private Integer shopId;
    private String shopName;

    private String createTime;
    private String payTime;
    private String shipTime;

    private String status;
    private String afterSaleStatus;

    /** 支付方式原值与中文标签 */
    private String payType;
    private String payTypeLabel;

    /** 物流：配送方式、物流状态 */
    private String deliveryType;
    private String shippingStatus;

    private Double totalAmount;
    private Double payAmount;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private List<OrderItemVO> items;
    /** 商品总件数 */
    private Integer itemCount;
    /** 商品种类数 */
    private Integer itemKinds;

    /** 是否可删除（订单已终态才允许，进行中订单不允许用户删除） */
    private Boolean canDelete;
    /** 是否可复购（已支付过的订单才提供复购入口） */
    private Boolean canRebuy;

    /** 详情页使用：状态流转记录（列表接口不填充） */
    private List<com.lxs.b2cmall.shop.entity.OrderStatusLog> statusLogs;
}
