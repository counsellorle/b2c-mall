package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.dto.UserLoginDTO;
import com.lxs.b2cmall.shop.dto.UserRegisterDTO;
import com.lxs.b2cmall.shop.entity.User;
import com.lxs.b2cmall.shop.vo.UserLoginVO;

public interface UserService {

    /** 注册：用户名唯一，密码 BCrypt 加密存储，返回脱敏后的用户信息 */
    User register(UserRegisterDTO dto);

    /** 登录：校验密码与账号状态，成功后签发 token 写入会话存储 */
    UserLoginVO login(UserLoginDTO dto);

    /** 登出：删除 token */
    void logout(String token);
}
