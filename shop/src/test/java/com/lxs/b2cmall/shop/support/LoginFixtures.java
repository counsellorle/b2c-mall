package com.lxs.b2cmall.shop.support;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.lang.reflect.Field;

/**
 * 登录相关测试的造数据与注入工具。
 *
 * 对应 .tdd/task/login-tdd-tasks.md，用于让 Service 层可以在纯内存环境下被测试，
 * 不依赖 Spring 上下文、Nacos、Redis 或真实 SQLite 文件。
 */
public final class LoginFixtures {

    private static final String NOW = "2026-10-13 15:00:00";

    private LoginFixtures() {
    }

    /** 构造一个密码已经 BCrypt 加密过的员工记录，模拟从库里查出来的样子。 */
    public static Employee employee(int id, String username, String rawPassword, Integer status) {
        Employee e = new Employee();
        e.setId(id);
        e.setShopId(1);
        e.setUsername(username);
        e.setPassword(rawPassword == null ? null : BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        e.setAvatarUrl("/avatars/default.png");
        e.setLoginCount(0);
        e.setStatus(status);
        e.setCreatedAt(NOW);
        e.setUpdatedAt(NOW);
        return e;
    }

    public static LoginDTO loginDTO(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    /** 构造带 IP / User-Agent 的请求，用于验证审计日志里的来源信息。 */
    public static MockHttpServletRequest request(String remoteAddr, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        request.addHeader("User-Agent", userAgent);
        return request;
    }

    /**
     * 把测试替身注入到私有 @Autowired 字段。
     *
     * 生产代码目前用字段注入，无法从外部构造。为了不改动生产代码，这里用反射兜底。
     * 待 .tdd/task/login-tdd-tasks.md「重构方向 2」落地（Service 改为构造注入）后，
     * 本方法可以整体删除，改为直接 new。
     */
    public static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    "无法注入测试替身: " + target.getClass().getSimpleName() + "#" + fieldName, ex);
        }
    }
}
