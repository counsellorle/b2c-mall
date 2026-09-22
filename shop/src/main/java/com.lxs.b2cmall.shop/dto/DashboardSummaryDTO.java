package com.lxs.b2cmall.shop.dto;

import lombok.Data;

@Data
public class DashboardSummaryDTO {
    private long pendingPayment;   // 待付款
    private long pendingShipment;  // 待发货
    private long shipped;          // 待收货
    private long completed;        // 交易成功
    private long failed;           // 交易失败
    private long pendingRefund;    // 待退款
    private double todayPaymentAmount;
    private double monthlyPaymentAmount;
    private long totalOrders;
}