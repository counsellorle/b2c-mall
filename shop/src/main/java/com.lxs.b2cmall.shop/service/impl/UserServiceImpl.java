package com.lxs.b2cmall.shop.service.impl;

import com.lxs.b2cmall.shop.auth.UserTokenStore;
import com.lxs.b2cmall.shop.dto.UserLoginDTO;
import com.lxs.b2cmall.shop.dto.UserRegisterDTO;
import com.lxs.b2cmall.shop.entity.User;
import com.lxs.b2cmall.shop.mapper.UserMapper;
import com.lxs.b2cmall.shop.service.UserService;
import com.lxs.b2cmall.shop.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    /** 登录态有效期：7 天 */
    private static final long TOKEN_TTL_SECONDS = 7 * 24 * 3600L;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTokenStore userTokenStore;

    @Override
    public User register(UserRegisterDTO dto) {
        User existing = userMapper.findByUsername(dto.getUsername());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now().format(FMT));
        userMapper.insert(user);

        user.setPassword(null);
        return user;
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        userTokenStore.save(token, user.getId(), TOKEN_TTL_SECONDS);

        UserLoginVO vo = new UserLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        return vo;
    }

    @Override
    public void logout(String token) {
        userTokenStore.remove(token);
    }
}
