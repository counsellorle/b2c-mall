package com.lxs.b2cmall.shop.vo;

import lombok.Data;

/**
 * B 端登录结果：token 由网关校验，店铺信息用于前端顶栏展示。
 */
@Data
public class EmployeeLoginVO {

    private String token;

    private Integer employeeId;

    private Integer shopId;

    private String username;

    private String avatarUrl;

    private String shopName;

    /** 登录态有效期（秒） */
    private Long expiresIn;
}
