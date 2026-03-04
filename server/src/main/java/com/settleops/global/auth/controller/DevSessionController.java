package com.settleops.global.auth.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
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
 *   <li>브라우저 시연 시 Consumer / Merchant 사용자 세션을 생성</li>
 * </ul>
 *
 * <p>동작 방식</p>
 * <ul>
 *   <li>요청 파라미터로 전달된 buyerId를 HttpSession에 저장한다.</li>
 *   <li>세션에 ROLE, BUYER_ID 값을 설정하여 이후 API에서 현재 사용자 식별에 사용한다.</li>
 * </ul>
 *
 * <p>예시</p>
 * <pre>
 * POST /api/dev/login-consumer?buyerId=BUYER_1
 * </pre>
 *
 * <p>사용 흐름</p>
 * <ol>
 *   <li>/api/dev/login-consumer 호출 → 세션 생성</li>
 *   <li>/api/me 호출 → 현재 사용자 확인</li>
 *   <li>Consumer API 호출 시 세션 기반 사용자 검증 수행</li>
 * </ol>
 *
 * <p>주의</p>
 * <ul>
 *   <li>DEV / LOCAL 환경에서만 사용한다.</li>
 *   <li>운영 환경에서는 제거하거나 비활성화해야 한다.</li>
 * </ul>
 */
@Profile({"local","dev"})
@RestController
@RequestMapping("/api/dev")
public class DevSessionController {

    @PostMapping("/login-consumer")
    public ResponseEntity<Void> loginConsumer(
            @RequestParam String buyerId,
            HttpSession session
    ) {
        session.setAttribute(MeController.SessionKeys.ROLE, "CONSUMER");
        session.setAttribute(MeController.SessionKeys.BUYER_ID, buyerId);
        session.setAttribute(MeController.SessionKeys.MERCHANT_ID, null);
        return ResponseEntity.ok().build();
    }
}