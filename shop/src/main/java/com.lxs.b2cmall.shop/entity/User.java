package com.lxs.b2cmall.shop.entity;

import lombok.Data;

/**
 * C 端用户（买家）。与商家员工 tb_employee 物理隔离，两套账号体系互不影响。
 */
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private Integer status;
    private String createdAt;
}
