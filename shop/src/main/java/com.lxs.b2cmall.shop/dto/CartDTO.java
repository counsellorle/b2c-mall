package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class CartDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotNull(message = "商品ID不能为空")
    private Integer productId;

    @Positive(message = "数量必须大于0")
    private Integer quantity = 1;
}