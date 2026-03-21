package com.settleops.domain.admin.query.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.global.error.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AuditEventQueryServiceTest {

    private AuditEventQueryService auditEventQueryService;

    @BeforeEach
    void setUp() {
        JPAQueryFactory jpaQueryFactory = mock(JPAQueryFactory.class);
        ObjectMapper objectMapper = new ObjectMapper();

        auditEventQueryService = new AuditEventQueryService(jpaQueryFactory, objectMapper);
    }

    @Test
    void blank_requestId_should_throw_bad_request() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> auditEventQueryService.findByRequestId(" ")
        );

        assertEquals("requestId는 필수입니다.", ex.getMessage());
    }
}