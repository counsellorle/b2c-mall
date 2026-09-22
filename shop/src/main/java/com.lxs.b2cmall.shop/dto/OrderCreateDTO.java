package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class OrderCreateDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "店铺ID不能为空")
    private Integer shopId;

    @NotBlank(message = "收货人不能为空")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String receiverAddress;

    @NotBlank(message = "支付方式不能为空")
    private String payType;  // alipay / wechat / paypal / other

    private String remark;

    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "商品ID不能为空")
        private Integer productId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}