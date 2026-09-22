package com.lxs.b2cmall.shop.vo;

import lombok.Data;

@Data
public class PayResult {
    private boolean success;
    private String message;
    private String payType;
    private String tradeNo;
    private Double amount;

    public static PayResult ok(String payType, String tradeNo, Double amount) {
        PayResult r = new PayResult();
        r.setSuccess(true);
        r.setMessage("支付成功");
        r.setPayType(payType);
        r.setTradeNo(tradeNo);
        r.setAmount(amount);
        return r;
    }

    public static PayResult fail(String message) {
        PayResult r = new PayResult();
        r.setSuccess(false);
        r.setMessage(message);
        return r;
    }
}