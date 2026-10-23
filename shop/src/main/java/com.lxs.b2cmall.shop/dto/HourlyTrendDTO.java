package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import java.util.List;

/**
 * 24 小时实时交易趋势：今日 vs 昨日各小时支付金额
 */
@Data
public class HourlyTrendDTO {
    /** 固定 24 项（00:00 ~ 23:00），无数据的小时金额为 0 */
    private List<HourItem> items;

    @Data
    public static class HourItem {
        /** 小时标签，如 "00:00" */
        private String hour;
        /** 今日该小时支付金额 */
        private double todayAmount;
        /** 昨日该小时支付金额 */
        private double yesterdayAmount;
    }
}
