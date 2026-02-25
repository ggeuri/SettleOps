package com.settleops.global.db;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * DB 무결성 예외 판별 유틸리티.
 *
 * <p>
 * {@link org.springframework.dao.DataIntegrityViolationException} 발생 시
 * 원인 체인을 순회하여 "UNIQUE 제약 조건 위반(중복 키)" 여부를 판별한다.
 * </p>
 *
 * <p>
 * 목적:
 * <ul>
 *   <li>동시성 상황에서 발생 가능한 Duplicate Key(UNIQUE 충돌)만 정상 수렴 처리</li>
 *   <li>FK 위반, NOT NULL 위반 등 예상치 못한 무결성 오류는 그대로 상위로 전파</li>
 * </ul>
 * </p>
 *
 * <p>
 * 현재 기준:
 * <ul>
 *   <li>MySQL 에러코드 1062 (Duplicate entry)만 중복으로 간주</li>
 *   <li>그 외 SQLState 23000 계열은 보수적으로 중복으로 판단하지 않음</li>
 * </ul>
 * </p>
 *
 * <p>
 * 본 클래스는 상태를 가지지 않는 정적(static) 유틸리티이며,
 * 스프링 빈으로 등록하지 않는다.
 * </p>
 */
public final class DbConstraintUtils {

    private DbConstraintUtils() {}

    public static boolean isDuplicateKey(DataIntegrityViolationException e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                // MySQL duplicate entry
                if (sqlEx.getErrorCode() == 1062) return true;

                // SQLState 23000 = integrity constraint violation (vendor 공통)
                // 단, 23000은 FK/NOT NULL 등도 포함될 수 있으니
                // errorCode=1062 우선, 그 외는 보수적으로 false 권장
            }
            t = t.getCause();
        }
        return false;
    }

    public static String rootMessage(Throwable e) {
        Throwable t = e;
        Throwable last = e;
        while (t != null) {
            last = t;
            t = t.getCause();
        }
        return last == null ? null : last.getMessage();
    }
}