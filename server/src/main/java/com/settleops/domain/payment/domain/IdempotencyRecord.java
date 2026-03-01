package com.settleops.domain.payment.domain;

import com.settleops.global.enums.IdempotencyTargetType;
import com.settleops.global.logging.RequestIdProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "idempotency_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency",
                        columnNames = {"target_type", "target_id", "idempotency_key"}
                )
        },
        indexes = {
                @Index(name = "idx_idem_target", columnList = "target_type, target_id"),
                @Index(name = "idx_idem_request_id", columnList = "request_id"),
                @Index(name = "idx_idem_created_at", columnList = "created_at")
        }
)
public class IdempotencyRecord {

        private final static String MDC_KEY = "requestId";

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "idempotency_id")
        private Long idempotencyId;

        @Enumerated(EnumType.STRING)
        @Column(name = "target_type", length = 32, nullable = false)
        private IdempotencyTargetType targetType;// 고정: PAY_ORDER

        @Column(name = "target_id", length = 64, nullable = false)
        private String targetId;   // orderId

        @Column(name = "idempotency_key", length = 128, nullable = false)
        private String idempotencyKey; // X-Idempotency-Key

        @Column(name = "payment_id", columnDefinition = "char(36)")
        private String paymentId; // 성공 시 생성된 paymentId

        @Column(name = "response_status", nullable = false)
        private Integer responseStatus; // 최초 처리 HTTP 상태코드 (예: 200)

        @Column(name = "request_id", columnDefinition = "char(36)", nullable = false)
        private String requestId; // X-Request-Id (E2E 재현용)

        @Column(name = "created_at", nullable = false, updatable = false,
                columnDefinition = "datetime(3)")
        private LocalDateTime createdAt;


        public static IdempotencyRecord create(
                IdempotencyTargetType targetType,
                String orderId,
                String idempotencyKey,
                String paymentId,
                int responseStatus
        ) {
                if (targetType == null) {
                        throw new IllegalArgumentException("targetType은 필수입니다.");
                }
                if (orderId == null || orderId.isBlank()) {
                        throw new IllegalArgumentException("orderId는 필수입니다.");
                }
                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        throw new IllegalArgumentException("idempotencyKey는 필수입니다.");
                }
                if (paymentId == null || paymentId.isBlank()) {
                        throw new IllegalArgumentException("paymentId는 필수입니다.");
                }

                String requestId = RequestIdProvider.current();

                IdempotencyRecord record = new IdempotencyRecord();
                record.targetType = targetType;
                record.targetId = orderId;
                record.idempotencyKey = idempotencyKey;
                record.paymentId = paymentId;
                record.responseStatus = responseStatus;
                record.requestId = requestId;
                record.createdAt = LocalDateTime.now();

                return record;
        }
}