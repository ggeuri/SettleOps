package com.settleops.global.auth.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 로그인 주체 정보를 반환하는 공통 인증 가드 API
 *
 * <p>기획서 규칙 (LOCKED)</p>
 * <ul>
 *   <li>라우팅 가드는 <b>/api/me</b> 단일 엔드포인트로만 판정한다.</li>
 *   <li>프론트는 모든 요청에 <b>credentials: include</b> 로 세션 쿠키를 포함한다.</li>
 *   <li>Consumer / Merchant / Admin 화면 라우팅은 role 기반으로 분기한다.</li>
 * </ul>
 *
 * <p>동작 방식</p>
 * <ul>
 *   <li>HttpSession에서 ROLE / BUYER_ID / MERCHANT_ID 를 조회한다.</li>
 *   <li>세션이 존재하지 않으면 401을 반환한다.</li>
 * </ul>
 *
 * <p>응답</p>
 * <pre>
 * {
 *   "role": "CONSUMER | MERCHANT | ADMIN",
 *   "buyerId": "...",
 *   "merchantId": "..."
 * }
 * </pre>
 *
 * <p>주의</p>
 * <ul>
 *   <li>본 API는 인증 상태 확인용이며 도메인 로직을 포함하지 않는다.</li>
 *   <li>프로젝트 전체에서 <b>/api/me 는 반드시 1개만 존재</b>해야 한다.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpSession session) {

        String role = (String) session.getAttribute(SessionKeys.ROLE);
        String buyerId = (String) session.getAttribute(SessionKeys.BUYER_ID);
        String merchantId = (String) session.getAttribute(SessionKeys.MERCHANT_ID);

        if (role == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(new MeResponse(role, buyerId, merchantId));
    }

    public record MeResponse(String role, String buyerId, String merchantId) {}

    /** 세션 키 하드코딩 방지 */
    public static final class SessionKeys {
        private SessionKeys() {}
        public static final String ROLE = "ROLE";
        public static final String BUYER_ID = "BUYER_ID";
        public static final String MERCHANT_ID = "MERCHANT_ID";
    }
}