package com.settleops.support;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * - QueryDSL repository 테스트용 빈 등록 <br/>
 * - JPAQueryFactory 생성 <br/>
 * - PaymentQueryRepository 주입 가능 상태 만들기
 * */
@TestConfiguration
public class QuerydslTestConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}