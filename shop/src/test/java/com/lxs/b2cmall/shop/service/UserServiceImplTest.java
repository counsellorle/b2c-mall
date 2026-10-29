package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.auth.UserTokenStore;
import com.lxs.b2cmall.shop.dto.UserLoginDTO;
import com.lxs.b2cmall.shop.dto.UserRegisterDTO;
import com.lxs.b2cmall.shop.entity.User;
import com.lxs.b2cmall.shop.mapper.UserMapper;
import com.lxs.b2cmall.shop.service.impl.UserServiceImpl;
import com.lxs.b2cmall.shop.vo.UserLoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P5 用户注册登录：
 * 1. 注册密码 BCrypt 落库、用户名唯一、昵称兜底
 * 2. 登录三种失败（不存在/密码错/禁用）与成功签发 token
 * 3. 登出删除 token
 */
class UserServiceImplTest {

    private UserMapper userMapper;
    private UserTokenStore tokenStore;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = Mockito.mock(UserMapper.class);
        tokenStore = Mockito.mock(UserTokenStore.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "userTokenStore", tokenStore);
    }

    private User existingUser(String rawPassword, int status) {
        User u = new User();
        u.setId(7);
        u.setUsername("buyer");
        u.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        u.setNickname("买家");
        u.setStatus(status);
        return u;
    }

    @Test
    void registerHashesPasswordAndDefaultsNickname() {
        when(userMapper.findByUsername("newbie")).thenReturn(null);
        // service 返回前会把入参对象的 password 置空，因此在 insert 调用时立刻校验落库值
        doAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertNotEquals("123456", saved.getPassword(), "密码不能明文落库");
            assertTrue(BCrypt.checkpw("123456", saved.getPassword()));
            assertEquals("newbie", saved.getNickname(), "未传昵称时用用户名兜底");
            assertEquals(1, saved.getStatus());
            return null;
        }).when(userMapper).insert(any(User.class));

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newbie");
        dto.setPassword("123456");
        User created = service.register(dto);

        assertNull(created.getPassword(), "返回体不能带密码");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userMapper.findByUsername("buyer")).thenReturn(existingUser("123456", 1));

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("buyer");
        dto.setPassword("123456");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.register(dto));
        assertEquals("用户名已存在", e.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginSuccessIssuesToken() {
        when(userMapper.findByUsername("buyer")).thenReturn(existingUser("123456", 1));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("buyer");
        dto.setPassword("123456");
        UserLoginVO vo = service.login(dto);

        assertNotNull(vo.getToken());
        assertEquals(32, vo.getToken().length(), "token 用 32 位无横线 UUID");
        assertEquals(7, vo.getUserId());
        assertEquals("buyer", vo.getUsername());
        verify(tokenStore).save(eq(vo.getToken()), eq(7), eq(7 * 24 * 3600L));
    }

    @Test
    void loginFailsOnUnknownUser() {
        when(userMapper.findByUsername("ghost")).thenReturn(null);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("ghost");
        dto.setPassword("123456");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.login(dto));
        assertEquals("账号不存在", e.getMessage());
        verify(tokenStore, never()).save(anyString(), anyInt(), anyLong());
    }

    @Test
    void loginFailsOnWrongPassword() {
        when(userMapper.findByUsername("buyer")).thenReturn(existingUser("123456", 1));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("buyer");
        dto.setPassword("bad-password");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.login(dto));
        assertEquals("密码错误", e.getMessage());
        verify(tokenStore, never()).save(anyString(), anyInt(), anyLong());
    }

    @Test
    void loginFailsWhenDisabled() {
        when(userMapper.findByUsername("buyer")).thenReturn(existingUser("123456", 0));

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("buyer");
        dto.setPassword("123456");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.login(dto));
        assertEquals("账号已被禁用", e.getMessage());
    }

    @Test
    void logoutRemovesToken() {
        service.logout("abc");
        verify(tokenStore).remove("abc");
    }
}
