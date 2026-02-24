/* =========================================================
   SettleOps - Flyway V3
   File: V3__create_orders_and_align_payment_order_id.sql

   [목적]
   - orders 테이블(주문 SoT) 신규 도입
   - payment.order_id를 orders.order_id와 타입/의미 정합(CHAR(36))으로 맞춤
   - FK는 V99에서 일괄 ON (V3에서는 FK 추가 금지)

   [주의]
   - 기존 로컬 DB에 payment.order_id가 'ORD_1001' 같은 문자열로 이미 적재되어 있으면
     MODIFY(order_id CHAR(36)) 단계에서 실패할 수 있습니다.
     → 로컬 개발환경이면 DB drop 후 재마이그레이션 권장.
   ========================================================= */

-- =========================================================
-- 1) ORDERS (SoT) 신규 도입
--   - MVP: CREATED / PAID
--   - order_id: UUID(CHAR(36)) 기준
-- =========================================================
CREATE TABLE IF NOT EXISTS orders (
                                      order_id     CHAR(36)     NOT NULL COMMENT '주문 고유 ID (UUID/ULID. MVP는 CHAR(36)로 통일)',
    merchant_id  VARCHAR(32)  NOT NULL COMMENT '가맹점 ID',
    buyer_id     VARCHAR(32)  NOT NULL COMMENT '구매자 ID',
    item_name    VARCHAR(255) NOT NULL COMMENT '상품명(표시용, MVP 최소)',
    amount       BIGINT       NOT NULL COMMENT 'KRW 정수(원) - 소수 금지',
    currency     CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT 'MVP: KRW 고정',
    status       VARCHAR(16)  NOT NULL COMMENT 'CREATED / PAID (MVP LOCKED)',
    created_at   DATETIME(6)  NOT NULL COMMENT '생성 시각',
    updated_at   DATETIME(6)  NOT NULL COMMENT '수정 시각',

    PRIMARY KEY (order_id),

    KEY idx_orders_merchant_id (merchant_id),
    KEY idx_orders_buyer_id (buyer_id),
    KEY idx_orders_status (status),
    KEY idx_orders_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='주문 SoT (MVP: CREATED/PAID)';

-- =========================================================
-- 2) PAYMENT.order_id 타입/의미 정합
--   - V1: VARCHAR(64) (seed 식별자)
--   - V3: CHAR(36) (orders.order_id 참조 키)
--   - FK는 V99에서 ON
-- =========================================================
ALTER TABLE payment
    MODIFY order_id CHAR(36) NOT NULL
    COMMENT 'orders.order_id 참조 키 (FK는 V99에서 ON)';

-- =========================================================
-- 3) MVP 1:1 강제 (LOCKED)
--   - order 1 : payment 1
-- =========================================================
ALTER TABLE payment
    ADD UNIQUE KEY uk_payment_order_id (order_id);