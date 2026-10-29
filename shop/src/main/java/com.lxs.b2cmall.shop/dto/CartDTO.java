package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class CartDTO {
    /**
     * 用户 ID：由服务端从登录态（网关注入的 X-User-Id / token）填充，
     * 前端传入的值会被覆盖，因此不再作为必填校验项。
     */
    private Integer userId;

    @NotNull(message = "商品ID不能为空")
    private Integer productId;

    @Positive(message = "数量必须大于0")
    private Integer quantity = 1;
}