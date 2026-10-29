package com.lxs.b2cmall.shop.auth;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 解析当前 B 端登录员工所属店铺。
 * <p>
 * 身份由网关校验 token 后注入请求头（X-Shop-Id / X-Employee-Id），服务端不再信任前端传入的 shopId。
 * 直连 8082 调试（未经网关）时两个头为空，返回 null，便于本地联调。
 */
@Component
public class CurrentShop {

    public static final String HEADER_SHOP_ID = "X-Shop-Id";
    public static final String HEADER_EMPLOYEE_ID = "X-Employee-Id";

    /** 取当前店铺 id，未登录（未经网关）返回 null */
    public Integer shopId(HttpServletRequest request) {
        return parseInt(request == null ? null : request.getHeader(HEADER_SHOP_ID));
    }

    /** 取当前员工 id，未登录返回 null */
    public Integer employeeId(HttpServletRequest request) {
        return parseInt(request == null ? null : request.getHeader(HEADER_EMPLOYEE_ID));
    }

    /** 取当前店铺 id，缺失时抛业务异常 */
    public Integer requireShopId(HttpServletRequest request) {
        Integer shopId = shopId(request);
        if (shopId == null) {
            throw new RuntimeException("登录已过期，请重新登录");
        }
        return shopId;
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
