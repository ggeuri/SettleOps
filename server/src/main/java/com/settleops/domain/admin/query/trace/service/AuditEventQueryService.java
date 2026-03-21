package com.settleops.domain.admin.query.trace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.settleops.domain.admin.query.trace.dto.AuditEventResponseDto;
import com.settleops.domain.admin.query.trace.dto.AuditEventRowDto;
import com.settleops.domain.hold.domain.QHoldEvent;
import com.settleops.domain.payment.domain.QPaymentEvent;
import com.settleops.domain.refund.domain.QRefundEvent;
import com.settleops.global.audit.EntityType;
import com.settleops.global.error.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditEventQueryService {

    private final JPAQueryFactory jpaQueryFactory;
    private final ObjectMapper objectMapper;

    public AuditEventResponseDto findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BadRequestException("requestId는 필수입니다.");
        }

        List<AuditEventRowDto> items = new ArrayList<>();

        addPaymentEvents(requestId, items);
        addHoldEvents(requestId, items);
        addRefundEvents(requestId, items);

        items.sort(
                Comparator.comparing(
                                AuditEventRowDto::getOccurredAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                AuditEventRowDto::getEntityType,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                AuditEventRowDto::getEntityId,
                                Comparator.nullsLast(String::compareTo)
                        )
        );

        return new AuditEventResponseDto(requestId, items);
    }

    private void addPaymentEvents(String requestId, List<AuditEventRowDto> items) {
        QPaymentEvent paymentEvent = QPaymentEvent.paymentEvent;

        var rows = jpaQueryFactory
                .selectFrom(paymentEvent)
                .where(paymentEvent.requestId.eq(requestId))
                .fetch();

        for (var event : rows) {
            items.add(AuditEventRowDto.of(
                    event.getOccurredAt(),
                    event.getEventType().name(),
                    EntityType.PAYMENT,
                    event.getPaymentId(),
                    event.getStatusBefore() == null ? null : event.getStatusBefore().name(),
                    event.getStatusAfter() == null ? null : event.getStatusAfter().name(),
                    "{}"
            ));
        }
    }

    private void addHoldEvents(String requestId, List<AuditEventRowDto> items) {
        QHoldEvent holdEvent = QHoldEvent.holdEvent;

        var rows = jpaQueryFactory
                .selectFrom(holdEvent)
                .where(holdEvent.requestId.eq(requestId))
                .fetch();

        for (var event : rows) {
            String metaJson = normalizeJson(event.getMetaJson());

            items.add(AuditEventRowDto.of(
                    event.getOccurredAt(),
                    event.getEventType().name(),
                    EntityType.HOLD,
                    event.getHoldId(),
                    extractStatus(metaJson, "before"),
                    extractStatus(metaJson, "after"),
                    metaJson
            ));
        }
    }

    private void addRefundEvents(String requestId, List<AuditEventRowDto> items) {
        QRefundEvent refundEvent = QRefundEvent.refundEvent;

        var rows = jpaQueryFactory
                .selectFrom(refundEvent)
                .where(refundEvent.requestId.eq(requestId))
                .fetch();

        for (var event : rows) {
            items.add(AuditEventRowDto.of(
                    event.getOccurredAt(),
                    event.getEventType().name(),
                    EntityType.REFUND,
                    event.getRefundId(),
                    event.getStatusBefore() == null ? null : event.getStatusBefore().name(),
                    event.getStatusAfter() == null ? null : event.getStatusAfter().name(),
                    "{}"
            ));
        }
    }

    private String normalizeJson(String metaJson) {
        return (metaJson == null || metaJson.isBlank()) ? "{}" : metaJson;
    }

    private String extractStatus(String metaJson, String nodeName) {
        try {
            JsonNode root = objectMapper.readTree(metaJson);
            JsonNode statusNode = root.path(nodeName).path("status");
            return statusNode.isMissingNode() || statusNode.isNull() ? null : statusNode.asText();
        } catch (Exception e) {
            return null;
        }
    }
}