package com.settleops.global.auth.controller;

import com.settleops.global.error.BadRequestException;
import com.settleops.global.error.ForbiddenException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEV 환경용 세션 주입 컨트롤러 (시연 / 로컬 개발 전용)
 *
 * <p>목적</p>
 * <ul>
 *   <li>실제 로그인 기능 없이도 세션 기반 인증 흐름을 테스트하기 위한 임시 API</li>
 *   <li>브라우저 시연 시 Consumer / Merchant / Admin 사용자 세션을 생성</li>
 * </ul>
 *
 * <p>보안(운영 사고 방지) 이중장치</p>
 * <ol>
 *   <li>@Profile({"local","dev"})로 prod에서 빈이 로딩되지 않도록 1차 차단</li>
 *   <li>dev-session.enabled=false(기본값) feature flag로 2차 차단</li>
 * </ol>
 *
 * <p>예시</p>
 * <pre>
 * POST /api/dev/login-consumer?buyerId=BUYER_1
 * POST /api/dev/login-merchant?merchantId=MERCHANT_1
 * </pre>
 */
@Profile({"local","dev"})
@RestController
@RequestMapping("/api/dev")
public class DevSessionController {

    /**
     * 운영 dev 사고 방지용 feature flag
     * - 기본값 false (dev-session.enabled 미설정 시 차단)
     * - local/dev 환경에서만 true로 설정해서 사용
     */
    @Value("${dev-session.enabled:false}")
    private boolean devSessionEnabled;

    @PostMapping("/login-consumer")
    public ResponseEntity<Void> loginConsumer(
            @RequestParam String buyerId,
            HttpSession session
    ) {
        // [필수] 2차 안전장치: feature flag로 dev 로그인 기능 자체를 차단할 수 있어야 함
        assertDevSessionEnabled();

        String normalizedBuyerId = normalizeId(buyerId, "buyerId");

        clearSession(session);

        session.setAttribute(MeController.SessionKeys.ROLE, "CONSUMER");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, normalizedBuyerId);
        session.setAttribute(MeController.SessionKeys.MERCHANT_ID, null);
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, null);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login-merchant")
    public ResponseEntity<Void> loginMerchant(
            @RequestParam String merchantId,
            HttpSession session
    ) {
        // [필수] 2차 안전장치: feature flag로 dev 로그인 기능 자체를 차단할 수 있어야 함
        assertDevSessionEnabled();

        String normalizedMerchantId = normalizeId(merchantId, "merchantId");

        clearSession(session);

        session.setAttribute(MeController.SessionKeys.ROLE, "MERCHANT");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, null);
        session.setAttribute(MeController.SessionKeys.MERCHANT_ID, normalizedMerchantId);
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, null);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login-admin")
    public ResponseEntity<Void> loginAdmin(
            @RequestParam String adminId,
            HttpSession session
    ) {
        assertDevSessionEnabled();

        String normalizedAdminId = normalizeId(adminId, "adminId");

        clearSession(session);

        session.setAttribute(MeController.SessionKeys.ROLE, "ADMIN");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, null);
        session.setAttribute(MeController.SessionKeys.MERCHANT_ID, null);
        session.setAttribute(MeController.SessionKeys.ADMIN_ID, normalizedAdminId);

        return ResponseEntity.ok().build();
    }

    private void assertDevSessionEnabled() {
        if (!devSessionEnabled) {
            throw new ForbiddenException("dev session disabled");
        }
    }

    // [필수] 길이 제한(정책 충돌 방지)
    private String normalizeId(String rawValue, String fieldName) {

        if (!StringUtils.hasText(rawValue)) {
            throw new BadRequestException(fieldName + "는 필수입니다.");
        }

        String normalized = rawValue.trim();

        if (normalized.length() > 32) {
            throw new BadRequestException(fieldName + " 길이가 올바르지 않습니다.(max 32)");
        }

        return normalized;
    }

    /**
     * 세션 오염 방지: dev login 호출 시 role/id를 항상 초기화하고 단일 role만 주입한다.
     */
    private void clearSession(HttpSession session) {
        session.removeAttribute(MeController.SessionKeys.ROLE);
        session.removeAttribute(MeController.SessionKeys.BUYER_ID);
        session.removeAttribute(MeController.SessionKeys.MERCHANT_ID);
        session.removeAttribute(MeController.SessionKeys.ADMIN_ID);
    }
}