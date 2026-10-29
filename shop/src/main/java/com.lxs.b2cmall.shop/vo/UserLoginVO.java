package com.lxs.b2cmall.shop.vo;

import lombok.Data;

/**
 * C 端登录结果：token 由前端保存，后续请求通过 Authorization: Bearer {token} 携带。
 */
@Data
public class UserLoginVO {
    private String token;
    private Integer userId;
    private String username;
    private String nickname;
    private String phone;
}
