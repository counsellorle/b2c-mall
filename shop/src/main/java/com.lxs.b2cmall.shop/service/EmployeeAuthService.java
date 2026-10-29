package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.auth.EmployeeTokenStore;
import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.entity.Shop;
import com.lxs.b2cmall.shop.mapper.ShopMapper;
import com.lxs.b2cmall.shop.vo.EmployeeLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * B 端登录门户：在 {@link EmployeeService#login} 的账号密码校验之上，
 * 签发 Redis 会话 token，供网关统一鉴权并注入 shopId。
 */
@Service
public class EmployeeAuthService {

    /** B 端登录态有效期：8 小时（后台会话比买家端更短） */
    private static final long TOKEN_TTL_SECONDS = 8 * 3600L;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeTokenStore employeeTokenStore;

    @Autowired
    private ShopMapper shopMapper;

    public EmployeeLoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        Employee employee = employeeService.login(loginDTO, request);

        String token = UUID.randomUUID().toString().replace("-", "");
        employeeTokenStore.save(token, employee.getShopId(), employee.getId(), TOKEN_TTL_SECONDS);

        EmployeeLoginVO vo = new EmployeeLoginVO();
        vo.setToken(token);
        vo.setEmployeeId(employee.getId());
        vo.setShopId(employee.getShopId());
        vo.setUsername(employee.getUsername());
        vo.setAvatarUrl(employee.getAvatarUrl());
        vo.setExpiresIn(TOKEN_TTL_SECONDS);

        if (employee.getShopId() != null) {
            Shop shop = shopMapper.findById(employee.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getShopName());
            }
        }
        return vo;
    }

    public void logout(String token) {
        employeeTokenStore.remove(token);
    }
}
