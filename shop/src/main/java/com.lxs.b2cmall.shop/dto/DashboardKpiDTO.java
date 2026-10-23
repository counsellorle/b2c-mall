package com.lxs.b2cmall.shop.dto;

import lombok.Data;

/**
 * 数据统计指标卡片（4 项核心经营数据，含同比昨日变化率）
 */
@Data
public class DashboardKpiDTO {

    /** 今日支付客户数（独立客户去重） */
    private long payUserCount;
    /** 昨日支付客户数 */
    private long yesterdayPayUserCount;
    /** 支付客户数同比昨日变化率（百分比，如 100.00 表示 +100%） */
    private double payUserRate;

    /** 今日支付订单数 */
    private long payOrderCount;
    /** 昨日支付订单数 */
    private long yesterdayPayOrderCount;
    /** 支付订单数同比昨日变化率 */
    private double payOrderRate;

    /** 今日退款金额（元） */
    private double refundAmount;
    /** 昨日退款金额（元） */
    private double yesterdayRefundAmount;
    /** 退款金额同比昨日变化率 */
    private double refundRate;

    /** 今日客单价（元）= 今日总支付金额 ÷ 支付订单数 */
    private double avgOrderPrice;
    /** 昨日客单价（元） */
    private double yesterdayAvgOrderPrice;
    /** 客单价同比昨日变化率 */
    private double avgOrderPriceRate;
}
