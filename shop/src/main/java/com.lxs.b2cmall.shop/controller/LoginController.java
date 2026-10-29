package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.dto.RegisterDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.service.EmployeeAuthService;
import com.lxs.b2cmall.shop.service.EmployeeService;
import com.lxs.b2cmall.shop.vo.EmployeeLoginVO;
import com.lxs.b2cmall.shop.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/employee")
public class LoginController {

    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_EMPLOYEE_TOKEN = "X-Employee-Token";
    private static final String BEARER = "bearer ";

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeAuthService employeeAuthService;

    /**
     * B 端登录：账号密码校验通过后签发 Redis 会话 token，网关据此鉴权并注入 shopId。
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@Valid @RequestBody LoginDTO loginDTO,
                                         HttpServletRequest request) {
        try {
            return Result.success(employeeAuthService.login(loginDTO, request));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Employee> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            Employee employee = employeeService.register(registerDTO);
            return Result.success(employee);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * B 端退出登录：删除 Redis 会话，token 立即失效。
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        try {
            employeeAuthService.logout(resolveToken(request));
            return Result.success("已退出登录");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader(HEADER_AUTH);
        if (StringUtils.hasText(auth)) {
            String trimmed = auth.trim();
            if (trimmed.length() > BEARER.length()
                    && trimmed.substring(0, BEARER.length()).toLowerCase().equals(BEARER)) {
                return trimmed.substring(BEARER.length()).trim();
            }
            return trimmed;
        }
        String header = request.getHeader(HEADER_EMPLOYEE_TOKEN);
        return header == null ? null : header.trim();
    }
}
