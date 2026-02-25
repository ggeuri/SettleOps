package com.settleops.domain.payment.domain;

import com.settleops.domain.order.domain.OrderStatus;
import com.settleops.global.enums.Action;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/*
* mysql> desc payment;
    +------------------+-------------+------+-----+---------+-------+
    | Field            | Type        | Null | Key | Default | Extra |
    +------------------+-------------+------+-----+---------+-------+
    | payment_id       | char(36)    | NO   | PRI | NULL    |       |
    | order_id         | varchar(64) | NO   | MUL | NULL    |       |
    | merchant_id      | varchar(32) | NO   | MUL | NULL    |       |
    | buyer_id         | varchar(32) | NO   | MUL | NULL    |       |
    | currency         | char(3)     | NO   |     | KRW     |       |
    | requested_amount | bigint      | NO   |     | NULL    |       |
    | captured_amount  | bigint      | NO   |     | 0       |       |
    | status           | varchar(32) | NO   | MUL | NULL    |       |
    | created_at       | datetime(6) | NO   | MUL | NULL    |       |
    | updated_at       | datetime(6) | NO   |     | NULL    |       |
    +------------------+-------------+------+-----+---------+-------+
* */

@Getter
@Setter
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_created_at", columnList = "created_at"),
                @Index(name = "idx_payment_order_id", columnList = "order_id"),
                @Index(name = "idx_payment_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_payment_buyer_id", columnList = "buyer_id")
        }
)
public class Payment {

    @Id
    @Column(name = "payment_id", columnDefinition = "char(36)", nullable = false)
    private String paymentId;

    @Column(name = "order_id", columnDefinition = "char(36)", nullable = false)
    private String orderId;

    @Column(name = "merchant_id", length = 32, nullable = false)
    private String merchantId;

    @Column(name = "buyer_id", length = 32, nullable = false)
    private String buyerId;

    @Column(name = "currency", columnDefinition = "char(3)", nullable = false)
    private String currency;

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount;

    @Column(name = "captured_amount", nullable = false)
    private Long capturedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Payment create(
            String orderId,
            String merchantId,
            String buyerId,
            Long amount
    ) {
        Payment payment = new Payment();

        payment.paymentId = UUID.randomUUID().toString();
        payment.orderId = orderId;
        payment.merchantId = merchantId;
        payment.buyerId = buyerId;
        payment.currency = "KRW";
        payment.requestedAmount = amount;
        payment.capturedAmount = 0L;
        payment.status = PaymentStatus.CREATED;
        payment.createdAt = LocalDateTime.now();
        payment.updatedAt = payment.createdAt;

        return payment;
    }


    public void capture() {
        if (this.status == PaymentStatus.CAPTURED) {
            return; // no-op (멱등 안전)
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedAmount = this.requestedAmount; // 전액 캡처 고정
        this.updatedAt = LocalDateTime.now();
    }

}
