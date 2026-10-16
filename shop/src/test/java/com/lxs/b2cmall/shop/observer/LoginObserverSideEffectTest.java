package com.lxs.b2cmall.shop.observer;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.event.LoginEvent;
import com.lxs.b2cmall.shop.mapper.EmployeeMapper;
import com.lxs.b2cmall.shop.mapper.LoginLogMapper;
import com.lxs.b2cmall.shop.service.impl.EmployeeServiceImpl;
import com.lxs.b2cmall.shop.support.InMemorySqlite;
import com.lxs.b2cmall.shop.support.LoginFixtures;
import com.lxs.b2cmall.shop.support.SqliteMapperSupport;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 登录之后"数据库里到底写了什么"的测试。
 *
 * 登录成功后，监听器会悄悄往数据库写审计日志、更新登录次数。这些动作看不到返回值，
 * 所以要连真的内存数据库来验证（不启动 Spring）。
 * 带 @Disabled 的是"故意关掉的红灯"，对应还没修的 bug。
 */
class LoginObserverSideEffectTest {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    private Connection connection;
    private SqlSession session;
    private EmployeeMapper employeeMapper;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        connection = InMemorySqlite.open();
        session = SqliteMapperSupport.factory(connection).openSession(true);

        employeeMapper = session.getMapper(EmployeeMapper.class);
        LoginLogMapper loginLogMapper = session.getMapper(LoginLogMapper.class);

        // 真实的监听器 + 真实的 Mapper
        AuditLogObserver auditLogObserver = new AuditLogObserver();
        LoginFixtures.inject(auditLogObserver, "loginLogMapper", loginLogMapper);

        UserStatusObserver userStatusObserver = new UserStatusObserver();
        LoginFixtures.inject(userStatusObserver, "employeeMapper", employeeMapper);

        // 简单的发布器：发什么事件就直接调对应的监听器，模拟 Spring 的同步事件
        ApplicationEventPublisher publisher = event -> {
            LoginEvent loginEvent = (LoginEvent) event;
            auditLogObserver.onLoginEvent(loginEvent);
            userStatusObserver.onLoginEvent(loginEvent);
        };

        service = new EmployeeServiceImpl();
        LoginFixtures.inject(service, "employeeMapper", employeeMapper);
        LoginFixtures.inject(service, "eventPublisher", publisher);
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

    /** 往库里插一个员工，返回它的 id。 */
    private int insertEmployee(String username, String rawPassword, int status) throws SQLException {
        return InMemorySqlite.insertEmployee(connection, 1, username,
                BCrypt.hashpw(rawPassword, BCrypt.gensalt()), status);
    }

    private LoginDTO loginDTO(String username, String password) {
        return LoginFixtures.loginDTO(username, password);
    }

    private int countLoginLogs() throws SQLException {
        return InMemorySqlite.countRows(connection, "tb_login_logs");
    }

    /** 取出所有审计日志的 status（1=成功，0=失败）。 */
    private List<Integer> loginLogStatuses() throws SQLException {
        List<Integer> statuses = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT status FROM tb_login_logs ORDER BY id")) {
            while (rs.next()) {
                statuses.add(rs.getInt("status"));
            }
        }
        return statuses;
    }

    private Integer loginLogUserId() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT user_id FROM tb_login_logs ORDER BY id LIMIT 1")) {
            return rs.next() ? rs.getInt("user_id") : null;
        }
    }

    @Test
    @DisplayName("登录成功 2 次 → 登录次数变 2，且各写 1 条「成功」日志（不多不少）")
    void successfulLoginUpdatesStatsAndWritesOneLog() throws Exception {
        int id = insertEmployee("shop01", "123456", 1);

        service.login(loginDTO("shop01", "123456"), LoginFixtures.request("1.2.3.4", UA));
        service.login(loginDTO("shop01", "123456"), LoginFixtures.request("1.2.3.4", UA));

        Employee found = employeeMapper.findByUsername("shop01");
        assertThat(found.getLoginCount()).isEqualTo(2);
        assertThat(found.getLastLoginTime()).isNotNull();

        // 每次 1 条。如果监听器被注册了两遍，这里会变成 4 条
        assertThat(countLoginLogs()).isEqualTo(2);
        assertThat(loginLogStatuses()).containsExactly(1, 1);
        assertThat(loginLogUserId()).isEqualTo(id);
    }

    @Test
    @DisplayName("密码错误 → 写 1 条「失败」日志，且 user_id 是真实用户 id")
    void wrongPasswordWritesSingleFailureLogWithRealUserId() throws Exception {
        int id = insertEmployee("shop01", "123456", 1);

        assertThatThrownBy(() -> service.login(
                loginDTO("shop01", "wrong-password"), LoginFixtures.request("1.2.3.4", UA)))
                .hasMessage("密码错误");

        assertThat(countLoginLogs()).isEqualTo(1);
        assertThat(loginLogStatuses()).containsExactly(0);
        assertThat(loginLogUserId()).isEqualTo(id);
    }

    @Test
    @DisplayName("现状：审计日志的登录地点目前写死成「待解析」（缺陷 B8 的证据）")
    void loginLocationIsCurrentlyHardcoded() throws Exception {
        insertEmployee("shop01", "123456", 1);

        service.login(loginDTO("shop01", "123456"), LoginFixtures.request("1.2.3.4", UA));

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT login_location FROM tb_login_logs LIMIT 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("login_location")).isEqualTo("待解析");
        }
    }

    @Test
    @Disabled("待修复缺陷 B2：status != 1 分支直接 throw，没有发事件，被禁账号的尝试零留痕")
    @DisplayName("【红灯】禁用账号的登录尝试也必须写一条审计日志")
    void disabledAccountAttemptMustBeAudited() throws Exception {
        insertEmployee("shop01", "123456", 0);

        assertThatThrownBy(() -> service.login(
                loginDTO("shop01", "123456"), LoginFixtures.request("1.2.3.4", UA)))
                .hasMessage("账号已被禁用");

        assertThat(countLoginLogs()).isEqualTo(1);
    }

    @Test
    @Disabled("待修复缺陷 B1：监听器拿 userId=null 去插 tb_login_logs.user_id（NOT NULL）会抛约束异常，"
            + "而且事件是同步的，异常会在抛「账号不存在」之前先冒出来，前端收到的是数据库错误")
    @DisplayName("【红灯】账号不存在：真实监听器参与时，仍应报「账号不存在」且不写垃圾日志")
    void accountNotFoundSurvivesRealListener() throws Exception {
        assertThatThrownBy(() -> service.login(
                loginDTO("__not_exist__", "123456"), LoginFixtures.request("1.2.3.4", UA)))
                .hasMessage("账号不存在");

        assertThat(countLoginLogs()).isZero();
    }
}
