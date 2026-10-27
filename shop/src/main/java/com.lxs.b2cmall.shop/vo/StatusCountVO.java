package com.lxs.b2cmall.shop.vo;

import lombok.Data;

/**
 * 订单状态分组计数（状态 Tab 用）
 */
@Data
public class StatusCountVO {
    private String status;
    private Long cnt;
}
