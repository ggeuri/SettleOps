package com.settleops.domain.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/*
* mysql> desc orders;
 * +-------------+--------------+------+-----+---------+-------+
 * | Field       | Type         | Null | Key | Default | Extra |
 * +-------------+--------------+------+-----+---------+-------+
 * | order_id    | char(36)     | NO   | PRI | NULL    |       |
 * | merchant_id | varchar(32)  | NO   | MUL | NULL    |       |
 * | buyer_id    | varchar(32)  | NO   | MUL | NULL    |       |
 * | item_name   | varchar(255) | NO   |     | NULL    |       |
 * | amount      | bigint       | NO   |     | NULL    |       |
 * | currency    | char(3)      | NO   |     | KRW     |       |
 * | status      | varchar(16)  | NO   | MUL | NULL    |       |
 * | created_at  | datetime(6)  | NO   | MUL | NULL    |       |
 * | updated_at  | datetime(6)  | NO   |     | NULL    |       |
 * +-------------+--------------+------+-----+---------+-------+
* */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_merchant", columnList = "merchant_id"),
                @Index(name = "idx_order_buyer", columnList = "buyer_id"),
                @Index(name = "idx_order_status", columnList = "status")
        }
)
public class Orders {

    @Id
    @Column(name = "order_id", columnDefinition = "char(36)", nullable = false, updatable = false)
    private String orderId;   // 서버 발급 불변 ID (UUIDv7/ULID 권장)

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "buyer_id", length = 64, nullable = false)
    private String buyerId;

    @Column(name = "item_name", length = 255, nullable = false)
    private String itemName;

    // KRW 정수 고정 (long / BIGINT)
    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status; // CREATED | PAID (MVP 2상태 LOCKED)

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "datetime(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "datetime(6)")
    private LocalDateTime updatedAt;

    /* =========================
       생성/상태전이 메서드 (SoT 변경은 Service에서만 호출)
       ========================= */

    public static Orders create(String merchantId,
                                String buyerId,
                                String itemName,
                                long amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        Orders orders = new Orders();
        orders.orderId = UUID.randomUUID().toString(); // ULID/UUIDv7 교체 가능
        orders.merchantId = merchantId;
        orders.buyerId = buyerId;
        orders.itemName = itemName;
        orders.amount = amount;
        orders.status = OrderStatus.CREATED;
        orders.createdAt = LocalDateTime.now();
        orders.updatedAt = LocalDateTime.now();
        return orders;
    }

    public void markPaid() {
        if (this.status == OrderStatus.PAID) {
            return; // no-op (멱등 안전)
        }
        this.status = OrderStatus.PAID;
        this.updatedAt = LocalDateTime.now();
    }
}