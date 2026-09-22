package com.lxs.b2cmall.shop.vo;

import lombok.Data;

@Data
public class OrderTrendVO {
    private String date;
    private long orderCount;
    private double amount;
}