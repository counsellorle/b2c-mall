package com.lxs.b2cmall.shop.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量发货入参
 */
@Data
public class BatchShipDTO {
    /** 待发货订单 id 列表 */
    private List<Integer> orderIds;
    /** 操作人（登录后应由会话注入，缺省 admin） */
    private String operator;
    /** 可选：同时更新物流类型，如「普通快递」「同城配送」 */
    private String deliveryType;
}
