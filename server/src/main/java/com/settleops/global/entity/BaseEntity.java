package com.settleops.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * <p>공통 감사(Audit) 컬럼을 제공하는 BaseEntity 입니다.</p>
 * <ul>
 *     <li>기획서 SoT 기준에 따라 모든 시간 컬럼은 DATETIME(6)으로 고정합니다.</li>
 *     <li>created_at, updated_at 컬럼명을 전 도메인에서 통일합니다.</li>
 *     <li>DB 정밀도(마이크로초)와 정합성을 맞추기 위해 columnDefinition = "datetime(6)"을 명시합니다.</li>
 * </ul>
 *
 * <p>주의</p>
 * <ul>
 *     <li>request 단위 추적과 무관한 순수 감사 컬럼입니다.</li>
 *     <li>시간 생성은 애플리케이션 레벨(LocalDateTime.now()) 기준입니다.</li>
 * </ul>
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    /**
     * 엔티티 최초 생성 시각입니다.
     *
     * <ul>
     *     <li>컬럼명: created_at</li>
     *     <li>nullable = false</li>
     *     <li>updatable = false (생성 이후 변경 금지)</li>
     *     <li>DB 타입: datetime(6)</li>
     * </ul>
     */
    @Column(name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "datetime(6)")
    protected LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정 시각입니다.
     *
     * <ul>
     *     <li>컬럼명: updated_at</li>
     *     <li>nullable = false</li>
     *     <li>DB 타입: datetime(6)</li>
     *     <li>Update 발생 시마다 자동 갱신됩니다.</li>
     * </ul>
     */
    @Column(name = "updated_at",
            nullable = false,
            columnDefinition = "datetime(6)")
    protected LocalDateTime updatedAt;

    /**
     * 엔티티가 최초 영속화(Persist) 되기 직전에 실행됩니다.
     *
     * <ul>
     *     <li>createdAt, updatedAt을 현재 시각으로 초기화합니다.</li>
     *     <li>JPA 생명주기 콜백(@PrePersist)을 사용합니다.</li>
     * </ul>
     */
    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 엔티티가 수정(Update) 되기 직전에 실행됩니다.
     *
     * <ul>
     *     <li>updatedAt을 현재 시각으로 갱신합니다.</li>
     *     <li>createdAt은 변경되지 않습니다.</li>
     *     <li>JPA 생명주기 콜백(@PreUpdate)을 사용합니다.</li>
     * </ul>
     */
    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}