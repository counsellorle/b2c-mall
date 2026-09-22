package com.lxs.b2cmall.shop.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class RegisterDTO {
    @NotBlank(message = "店铺名不能为空")
    private String shopName;

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}