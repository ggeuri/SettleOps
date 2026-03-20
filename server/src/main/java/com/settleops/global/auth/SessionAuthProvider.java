package com.settleops.global.auth;

import com.settleops.global.error.ForbiddenException;
import com.settleops.global.error.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthProvider {

    /**
     * 현재 SecurityContext에 저장된 인증 주체(principal)를 문자열 ID로 반환한다.
     * <p>
     * SessionAuthenticationFilter에서 역할별 ID를 principal로 주입하므로,
     * CONSUMER면 buyerId, MERCHANT면 merchantId, ADMIN이면 adminId가 반환된다.
     *
     * @return 현재 인증 주체 ID
     * @throws UnauthorizedException 인증 정보가 없거나 principal 형식이 올바르지 않은 경우
     */
    public String getCurrentPrincipal() {
        return getAuthentication().getName();
    }

    /**
     * 현재 로그인 사용자가 CONSUMER 권한인지 검증한 뒤 buyerId를 반환한다.
     *
     * @return 현재 consumer의 buyerId
     * @throws ForbiddenException 현재 사용자가 CONSUMER 권한이 아닌 경우
     */
    public String getCurrentConsumerId() {
        return getPrincipalByRole("ROLE_CONSUMER", "consumer 권한이 없습니다.");
    }

    /**
     * 현재 로그인 사용자가 MERCHANT 권한인지 검증한 뒤 merchantId를 반환한다.
     *
     * @return 현재 merchant의 merchantId
     * @throws ForbiddenException 현재 사용자가 MERCHANT 권한이 아닌 경우
     */
    public String getCurrentMerchantId() {
        return getPrincipalByRole("ROLE_MERCHANT", "merchant 권한이 없습니다.");
    }

    /**
     * 현재 로그인 사용자가 ADMIN 권한인지 검증한 뒤 adminId를 반환한다.
     *
     * @return 현재 admin의 adminId
     * @throws ForbiddenException 현재 사용자가 ADMIN 권한이 아닌 경우
     */
    public String getCurrentAdminId() {
        return getPrincipalByRole("ROLE_ADMIN", "admin 권한이 없습니다.");
    }

    /**
     * 현재 로그인 사용자가 ADMIN 권한인지 검증한다.
     * <p>
     * 반환값이 필요 없고, 관리자 권한 보유 여부만 확인할 때 사용한다.
     *
     * @throws ForbiddenException 현재 사용자가 ADMIN 권한이 아닌 경우
     */
    public void requireAdmin() {
        requireRole("ROLE_ADMIN", "admin 권한이 없습니다.");
    }

    /**
     * 지정된 역할을 가진 사용자인지 검증한 뒤 현재 principal ID를 반환한다.
     *
     * @param requiredRole 검증할 권한명 (예: ROLE_CONSUMER)
     * @param forbiddenMessage 권한 불일치 시 사용할 예외 메시지
     * @return 현재 인증 주체 ID
     * @throws ForbiddenException 현재 사용자가 지정된 권한이 아닌 경우
     */
    private String getPrincipalByRole(String requiredRole, String forbiddenMessage) {
        requireRole(requiredRole, forbiddenMessage);
        return getCurrentPrincipal();
    }

    /**
     * 현재 인증 사용자가 지정된 권한을 가지고 있는지 검증한다.
     *
     * @param requiredRole 검증할 권한명
     * @param forbiddenMessage 권한 불일치 시 사용할 예외 메시지
     * @throws ForbiddenException 현재 사용자가 지정된 권한이 아닌 경우
     */
    private void requireRole(String requiredRole, String forbiddenMessage) {
        Authentication authentication = getAuthentication();

        boolean hasRole = authentication.getAuthorities().stream()
                .anyMatch(a -> requiredRole.equals(a.getAuthority()));

        if (!hasRole) {
            throw new ForbiddenException(forbiddenMessage);
        }
    }

    /**
     * 현재 요청의 SecurityContext에서 Authentication을 조회한다.
     *
     * @return 현재 인증 객체
     * @throws UnauthorizedException 인증 정보가 없거나 인증되지 않은 경우
     */
    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new UnauthorizedException("인증 정보가 없습니다.");
        }

        return authentication;
    }
}