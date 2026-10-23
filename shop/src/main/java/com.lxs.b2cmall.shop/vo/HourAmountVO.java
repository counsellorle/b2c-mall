package com.lxs.b2cmall.shop.vo;

import lombok.Data;

/**
 * 某小时的支付金额汇总（Dashboard 24 小时趋势用）
 */
@Data
public class HourAmountVO {
    /** 小时，格式 "00" ~ "23" */
    private String hour;
    /** 该小时支付金额合计 */
    private double amount;
}
