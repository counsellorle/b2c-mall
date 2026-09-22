package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardTrendDTO {
    private List<TrendItem> items;

    @Data
    public static class TrendItem {
        private String date;
        private long orderCount;
        private double amount;
    }
}