package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class OrderStatusLog {
    private Integer id;
    private Integer orderId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String remark;
    private String createTime;
}