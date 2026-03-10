package com.settleops.domain.payment.domain;

import com.settleops.global.enums.IdempotencyTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

        @Column(name = "response_status")
        private Integer responseStatus; // 최초 처리 HTTP 상태코드 (예: 200)

        @Column(name = "request_id", columnDefinition = "char(36)", nullable = false)
        private String requestId; // X-Request-Id (E2E 재현용)

        @Column(name = "created_at", nullable = false, updatable = false, insertable = false,
                columnDefinition = "datetime(3)")
        private LocalDateTime createdAt;


        /**
         * 멱등키 선점용 row를 생성한다.
         *
         * <p>동일 (targetType, targetId, idempotencyKey) 요청 중
         * 최초 요청만 insert에 성공하며, 이 시점에는 아직 성공 결과가 없으므로
         * paymentId / responseStatus 는 null 상태로 둔다.</p>
         */
        public static IdempotencyRecord claim(
                IdempotencyTargetType targetType,
                String targetId,
                String idempotencyKey,
                String requestId
        ) {
                IdempotencyRecord record = new IdempotencyRecord();
                record.targetType = targetType;
                record.targetId = targetId;
                record.idempotencyKey = idempotencyKey;
                record.requestId = requestId;
                return record;
        }

        /**
         * 선점된 멱등 row에 성공 결과를 기록한다.
         *
         * <p>pay 성공 이후 같은 트랜잭션 안에서 호출하며,
         * 최종 응답 기준 paymentId / responseStatus / requestId 를 반영한다.</p>
         */
        public void markSucceeded(String paymentId, int responseStatus, String requestId) {
                this.paymentId = paymentId;
                this.responseStatus = responseStatus;
                this.requestId = requestId;
        }
}