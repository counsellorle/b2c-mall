package com.lxs.b2cmall.shop.support;

import com.lxs.b2cmall.shop.vo.OrderItemVO;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 订单展示用标签与脱敏（B 端订单查询与 C 端订单管理共用）。
 */
public final class OrderLabelSupport {

    private OrderLabelSupport() {
    }

    /** 手机号脱敏：138****8000 */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String p = phone.trim();
        if (p.length() <= 4) {
            return p;
        }
        if (p.length() >= 7) {
            return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
        }
        return p.charAt(0) + "****" + p.charAt(p.length() - 1);
    }

    /** 支付方式英文值 → 中文标签 */
    public static String payTypeLabel(String payType) {
        if (!StringUtils.hasText(payType)) {
            return "";
        }
        switch (payType.trim().toLowerCase(Locale.ROOT)) {
            case "alipay":
                return "支付宝";
            case "wechat":
            case "wechatpay":
                return "微信支付";
            case "paypal":
                return "贝宝";
            case "balance":
                return "余额支付";
            default:
                return payType;
        }
    }

    /** 商品类型标签：优先用商品类别，缺失时按 product_type 兜底 */
    public static String productTypeLabel(OrderItemVO item) {
        if (StringUtils.hasText(item.getProductCategory())) {
            return item.getProductCategory();
        }
        String type = item.getProductType();
        if (!StringUtils.hasText(type)) {
            return "";
        }
        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "physical":
                return "实物商品";
            case "virtual":
                return "虚拟商品";
            case "seckill":
                return "秒杀商品";
            default:
                return type;
        }
    }
}
