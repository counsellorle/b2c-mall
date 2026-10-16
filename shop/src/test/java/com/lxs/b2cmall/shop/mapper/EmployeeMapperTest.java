package com.lxs.b2cmall.shop.mapper;

import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.LoginFixtures;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 员工表的数据库访问测试（Mapper 层）。
 *
 * 测查/改员工表能不能正常工作：字段映射、查不到返回 null、密码加密存储、用户名唯一。
 * 必须用真数据库——字段映射错了不会报错，只会悄悄变成 null（不启动 Spring）。
 * 带 @Disabled 的是"故意关掉的红灯"，对应还没修的 bug。
 */
class EmployeeMapperTest {

    private Connection connection;
    private SqlSession session;
    private EmployeeMapper employeeMapper;

    @BeforeEach
    void setUp() {
        connection = InMemorySqlite.open();
        session = SqliteMapperSupport.factory(connection).openSession(true);
        employeeMapper = session.getMapper(EmployeeMapper.class);
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception ignored) {
            // 关闭失败不影响结论
        }
    }

    @Test
    @DisplayName("数据库的 shop_id 能正确映射成 Java 的 shopId（漏映射会悄悄变成 null）")
    void mapsUnderscoreColumnsToCamelCase() throws Exception {
        InMemorySqlite.insertEmployee(connection, 7, "shop01",
                BCrypt.hashpw("123456", BCrypt.gensalt()), 1);

        Employee found = employeeMapper.findByUsername("shop01");

        assertThat(found).isNotNull();
        assertThat(found.getShopId()).isEqualTo(7);
        assertThat(found.getAvatarUrl()).isEqualTo("/avatars/default.png");
        assertThat(found.getLoginCount()).isEqualTo(0);
        assertThat(found.getStatus()).isEqualTo(1);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("查一个不存在的用户名 → 返回 null，而不是抛异常")
    void returnsNullForUnknownUsername() {
        assertThat(employeeMapper.findByUsername("__not_exist__")).isNull();
    }

    @Test
    @DisplayName("密码列里存的是加密后的串，绝不是明文")
    void passwordColumnHoldsBcryptHash() throws Exception {
        String raw = "123456";
        InMemorySqlite.insertEmployee(connection, 1, "shop01",
                BCrypt.hashpw(raw, BCrypt.gensalt()), 1);

        Employee found = employeeMapper.findByUsername("shop01");

        assertThat(found.getPassword()).isNotEqualTo(raw);
        assertThat(found.getPassword()).startsWith("$2a$");
        assertThat(found.getPassword()).hasSize(60);
    }

    @Test
    @DisplayName("updateLoginInfo 能把登录次数加 1，并写入最后登录时间")
    void updateLoginInfoIncrementsCountAndStampsTime() throws Exception {
        int id = InMemorySqlite.insertEmployee(connection, 1, "shop01",
                BCrypt.hashpw("123456", BCrypt.gensalt()), 1);

        employeeMapper.updateLoginInfo(id);
        employeeMapper.updateLoginInfo(id);

        // 次数是累加到 2，不是覆盖成 1
        Employee found = employeeMapper.findByUsername("shop01");
        assertThat(found.getLoginCount()).isEqualTo(2);
        assertThat(found.getLastLoginTime()).isNotNull();
    }

    @Test
    @Disabled("待修复缺陷 B10：tb_employee.username 没有唯一索引，当前可以插入两条同名记录")
    @DisplayName("【红灯】用户名必须有唯一约束（同名的第二条插不进去）")
    void usernameMustBeUnique() {
        employeeMapper.insert(LoginFixtures.employee(0, "dup_user", "123456", 1));

        assertThatThrownBy(() -> employeeMapper.insert(
                LoginFixtures.employee(0, "dup_user", "123456", 1)))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("现状：没有唯一索引，能插两条同名记录，而且查用户名时直接报 TooManyResultsException（缺陷 B10 的证据）")
    void duplicateUsernameCurrentlyAllowedAndBreaksLookup() throws Exception {
        employeeMapper.insert(LoginFixtures.employee(0, "dup_user", "123456", 1));
        employeeMapper.insert(LoginFixtures.employee(0, "dup_user", "123456", 1));

        assertThat(InMemorySqlite.countRows(connection, "tb_employee")).isEqualTo(2);

        // 后果比"取第一条"更严重：一旦有重名账号，这个用户名的登录会直接报错
        assertThatThrownBy(() -> employeeMapper.findByUsername("dup_user"))
                .isInstanceOf(org.apache.ibatis.exceptions.TooManyResultsException.class);
    }
}
