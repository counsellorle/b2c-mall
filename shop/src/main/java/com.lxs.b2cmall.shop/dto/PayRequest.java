package com.lxs.b2cmall.shop.dto;

import lombok.Data;

@Data
public class PayRequest {
    private Integer orderId;
    private String orderNo;
    private Double amount;
    private String payType;
}