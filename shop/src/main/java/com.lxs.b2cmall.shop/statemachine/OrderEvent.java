package com.lxs.b2cmall.shop.statemachine;

public enum OrderEvent {
    PAY_SUCCESS,    // 支付成功
    SHIP,           // 发货
    CONFIRM,        // 确认收货
    CANCEL,         // 取消
    APPLY_REFUND    // 申请退款
}