package com.lxs.b2cmall.shop.entity;

import lombok.Data;

@Data
public class LoginLog {
    private Integer id;
    private Integer userId;
    private String loginIp;
    private String loginDevice;
    private String loginLocation;
    private String loginTime;
    private String userAgent;
    private Integer status;
}