package com.lxs.b2cmall.shop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent {
    private Integer userId;
    private String loginIp;
    private String loginDevice;
    private String userAgent;
    private boolean success;
}