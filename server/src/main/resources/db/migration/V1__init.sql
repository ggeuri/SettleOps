/* =========================================================
   SettleOps - Flyway V1 Init (MySQL 8.x.44)  [LOCKED CORE]
   - DROP 없음 (Flyway 누적)
   - FK는 OFF (통합 직전 V99에서 ON)
   - CHECK 최소화(DEV/TEST 발목 방지) → 필요 시 V99에서 강화
   - meta_json(JSON) : NOT NULL (LOCKED: 운영 재현 품질 보호)
   - request_id: CHAR(36) NOT NULL + INDEX (event/audit)  [LOCKED]
   - entity_id: UUID CHAR(36) 권장(기획서 정합)
   - event PK: BIGINT AUTO_INCREMENT 권장(운영/정렬/페이징)
   ========================================================= */

-- NOTE: SET NAMES ... 는 환경/커넥션 설정과 충돌 가능성이 있어 실행 구문 제거(주석만 유지)
-- SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- =========================================================
-- 1) PAYMENT (SoT)
-- =========================================================
CREATE TABLE IF NOT EXISTS payment (
                                       payment_id        CHAR(36)    NOT NULL COMMENT '결제 고유 ID (UUID)',
    order_id          VARCHAR(64) NOT NULL COMMENT '주문 ID(seed 식별자). 멱등 SoT는 idempotency_record',

    merchant_id       VARCHAR(32) NOT NULL COMMENT '가맹점 ID',
    buyer_id          VARCHAR(32) NOT NULL COMMENT '구매자 ID',

    currency          CHAR(3)     NOT NULL DEFAULT 'KRW' COMMENT '통화 (MVP: KRW)',
    requested_amount  BIGINT      NOT NULL COMMENT '결제 요청 금액(KRW 원 단위)',
    captured_amount   BIGINT      NOT NULL DEFAULT 0 COMMENT '승인/캡처 금액(KRW 원 단위)',

    status            VARCHAR(32) NOT NULL COMMENT 'CREATED / CAPTURED',

    created_at        DATETIME(6) NOT NULL COMMENT '생성 일시',
    updated_at        DATETIME(6) NOT NULL COMMENT '수정 일시',

    PRIMARY KEY (payment_id),

    -- order_id UNIQUE 제거(기획서: 주문=결제 1개 강제 금지). 멱등은 idempotency_record가 SoT.
    KEY idx_payment_order_id (order_id),
    KEY idx_payment_merchant_id (merchant_id),
    KEY idx_payment_buyer_id (buyer_id),
    KEY idx_payment_status (status),
    KEY idx_payment_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='결제 SoT';

-- =========================================================
-- 2) PAYMENT_EVENT (insert-only)
--   - PK는 BIGINT(AI) : 운영/정렬/페이지네이션 유리
--   - CONFIRMED는 event로만 관리(LOCKED)
-- =========================================================
CREATE TABLE IF NOT EXISTS payment_event (
                                             payment_event_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '이벤트 PK(AI)',
                                             payment_id       CHAR(36)    NOT NULL COMMENT 'payment.payment_id (V99에서 FK ON)',
    event_type       VARCHAR(32) NOT NULL COMMENT 'PAYMENT_CREATED / PAYMENT_CAPTURED / PAYMENT_CONFIRMED',

    status_before    VARCHAR(64) NULL COMMENT '상태 전이 전(옵션)',
    status_after     VARCHAR(64) NULL COMMENT '상태 전이 후(옵션)',

    request_id       CHAR(36)    NOT NULL COMMENT 'X-Request-Id [LOCKED]',
    occurred_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '이벤트 발생 일시',

    PRIMARY KEY (payment_event_id),

    -- (옵션) 동일 이벤트 중복 insert 방지: 같은 payment에 같은 type은 1회만
    UNIQUE KEY uk_payment_event_payment_type (payment_id, event_type),

    KEY idx_payment_event_request_id (request_id),
    KEY idx_payment_event_payment_id_occurred_at (payment_id, occurred_at),
    KEY idx_payment_event_type_occurred_at (event_type, occurred_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='결제 이벤트(insert-only). CONFIRMED는 event로만 관리';

-- =========================================================
-- 3) IDEMPOTENCY_RECORD (SoT)
-- =========================================================
CREATE TABLE IF NOT EXISTS idempotency_record (
                                                  idempotency_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',

                                                  target_type       VARCHAR(32)   NOT NULL DEFAULT 'PAY_ORDER' COMMENT 'LOCKED: PAY_ORDER',
    target_id         VARCHAR(64)   NOT NULL COMMENT 'LOCKED: orderId',
    idempotency_key   VARCHAR(128)  NOT NULL COMMENT '헤더 X-Idempotency-Key',

    payment_id        CHAR(36)      NULL COMMENT '결과 리소스(성공 시 paymentId)',
    response_status   INT           NOT NULL COMMENT '최초 처리 HTTP 상태코드(예: 200)',
    request_id        CHAR(36)      NOT NULL COMMENT 'E2E 재현용 requestId (LOCKED)',

    created_at        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',

    PRIMARY KEY (idempotency_id),

    UNIQUE KEY uk_idempotency (target_type, target_id, idempotency_key),

    KEY idx_idem_target (target_type, target_id),
    KEY idx_idem_request_id (request_id),
    KEY idx_idem_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='멱등 처리 레코드(SoT, pay/orderId 기준)';

-- =========================================================
-- 4) SETTLEMENT_BATCH (SoT)
-- =========================================================
CREATE TABLE IF NOT EXISTS settlement_batch (
                                                batch_id      BIGINT      NOT NULL AUTO_INCREMENT COMMENT '시스템 내부 식별자 (PK)',
                                                batch_key     DATE        NOT NULL COMMENT '[LOCKED] 정산 기준일(KST). 중복 실행 방지(UNIQUE)',
                                                run_id        CHAR(36)    NOT NULL COMMENT '[Trace] 실행 식별자 (UUID)',
    triggered_by  VARCHAR(50) NOT NULL COMMENT '실행 주체 (SYSTEM/ADMIN:xxx)',
    result        VARCHAR(20) NOT NULL COMMENT '결과: OK, FAIL (LOCKED: SKIP은 audit_log로만)',

    request_id    CHAR(36)    NOT NULL COMMENT '[Trace] X-Request-Id (LOCKED)',

    fail_reason   TEXT        NULL COMMENT '[운영] FAIL 시 에러 메시지 원본(옵션)',
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '배치 시작/기록 시각',
    finished_at   DATETIME(6) NULL COMMENT '[운영] 배치 종료 시각(옵션)',

    PRIMARY KEY (batch_id),

    UNIQUE KEY uk_settlement_batch_key (batch_key),

    KEY idx_settlement_batch_request_id (request_id),
    KEY idx_settlement_batch_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='정산 배치 실행 이력(OK/FAIL만 저장, SKIP은 audit_log)';

-- =========================================================
-- 5) SETTLEMENT (SoT)
--   - U4/U5/A3 실사용 최적: (merchant_id, base_date) 인덱스
--   - (권장) merchant_id+base_date UNIQUE : merchant/day 1건 SoT 강화
-- =========================================================
CREATE TABLE IF NOT EXISTS settlement (
                                          settlement_id        CHAR(36)    NOT NULL COMMENT '정산 ID (UUID)',
    settlement_no        VARCHAR(50) NOT NULL COMMENT '표시용 정산 번호(예: SET-YYYYMMDD-001)',
    batch_id             BIGINT      NOT NULL COMMENT 'settlement_batch.batch_id (V99에서 FK ON)',
    merchant_id          VARCHAR(32) NOT NULL COMMENT '판매자(가맹점) ID',

    status               VARCHAR(32) NOT NULL COMMENT 'READY/HOLD_ACTIVE/PAY_REQUESTED/PAID',
    base_date            DATE        NOT NULL COMMENT '정산 기준일(KST)',

    gross                BIGINT      NOT NULL COMMENT '총액(집계 SoT)',
    fee                  BIGINT      NOT NULL COMMENT '수수료(집계 SoT)',
    vat                  BIGINT      NOT NULL COMMENT '부가세(집계 SoT)',
    net                  BIGINT      NOT NULL COMMENT '정산액(집계 SoT)',

    paid_requested_by    VARCHAR(32) NULL COMMENT '지급 요청자(4-eyes)',
    paid_requested_at    DATETIME(6) NULL COMMENT '지급 요청 시각',
    paid_approved_by     VARCHAR(32) NULL COMMENT '지급 승인자(4-eyes)',
    paid_approved_at     DATETIME(6) NULL COMMENT '지급 승인 시각',

    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,

    PRIMARY KEY (settlement_id),

    -- 상태/운영 조회
    KEY idx_settlement_status (status),
    KEY idx_settlement_base_date (base_date),
    KEY idx_settlement_batch (batch_id),

    -- 핵심 조회 패턴(merchant + 기간)
    KEY idx_settlement_merchant_base_date (merchant_id, base_date),

    -- Admin 리스트 최적(옵션이지만 유용)
    KEY idx_settlement_status_base_date (status, base_date),

    -- (권장) merchant/day 1건 SoT 강화 (배치 집계 모델과 정합)
    UNIQUE KEY uk_settlement_merchant_base_date (merchant_id, base_date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='정산 헤더 SoT(집계 결과)';

-- =========================================================
-- 6) SETTLEMENT_LINE (Evidence / 검증 근거)
-- =========================================================
CREATE TABLE IF NOT EXISTS settlement_line (
                                               settlement_line_id  BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'PK (AI)',
                                               settlement_id       CHAR(36)    NOT NULL COMMENT 'settlement.settlement_id (V99에서 FK ON)',
    payment_id          CHAR(36)    NOT NULL COMMENT 'payment.payment_id (V99에서 FK ON)',
    line_type           VARCHAR(20) NOT NULL COMMENT 'PAYMENT, REFUND',
    amount              BIGINT      NOT NULL COMMENT '항상 양수, 부호는 line_type으로 해석',
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    PRIMARY KEY (settlement_line_id),

    KEY idx_settlement_line_payment_id (payment_id),
    KEY idx_settlement_line_settlement_id (settlement_id),
    KEY idx_settlement_line_type (line_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='정산 상세 내역(검증 근거). amount는 양수, 부호는 line_type으로만 해석';

-- =========================================================
-- 7) HOLD (SoT)
--   - settlement_id UNIQUE (MVP 필수)
--   - requested_comment: 스냅샷(immutable)
-- =========================================================
CREATE TABLE IF NOT EXISTS hold (
                                    hold_id               CHAR(36)    NOT NULL COMMENT 'UUID',
    settlement_id         CHAR(36)    NOT NULL COMMENT 'settlement.settlement_id (V99에서 FK ON)',

    status                VARCHAR(32) NOT NULL COMMENT 'HOLD_REQUESTED / HOLD_ACTIVE / RELEASED',
    requested_reason_code VARCHAR(64) NOT NULL COMMENT 'MANUAL_REVIEW / RISK_SUSPECTED',
    requested_comment     TEXT        NOT NULL COMMENT '최초 요청 코멘트 스냅샷(immutable)',

    created_by            VARCHAR(32) NOT NULL COMMENT '최초 요청자(immutable)',
    created_at            DATETIME(6) NOT NULL,
    updated_at            DATETIME(6) NOT NULL,

    PRIMARY KEY (hold_id),

    UNIQUE KEY uk_hold_settlement_id (settlement_id),

    KEY idx_hold_status (status),
    KEY idx_hold_created_at (created_at),
    KEY idx_hold_updated_at (updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='Hold SoT';

-- =========================================================
-- 8) HOLD_EVENT (insert-only)
--   - meta_json: NOT NULL (LOCKED)
-- =========================================================
CREATE TABLE IF NOT EXISTS hold_event (
                                          hold_event_id BIGINT      NOT NULL AUTO_INCREMENT,
                                          hold_id       CHAR(36)    NOT NULL COMMENT 'hold.hold_id (V99에서 FK ON)',
    event_type    VARCHAR(32) NOT NULL,
    request_id    CHAR(36)    NOT NULL,
    actor_type    VARCHAR(32) NOT NULL,
    actor_id      VARCHAR(32) NOT NULL,
    occurred_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    meta_json     JSON        NOT NULL DEFAULT (JSON_OBJECT()) COMMENT 'LOCKED: 재현 품질 보호',

    PRIMARY KEY (hold_event_id),

    KEY idx_hold_event_request_id (request_id),
    KEY idx_hold_event_hold_id_occurred_at (hold_id, occurred_at),
    KEY idx_hold_event_type (event_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='Hold 이벤트(insert-only)';

-- =========================================================
-- 9) REFUND (SoT)  [MVP: payment_id 당 1회 환불]
-- =========================================================
CREATE TABLE IF NOT EXISTS refund (
                                      refund_id     CHAR(36)     NOT NULL COMMENT '환불 ID (UUID)',
    payment_id    CHAR(36)     NOT NULL COMMENT 'payment.payment_id (V99에서 FK ON)',
    merchant_id   VARCHAR(32)  NOT NULL COMMENT 'merchant 스냅샷(불변 취급)',
    buyer_id      VARCHAR(32)  NOT NULL COMMENT 'buyer 스냅샷(불변 취급)',

    amount        BIGINT       NOT NULL COMMENT 'KRW 정수(원)',
    currency      CHAR(3)      NOT NULL DEFAULT 'KRW' COMMENT '통화 (MVP: KRW)',

    status        VARCHAR(32)  NOT NULL COMMENT 'REQUESTED/APPROVED/REJECTED',
    reason_text   VARCHAR(255) NOT NULL COMMENT '환불 사유(텍스트, MVP 필수)',

    requested_at  DATETIME(6)  NOT NULL COMMENT '업무상 요청 발생 시각',
    decided_at    DATETIME(6)  NULL COMMENT '승인/거절 시각(결정 전 NULL)',

    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'row 생성 시각',
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'row 갱신 시각',

    PRIMARY KEY (refund_id),

    -- [LOCKED/MVP] payment_id당 환불 1회만 가능
    UNIQUE KEY uk_refund_payment (payment_id),

    KEY idx_refund_status (status),
    KEY idx_refund_merchant (merchant_id),
    KEY idx_refund_buyer (buyer_id),
    KEY idx_refund_requested_at (requested_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='환불 SoT(REQUESTED→APPROVED/REJECTED). REFUNDED 금지(LOCKED)';

-- =========================================================
-- 10) REFUND_EVENT (insert-only)
--   - PK는 BIGINT(AI)
-- =========================================================
CREATE TABLE IF NOT EXISTS refund_event (
                                            refund_event_id BIGINT      NOT NULL AUTO_INCREMENT COMMENT '이벤트 PK(AI)',
                                            refund_id       CHAR(36)    NOT NULL COMMENT 'refund.refund_id (V99에서 FK ON)',

    event_type      VARCHAR(32) NOT NULL COMMENT 'REFUND_REQUESTED / REFUND_APPROVED / REFUND_REJECTED',
    status_before   VARCHAR(32) NULL,
    status_after    VARCHAR(32) NOT NULL,

    request_id      CHAR(36)    NOT NULL,
    actor_type      VARCHAR(16) NOT NULL,
    actor_id        VARCHAR(32) NOT NULL,
    occurred_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (refund_event_id),

    KEY idx_rfe_refund (refund_id),
    KEY idx_rfe_request (request_id),
    KEY idx_rfe_occurred (occurred_at),
    KEY idx_rfe_refund_occurred (refund_id, occurred_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='환불 이벤트(insert-only)';

-- =========================================================
-- 11) REFUND_SETTLEMENT_LINK (Evidence SoT, insert-only)
--   [LOCKED] 컬럼: refund_id, settlement_id, created_at
--   [LOCKED] UNIQUE(refund_id), INDEX(settlement_id)
--   -> link_id 제거 (증거 SoT를 단순/강제)
-- =========================================================
CREATE TABLE IF NOT EXISTS refund_settlement_link (
                                                      refund_id     CHAR(36)    NOT NULL COMMENT 'refund.refund_id (V99에서 FK ON)',
    settlement_id CHAR(36)    NOT NULL COMMENT 'settlement.settlement_id (V99에서 FK ON)',
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (refund_id),

    KEY idx_rsl_settlement (settlement_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='환불이 특정 settlement에 반영되었다는 증거(insert-only, SoT=refund_id 단일)';

-- =========================================================
-- 12) AUDIT_LOG (insert-only)
--   - meta_json: NOT NULL (LOCKED)
-- =========================================================
CREATE TABLE IF NOT EXISTS audit_log (
                                         audit_id       BIGINT      NOT NULL AUTO_INCREMENT,
                                         request_id     CHAR(36)    NOT NULL,
    occurred_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    actor_type     VARCHAR(32) NOT NULL,
    actor_id       VARCHAR(32) NOT NULL,

    action         VARCHAR(64) NOT NULL,
    entity_type    VARCHAR(32) NOT NULL,
    entity_id      CHAR(36)    NOT NULL,

    status_before  VARCHAR(64) NULL,
    status_after   VARCHAR(64) NULL,

    merchant_id    VARCHAR(32) NULL,

    meta_json      JSON        NOT NULL DEFAULT (JSON_OBJECT()) COMMENT 'LOCKED: 재현 품질 보호',

    PRIMARY KEY (audit_id),

    KEY idx_audit_request_id (request_id),
    KEY idx_audit_entity_time (entity_type, entity_id, occurred_at),
    KEY idx_audit_merchant_id (merchant_id),
    KEY idx_audit_occurred_at (occurred_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
    COMMENT='행위 로그(insert-only). 요청 단위 재현용';

-- =========================================================
-- [DEV NOTE] FK/CHECK 강화는 V99__fk_on.sql에서 적용
-- =========================================================