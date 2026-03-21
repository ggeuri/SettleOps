package com.settleops.global.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settleops.global.enums.Action;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test-db")
@Transactional
class AuditLogRepositoryImplMySqlIT {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("includeNoOp=false 이면 no-op=true 로그는 제외된다")
    void findByRequestId_excludes_noop_when_includeNoOp_false() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();

        auditLogRepository.save(AuditLog.builder()
                .requestId(requestId)
                .occurredAt(LocalDateTime.now().minusMinutes(1))
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId(UUID.randomUUID().toString())
                .statusBefore("HOLD_REQUESTED")
                .statusAfter("HOLD_ACTIVE")
                .merchantId("mrc_1001")
                .metaJson(metaJson(false, null))
                .build());

        auditLogRepository.save(AuditLog.builder()
                .requestId(requestId)
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId(UUID.randomUUID().toString())
                .statusBefore("HOLD_ACTIVE")
                .statusAfter("HOLD_ACTIVE")
                .merchantId("mrc_1001")
                .metaJson(metaJson(true, "ALREADY_APPROVED"))
                .build());

        // when
        var page = auditLogRepository.findByRequestIdOrderByOccurredAtDesc(
                requestId,
                false,
                PageRequest.of(0, 20)
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getMetaJson()).contains("\"noOp\":false");
    }

    @Test
    @DisplayName("includeNoOp=true 이면 no-op=true 로그도 포함된다")
    void findByRequestId_includes_noop_when_includeNoOp_true() throws Exception {
        // given
        String requestId = UUID.randomUUID().toString();

        auditLogRepository.save(AuditLog.builder()
                .requestId(requestId)
                .occurredAt(LocalDateTime.now().minusMinutes(1))
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId(UUID.randomUUID().toString())
                .statusBefore("HOLD_REQUESTED")
                .statusAfter("HOLD_ACTIVE")
                .merchantId("mrc_1001")
                .metaJson(metaJson(false, null))
                .build());

        auditLogRepository.save(AuditLog.builder()
                .requestId(requestId)
                .occurredAt(LocalDateTime.now())
                .actorType(ActorType.ADMIN)
                .actorId("admin1")
                .action(Action.HOLD_APPROVED)
                .entityType(EntityType.HOLD)
                .entityId(UUID.randomUUID().toString())
                .statusBefore("HOLD_ACTIVE")
                .statusAfter("HOLD_ACTIVE")
                .merchantId("mrc_1001")
                .metaJson(metaJson(true, "ALREADY_APPROVED"))
                .build());

        // when
        var page = auditLogRepository.findByRequestIdOrderByOccurredAtDesc(
                requestId,
                true,
                PageRequest.of(0, 20)
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    private String metaJson(boolean noOp, String noOpReason) throws Exception {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("noOp", noOp);

        if (noOpReason != null) {
            meta.put("noOpReason", noOpReason);
        }

        return objectMapper.writeValueAsString(meta);
    }
}