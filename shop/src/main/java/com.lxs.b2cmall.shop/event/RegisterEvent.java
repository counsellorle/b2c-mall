package com.lxs.b2cmall.shop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEvent {
    private Integer shopId;
    private Integer employeeId;
    private String shopName;
    private String username;
}