package com.lxs.b2cmall.shop.dto;

import lombok.Data;

/**
 * 店铺订单查询条件（P3 订单查询，共 11 个筛选项）。
 *
 * 说明：订单表本身不存「订单类型 / 订单类别」，两者按订单内商品所属属性匹配
 * （订单中含任一满足条件的商品行即命中），避免再为订单表加冗余字段。
 */
@Data
public class OrderQueryDTO {

    /** 1. 订单编号（模糊） */
    private String orderNo;
    /** 2. 订单类型：按商品 product_type 匹配（physical / virtual / seckill 等） */
    private String orderType;
    /** 3. 订单类别：按商品 product_category 匹配（实物商品 / 虚拟商品 / 卡券） */
    private String orderCategory;
    /** 4. 订单状态：待付款 / 待发货 / 待收货 / 交易成功 / 交易失败 / 待退款 */
    private String status;
    /** 5. 收件人（模糊） */
    private String receiverName;
    /** 6. 收件人电话（模糊） */
    private String receiverPhone;
    /** 7. 支付方式：alipay / wechat / paypal / balance */
    private String payType;
    /** 8. 售后状态 */
    private String afterSaleStatus;
    /** 9. 物流类型：普通快递 / 同城配送 等 */
    private String deliveryType;
    /** 10. 门店（店铺 id） */
    private Integer shopId;
    /** 11. 下单时间区间（含端点，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss） */
    private String startTime;
    private String endTime;

    /** 页码，从 1 开始 */
    private Integer page = 1;
    /** 每页条数，默认 10 */
    private Integer pageSize = 10;

    /** 只传日期时补全到当天 00:00:00，避免字符串比较漏掉当天数据 */
    public String getStartTime() {
        return normalize(startTime, " 00:00:00");
    }

    /** 只传日期时补全到当天 23:59:59，保证区间右端点包含整天 */
    public String getEndTime() {
        return normalize(endTime, " 23:59:59");
    }

    public int getOffset() {
        int p = page == null || page < 1 ? 1 : page;
        int s = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (p - 1) * s;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private String normalize(String value, String timeSuffix) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        return v.length() == 10 ? v + timeSuffix : v;
    }
}
