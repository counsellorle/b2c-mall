package com.lxs.b2cmall.shop.auth;

import com.lxs.b2cmall.shop.support.LoginFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P6 身份解析测试：网关注入身份优先、直连 token 回退、缺失时拒绝。
 */
class P6IdentityTest {

    @Mock
    private UserTokenStore userTokenStore;

    private CurrentUser currentUser;

    private CurrentShop currentShop;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        currentUser = new CurrentUser();
        LoginFixtures.inject(currentUser, "tokenStore", userTokenStore);
        currentShop = new CurrentShop();
    }

    @Test
    @DisplayName("网关注入 X-User-Id 时直接采用，不再查 Redis")
    void injectedUserIdWins() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CurrentUser.HEADER_USER_ID, "5");

        assertThat(currentUser.requireUserId(request)).isEqualTo(5);
        verify(userTokenStore, never()).findUserId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("未经网关时回退为 token 校验（本地直连调试）")
    void fallbackToTokenWhenNoInjectedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CurrentUser.HEADER_TOKEN, "abc");
        when(userTokenStore.findUserId("abc")).thenReturn(7);

        assertThat(currentUser.requireUserId(request)).isEqualTo(7);
    }

    @Test
    @DisplayName("既无注入头也无 token 时提示先登录")
    void missingIdentityRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> currentUser.requireUserId(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("请先登录");
    }

    @Test
    @DisplayName("注入的 userId 非法（非数字）视为未登录")
    void invalidInjectedUserIdRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CurrentUser.HEADER_USER_ID, "abc");

        assertThatThrownBy(() -> currentUser.requireUserId(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("请先登录");
    }

    @Test
    @DisplayName("店铺/员工身份从网关注入头解析")
    void shopIdentityParsedFromHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CurrentShop.HEADER_SHOP_ID, "3");
        request.addHeader(CurrentShop.HEADER_EMPLOYEE_ID, "9");

        assertThat(currentShop.shopId(request)).isEqualTo(3);
        assertThat(currentShop.employeeId(request)).isEqualTo(9);
        assertThat(currentShop.requireShopId(request)).isEqualTo(3);
    }

    @Test
    @DisplayName("无店铺头时 requireShopId 抛登录过期")
    void missingShopIdRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(currentShop.shopId(request)).isNull();
        assertThatThrownBy(() -> currentShop.requireShopId(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("登录已过期，请重新登录");
    }
}
