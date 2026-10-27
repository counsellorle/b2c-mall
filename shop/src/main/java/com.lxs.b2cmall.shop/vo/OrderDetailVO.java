package com.lxs.b2cmall.shop.vo;

import com.lxs.b2cmall.shop.entity.OrderStatusLog;
import lombok.Data;

import java.util.List;

/**
 * 店铺订单详情：订单组（含商品行/物流/支付信息）+ 状态流转日志
 */
@Data
public class OrderDetailVO {
    private OrderGroupVO order;
    private List<OrderStatusLog> statusLogs;
}
