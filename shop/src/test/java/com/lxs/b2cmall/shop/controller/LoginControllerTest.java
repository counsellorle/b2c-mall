package com.lxs.b2cmall.shop.controller;

import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.service.EmployeeAuthService;
import com.lxs.b2cmall.shop.service.EmployeeService;
import com.lxs.b2cmall.shop.vo.EmployeeLoginVO;
import com.lxs.b2cmall.shop.support.LoginFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录接口的返回格式测试（Controller 层）。
 *
 * 测 POST /employee/login 返回的 JSON：成功时是不是 code=200，
 * 失败时 message 对不对、会不会泄露内部信息。
 * Service 换成假的，只用 standaloneSetup 挂这一个 Controller，不启动 Spring。
 * 带 @Disabled 的是"故意关掉的红灯"，对应还没修的 bug。
 */
class LoginControllerTest {

    private static final String VALID_BODY =
            "{\"username\":\"shop01\",\"password\":\"123456\"}";

    @Mock
    private EmployeeService employeeService;

    @Mock
    private EmployeeAuthService employeeAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LoginController controller = new LoginController();
        LoginFixtures.inject(controller, "employeeService", employeeService);
        LoginFixtures.inject(controller, "employeeAuthService", employeeAuthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 造一个"登录成功后返回"的登录结果（含 token 与店铺信息）。 */
    private EmployeeLoginVO loggedInVO() {
        EmployeeLoginVO vo = new EmployeeLoginVO();
        vo.setToken("token-abc");
        vo.setEmployeeId(1);
        vo.setShopId(1);
        vo.setUsername("shop01");
        vo.setShopName("testShop");
        vo.setExpiresIn(28800L);
        return vo;
    }

    // ==================== 正常登录 ====================

    @Test
    @DisplayName("登录成功 → 返回 code=200 和用户信息")
    void loginReturnsSuccessResult() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenReturn(loggedInVO());

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.username").value("shop01"))
                .andExpect(jsonPath("$.data.employeeId").value(1));
    }

    // ==================== 登录失败 ====================

    @Test
    @DisplayName("密码错误 → 返回 code=500，message 为「密码错误」")
    void wrongPasswordReturnsErrorResult() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("密码错误"));

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("密码错误"));
    }

    @Test
    @DisplayName("账号不存在 → 返回 code=500，message 为「账号不存在」")
    void accountNotFoundReturnsErrorResult() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("账号不存在"));

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"__not_exist__\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("账号不存在"));
    }

    @Test
    @DisplayName("登录失败 → 错误信息里不能出现 SQL 语句、异常类名、密码密文")
    void errorMessageHasNoInternalDetail() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("密码错误"));

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                // 泄露这些等于把系统内幕告诉攻击者
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("SQLite"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    // ==================== 红灯队列 ====================

    @Test
    @Disabled("待修复缺陷 B6：@Valid 校验失败抛 MethodArgumentNotValidException，"
            + "而 Controller 只 catch RuntimeException，接不到，返回的是 Spring 默认错误格式")
    @DisplayName("【红灯】用户名为空时，返回格式也要和其他错误一样（有 code 和 message）")
    void blankUsernameReturnsResultShape() throws Exception {
        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Disabled("待任务项 1.2.4 落地：password 为 null 时 Jackson 默认会输出 \"password\": null，"
            + "是否允许回显这个键要先定契约")
    @DisplayName("【红灯】登录成功的返回体里不应该出现 password 键")
    void responseMustNotCarryPassword() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenReturn(loggedInVO());

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @Disabled("待修复缺陷 B5：当前失败时 HTTP 恒 200、业务码恒 500，无法区分三类失败")
    @DisplayName("【红灯】三种失败应该能用不同的错误码区分开")
    void failureCodesAreDistinguishable() throws Exception {
        when(employeeAuthService.login(any(LoginDTO.class), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("密码错误"));

        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(500)));
    }

    @Test
    @Disabled("待修复缺陷 B6：校验失败时响应里没有 Result.message，拿不到这句文案")
    @DisplayName("【红灯】用户名为空 / 全是空格时必须被拒绝，并提示「账号不能为空」")
    void blankUsernameIsRejectedWithMessage() throws Exception {
        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").value("账号不能为空"));
    }

    @Test
    @Disabled("待修复缺陷 B6：同 B6，当前拿不到校验文案")
    @DisplayName("【红灯】密码为空时必须被拒绝，并提示「密码不能为空」")
    void blankPasswordIsRejectedWithMessage() throws Exception {
        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"shop01\"}"))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").value("密码不能为空"));
    }

    @Test
    @Disabled("待修复缺陷 B6：请求体为空时 Spring 抛 HttpMessageNotReadableException，返回默认 400 错误体")
    @DisplayName("【红灯】空请求体也必须返回统一的 Result 格式")
    void emptyBodyStillReturnsResultShape() throws Exception {
        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
