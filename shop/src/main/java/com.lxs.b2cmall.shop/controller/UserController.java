package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.auth.CurrentUser;
import com.lxs.b2cmall.shop.dto.UserLoginDTO;
import com.lxs.b2cmall.shop.dto.UserRegisterDTO;
import com.lxs.b2cmall.shop.entity.User;
import com.lxs.b2cmall.shop.service.UserService;
import com.lxs.b2cmall.shop.vo.Result;
import com.lxs.b2cmall.shop.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * C 端用户：注册 / 登录 / 登出。
 * 与商家员工（/employee/login）是两套完全独立的账号体系。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        try {
            return Result.success(userService.register(dto));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        try {
            return Result.success(userService.login(dto));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        userService.logout(currentUser.token(request));
        return Result.success("已退出登录");
    }
}
