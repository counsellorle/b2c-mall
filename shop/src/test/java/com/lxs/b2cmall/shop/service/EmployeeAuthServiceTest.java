package com.lxs.b2cmall.shop.service;

import com.lxs.b2cmall.shop.auth.EmployeeTokenStore;
import com.lxs.b2cmall.shop.dto.LoginDTO;
import com.lxs.b2cmall.shop.entity.Employee;
import com.lxs.b2cmall.shop.entity.Shop;
import com.lxs.b2cmall.shop.mapper.ShopMapper;
import com.lxs.b2cmall.shop.support.LoginFixtures;
import com.lxs.b2cmall.shop.vo.EmployeeLoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B 端登录门户：签发 Redis 会话、返回店铺信息、登出删除会话。
 */
class EmployeeAuthServiceTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private EmployeeTokenStore employeeTokenStore;

    @Mock
    private ShopMapper shopMapper;

    private EmployeeAuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new EmployeeAuthService();
        LoginFixtures.inject(authService, "employeeService", employeeService);
        LoginFixtures.inject(authService, "employeeTokenStore", employeeTokenStore);
        LoginFixtures.inject(authService, "shopMapper", shopMapper);
    }

    private Employee employee(int id, int shopId) {
        Employee e = LoginFixtures.employee(id, "shop01", null, 1);
        e.setShopId(shopId);
        return e;
    }

    @Test
    @DisplayName("登录成功 → 签发 token 并写入 Redis，返回店铺信息")
    void loginIssuesToken() {
        Employee emp = employee(9, 2);
        when(employeeService.login(any(LoginDTO.class), any(MockHttpServletRequest.class))).thenReturn(emp);

        Shop shop = new Shop();
        shop.setId(2);
        shop.setShopName("二号店");
        when(shopMapper.findById(2)).thenReturn(shop);

        EmployeeLoginVO vo = authService.login(LoginFixtures.loginDTO("shop01", "123456"),
                new MockHttpServletRequest());

        assertThat(vo.getToken()).hasSize(32);
        assertThat(vo.getEmployeeId()).isEqualTo(9);
        assertThat(vo.getShopId()).isEqualTo(2);
        assertThat(vo.getShopName()).isEqualTo("二号店");
        assertThat(vo.getExpiresIn()).isEqualTo(8 * 3600L);

        ArgumentCaptor<Integer> shopCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(employeeTokenStore).save(eq(vo.getToken()), shopCaptor.capture(), eq(9), eq(8 * 3600L));
        assertThat(shopCaptor.getValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("密码错误时不签发 token")
    void loginFailureDoesNotIssueToken() {
        when(employeeService.login(any(LoginDTO.class), any(MockHttpServletRequest.class)))
                .thenThrow(new RuntimeException("密码错误"));

        assertThatThrownBy(() -> authService.login(LoginFixtures.loginDTO("shop01", "bad"),
                new MockHttpServletRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("密码错误");

        verify(employeeTokenStore, never()).save(anyString(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("登出删除对应 token 会话")
    void logoutRemovesSession() {
        authService.logout("token-abc");

        verify(employeeTokenStore).remove("token-abc");
    }
}
