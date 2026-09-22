package com.lxs.b2cmall.shop.statemachine;

public enum OrderStatus {
    PENDING_PAYMENT("待付款"),
    PENDING_SHIPMENT("待发货"),
    SHIPPED("待收货"),
    COMPLETED("交易成功"),
    FAILED("交易失败"),
    PENDING_REFUND("待退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}