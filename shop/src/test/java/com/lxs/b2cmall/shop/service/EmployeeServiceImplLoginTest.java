package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.event.LoginEvent;
import com.lxs.b2cmall.shop.mapper.EmployeeMapper;
import com.lxs.b2cmall.shop.service.impl.EmployeeServiceImpl;
import com.lxs.b2cmall.shop.support.LoginFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录功能的测试（Service 层）。
 *
 * 测 login() 的四种情况：密码对 / 密码错 / 账号不存在 / 账号被禁用。
 * 用 Mockito 把数据库换成假的，不启动 Spring，跑得快。
 * 带 @Disabled 的是"故意关掉的红灯"（对应还没修的 bug），修好后删掉注解就会变绿。
 */
class EmployeeServiceImplLoginTest {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmployeeServiceImpl();
        // 生产代码用的是字段注入，只能反射把测试替身塞进去
        LoginFixtures.inject(service, "employeeMapper", employeeMapper);
        LoginFixtures.inject(service, "eventPublisher", eventPublisher);
    }

    /** 让"假数据库"在查这个用户名时返回一个员工。status：1=启用，0=禁用。 */
    private void givenEmployee(String username, String rawPassword, Integer status) {
        when(employeeMapper.findByUsername(username))
                .thenReturn(LoginFixtures.employee(1, username, rawPassword, status));
    }

    /** 取出被发出的那个登录事件，用来检查事件内容。 */
    private LoginEvent capturedLoginEvent() {
        ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    // ==================== 密码正确 ====================

    @Test
    @DisplayName("密码正确 → 登录成功，返回的用户信息里密码已被清空")
    void clearsPasswordAndReturnsUser() {
        givenEmployee("shop01", "123456", 1);

        Employee result = service.login(
                LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA));

        // 密码必须清空，不能回传给前端
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("shop01");
        assertThat(result.getPassword()).isNull();
    }

    @Test
    @DisplayName("密码正确时，按用户名查数据库恰好查 1 次")
    void queriesRepositoryExactlyOnce() {
        givenEmployee("shop01", "123456", 1);

        service.login(LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA));

        verify(employeeMapper, times(1)).findByUsername("shop01");
    }

    @Test
    @DisplayName("密码正确时，发出的事件里 success=true 且带着正确的用户 id")
    void publishesSuccessEvent() {
        givenEmployee("shop01", "123456", 1);

        service.login(LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA));

        LoginEvent event = capturedLoginEvent();
        assertThat(event.isSuccess()).isTrue();
        assertThat(event.getUserId()).isEqualTo(1);
    }

    @Test
    @DisplayName("状态是 1 时判定为「已启用」，允许登录")
    void statusOneIsEnabled() {
        givenEmployee("shop01", "123456", 1);

        Employee result = service.login(LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA));

        assertThat(result.getStatus()).isEqualTo(1);
    }

    // ==================== 密码错误 ====================

    @Test
    @DisplayName("密码不匹配 → 报「密码错误」")
    void throwsPasswordMismatch() {
        givenEmployee("shop01", "123456", 1);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "not-my-password"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("密码错误");
    }

    @Test
    @DisplayName("密码错误时，事件里的 userId 必须是真实 id（风控要能定位到是哪个账号）")
    void publishesFailureEventWithRealUserId() {
        givenEmployee("shop01", "123456", 1);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "not-my-password"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class);

        LoginEvent event = capturedLoginEvent();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getUserId()).isEqualTo(1);
    }

    @Test
    @DisplayName("密码错误时，不能去修改登录次数")
    void doesNotTouchLoginStatsOnWrongPassword() {
        givenEmployee("shop01", "123456", 1);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "not-my-password"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class);

        verify(employeeMapper, never()).updateLoginInfo(anyInt());
    }

    @Test
    @DisplayName("密码末尾多一个空格也算错")
    void trailingSpaceIsRejected() {
        givenEmployee("shop01", "123456", 1);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "123456 "),
                LoginFixtures.request("1.2.3.4", UA)))
                .hasMessage("密码错误");
    }

    // ==================== 账号不存在 ====================

    @Test
    @DisplayName("查不到这个用户 → 报「账号不存在」")
    void throwsAccountNotFound() {
        when(employeeMapper.findByUsername(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("__not_exist__", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("账号不存在");
    }

    @Test
    @DisplayName("账号不存在时也发出失败事件，但 userId 为空")
    void publishesFailureEventWithNullUserId() {
        when(employeeMapper.findByUsername(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("__not_exist__", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class);

        LoginEvent event = capturedLoginEvent();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getUserId()).isNull();
    }

    // 缺陷 B1（账号不存在时插审计日志抛约束异常）只有在真实监听器参与时才测得出，
    // 见 LoginObserverSideEffectTest#accountNotFoundSurvivesRealListener。

    // ==================== 账号被禁用 ====================

    @Test
    @DisplayName("状态不是 1 → 报「账号已被禁用」")
    void throwsAccountDisabled() {
        givenEmployee("shop01", "123456", 0);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("账号已被禁用");
    }

    @Test
    @DisplayName("要先验密码再验状态：账号被禁用 + 密码也错时，应该报「密码错误」")
    void passwordCheckedBeforeStatus() {
        // 否则攻击者能用任意密码试出"这个账号存在且被禁用了"
        givenEmployee("shop01", "123456", 0);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "wrong"),
                LoginFixtures.request("1.2.3.4", UA)))
                .hasMessage("密码错误");
    }

    @Test
    @DisplayName("禁用账号登录失败时，同样不改登录次数")
    void doesNotTouchLoginStatsOnDisabledAccount() {
        givenEmployee("shop01", "123456", 0);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class);

        verify(employeeMapper, never()).updateLoginInfo(anyInt());
    }

    @Test
    @Disabled("待修复缺陷 B11：status 为 null 时 Integer 拆箱会抛 NPE")
    @DisplayName("【红灯】status 是 null 时应视为「禁用」，而不是抛空指针异常")
    void nullStatusIsNotNpe() {
        givenEmployee("shop01", "123456", null);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("账号已被禁用")
                .isNotInstanceOf(NullPointerException.class);
    }

    @Test
    @Disabled("待修复缺陷 B2：当前 status != 1 分支直接 throw，没有发事件")
    @DisplayName("【红灯】禁用账号的登录尝试也必须留下失败事件")
    void disabledAttemptMustBeAudited() {
        givenEmployee("shop01", "123456", 0);

        assertThatThrownBy(() -> service.login(
                LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA)))
                .isInstanceOf(RuntimeException.class);

        LoginEvent event = capturedLoginEvent();
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getUserId()).isEqualTo(1);
    }

    // ==================== 事件内容 ====================

    @Test
    @Disabled("待修复缺陷 B4：当前 loginDevice 与 userAgent 传入的是同一个值")
    @DisplayName("【红灯】事件里的 loginDevice 不该和 userAgent 一模一样")
    void deviceShouldNotDuplicateUserAgent() {
        givenEmployee("shop01", "123456", 1);

        service.login(LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("1.2.3.4", UA));

        LoginEvent event = capturedLoginEvent();
        assertThat(event.getLoginDevice()).isNotEqualTo(event.getUserAgent());
    }

    @Test
    @DisplayName("客户端 IP 要写进事件里（审计日志要用）")
    void carriesClientIp() {
        givenEmployee("shop01", "123456", 1);

        service.login(LoginFixtures.loginDTO("shop01", "123456"),
                LoginFixtures.request("203.0.113.7", UA));

        LoginEvent event = capturedLoginEvent();
        assertThat(event.getLoginIp()).isEqualTo("203.0.113.7");
    }
}
