package com.settleops.global.db;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * DB 무결성 예외 판별 유틸리티.
 *
 * <p>
 * {@link org.springframework.dao.DataIntegrityViolationException} 발생 시
 * 원인 체인을 순회하여 "UNIQUE 제약 조건 위반(중복 키)" 여부를 판별합니다.
 * </p>
 *
 * <p>
 * 목적:
 * </p>
 * <ul>
 *   <li>동시성 상황에서 발생 가능한 Duplicate Key(UNIQUE 충돌)만 정상 수렴 처리</li>
 *   <li>FK 위반, NOT NULL 위반 등 예상치 못한 무결성 오류는 그대로 상위로 전파</li>
 * </ul>
 *
 * <p>
 * 현재 기준:
 * </p>
 * <ul>
 *   <li>MySQL 에러코드 1062 (Duplicate entry)</li>
 *   <li>H2 / ANSI SQLState 23505 (unique violation)</li>
 * </ul>
 *
 * <p>
 * 본 클래스는 상태를 가지지 않는 정적(static) 유틸리티이며,
 * 스프링 빈으로 등록하지 않습니다.
 * </p>
 */
public final class DbConstraintUtils {

    private DbConstraintUtils() {
    }

    public static boolean isDuplicateKey(DataIntegrityViolationException e) {
        Throwable t = e;

        while (t != null) {
            if (t instanceof SQLException sqlEx) {
                int errorCode = sqlEx.getErrorCode();
                String sqlState = sqlEx.getSQLState();

                // MySQL duplicate entry
                if (errorCode == 1062) {
                    return true;
                }

                // H2 / ANSI unique violation
                if ("23505".equals(sqlState)) {
                    return true;
                }
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