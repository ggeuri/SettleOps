package com.settleops.global.auth;

import com.settleops.global.auth.controller.MeController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MerchantSessionResolverTest {

    @Test
    @DisplayName("세션이 없으면 401을 반환한다")
    void resolveMerchantId_unauthorizedWhenSessionMissing() {
        MerchantSessionResolver resolver = new MerchantSessionResolver();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        Mockito.when(request.getSession(false)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveMerchantId(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(401);
                });
    }

    @Test
    @DisplayName("세션에 merchantId가 없으면 401을 반환한다")
    void resolveMerchantId_unauthorizedWhenMerchantIdMissing() {
        MerchantSessionResolver resolver = new MerchantSessionResolver();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession session = Mockito.mock(HttpSession.class);

        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute(MeController.SessionKeys.MERCHANT_ID)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolveMerchantId(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(401);
                });
    }

    @Test
    @DisplayName("세션에 merchantId가 있으면 해당 값을 반환한다")
    void resolveMerchantId_returnsMerchantId() {
        MerchantSessionResolver resolver = new MerchantSessionResolver();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession session = Mockito.mock(HttpSession.class);

        Mockito.when(request.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute(MeController.SessionKeys.MERCHANT_ID)).thenReturn("merchant-1");

        String result = resolver.resolveMerchantId(request);

        assertThat(result).isEqualTo("merchant-1");
    }
}