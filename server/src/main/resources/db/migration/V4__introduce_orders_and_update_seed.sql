/* =========================================================
   SettleOps - Flyway V4 (Seed)
   File: V4__introduce_orders_and_update_seed.sql

   [목적]
   - 기존 seed의 'ORD_*' 문자열 사용 금지(ORD_ prefix 제거)
   - orders SoT 도입 이후 seed를 orders 기반(UUID)으로 재구성
   - payment.order_id = orders.order_id 정합 유지
   - idempotency_record.target_id = orders.order_id 정합 유지

   [정합 보강(기획서 LOCKED)]
   - CONFIRMED SoT는 payment_event(PAYMENT_CONFIRMED)로만 표현 → 각 케이스에 CONFIRMED 이벤트 추가
   - refund SoT row는 UPDATE 없이 최종 상태로 INSERT(=APPROVED)하되,
     refund_event는 REQUESTED/APPROVED 2개를 insert-only로 남긴다
   - FK는 V99에서 ON (V4에서는 FK 추가 금지)

   [주의]
   - V2 seed와 유니크 키(배치키/정산(merchant,base_date))가 겹치지 않도록
     V4는 날짜를 2026-03-10/11로 사용한다.
   ========================================================= */

-- ---------------------------------------------------------
-- CASE A (refund reflected in settlement: link exists)
-- ---------------------------------------------------------
SET @ord_a  = '0a0a0a0a-0a0a-0a0a-0a0a-0a0a0a0a0a0a';
SET @pay_a  = '41111111-1111-1111-1111-111111111111';
SET @stl_a  = '44444444-4444-4444-4444-444444444440';
SET @hold_a = '55555555-5555-5555-5555-555555555550';
SET @rfd_a  = 'aaaaaaaa-1111-2222-3333-aaaaaaaaaaab';

-- V2 seed와 batch_key / base_date 충돌 방지용
SET @base_date_a = '2026-03-10';
SET @batch_key_a = '2026-03-10';
SET @settlement_no_a = 'SET-20260310-001';

-- request ids (seed용 고정)
SET @req_pay_created_a   = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
SET @req_pay_captured_a  = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
SET @req_pay_confirmed_a = 'abababab-0000-0000-0000-0000000000aa';
SET @req_idem_a          = 'ffffffff-ffff-ffff-ffff-ffffffffffff';

SET @req_batch_a         = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
SET @req_hold_a          = 'gggggggg-gggg-gggg-gggg-gggggggggggg';

SET @req_rfd_requested_a = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
SET @req_rfd_approved_a  = 'bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc';

SET @req_audit_settlement_created_a = 'cccccccc-cccc-cccc-cccc-cccccccccccc';

-- 0) ORDERS (SoT)
INSERT INTO orders (
    order_id, merchant_id, buyer_id, item_name,
    amount, currency, status,
    created_at, updated_at
) VALUES (
             @ord_a, 'MERCHANT_1001', 'BUYER_1001', '테스트 상품(A)',
             10000, 'KRW', 'PAID',
             NOW(6), NOW(6)
         );

-- 1) PAYMENT (order 1:1 payment)
INSERT INTO payment (
    payment_id, order_id, merchant_id, buyer_id,
    currency, requested_amount, captured_amount,
    status, created_at, updated_at
) VALUES (
             @pay_a, @ord_a, 'MERCHANT_1001', 'BUYER_1001',
             'KRW', 10000, 10000,
             'CAPTURED', NOW(6), NOW(6)
         );

-- 2) PAYMENT_EVENT (CREATED → CAPTURED → CONFIRMED)
INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_a,'PAYMENT_CREATED',  @req_pay_created_a,   NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_a,'PAYMENT_CAPTURED', @req_pay_captured_a,  NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_a,'PAYMENT_CONFIRMED',@req_pay_confirmed_a, NOW(6));

-- 3) IDEMPOTENCY_RECORD (target_id = order_id)
INSERT INTO idempotency_record (
    target_type, target_id, idempotency_key,
    payment_id, response_status, request_id, created_at
) VALUES (
             'PAY_ORDER', @ord_a, 'idem-1001-0001',
             @pay_a, 200, @req_idem_a, NOW(3)
         );

-- 4) SETTLEMENT_BATCH (uk_settlement_batch_key 충돌 방지: 2026-03-10 사용)
INSERT INTO settlement_batch (
    batch_key, run_id, triggered_by,
    result, request_id, fail_reason, created_at, finished_at
) VALUES (
             @batch_key_a,
             '33333333-3333-3333-3333-333333333333',
             'SYSTEM',
             'OK',
             @req_batch_a,
             NULL,
             NOW(6), NOW(6)
         );
SET @batch_id_a = LAST_INSERT_ID();

-- 5) SETTLEMENT (uk_settlement_merchant_base_date 충돌 방지: base_date 2026-03-10)
INSERT INTO settlement (
    settlement_id, settlement_no, batch_id,
    merchant_id, status, base_date,
    gross, fee, vat, net,
    paid_requested_by, paid_requested_at,
    paid_approved_by, paid_approved_at,
    created_at, updated_at
) VALUES (
             @stl_a,
             @settlement_no_a,
             @batch_id_a,
             'MERCHANT_1001',
             'READY',
             @base_date_a,
             10000, 1000, 100, 8900,
             NULL, NULL,
             NULL, NULL,
             NOW(6), NOW(6)
         );

-- 6) SETTLEMENT_LINE (PAYMENT)
INSERT INTO settlement_line (
    settlement_id, payment_id, line_type, amount, created_at
) VALUES (
             @stl_a,
             @pay_a,
             'PAYMENT',
             10000,
             NOW(6)
         );

-- 7) HOLD (HOLD_ACTIVE)
INSERT INTO hold (
    hold_id, settlement_id,
    status, requested_reason_code, requested_comment,
    created_by, created_at, updated_at
) VALUES (
             @hold_a,
             @stl_a,
             'HOLD_ACTIVE',
             'MANUAL_REVIEW',
             '테스트 홀드(A)',
             'ADMIN_1',
             NOW(6), NOW(6)
         );

-- 8) HOLD_EVENT (meta_json NOT NULL)
INSERT INTO hold_event (
    hold_id, event_type, request_id,
    actor_type, actor_id,
    occurred_at, meta_json
) VALUES (
             @hold_a,
             'HOLD_APPROVED',
             @req_hold_a,
             'ADMIN',
             'ADMIN_1',
             NOW(6),
             JSON_OBJECT('seed', true, 'case', 'A')
         );

-- 9) REFUND (SoT) + REFUND_EVENT (insert-only)
SET @rfd_decided_at_a = NOW(6);

INSERT INTO refund (
    refund_id, payment_id,
    merchant_id, buyer_id,
    amount, currency,
    status, reason_text,
    requested_at, decided_at,
    created_at, updated_at
) VALUES (
             @rfd_a,
             @pay_a,
             'MERCHANT_1001',
             'BUYER_1001',
             10000, 'KRW',
             'APPROVED',
             '테스트 환불(A)',
             NOW(6), @rfd_decided_at_a,
             NOW(6), NOW(6)
         );

INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_a,
             'REFUND_REQUESTED',
             NULL,
             'REQUESTED',
             @req_rfd_requested_a,
             'BUYER',
             'BUYER_1001',
             NOW(6)
         );

INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_a,
             'REFUND_APPROVED',
             'REQUESTED',
             'APPROVED',
             @req_rfd_approved_a,
             'ADMIN',
             'ADMIN_1',
             @rfd_decided_at_a
         );

-- 10) REFUND_SETTLEMENT_LINK (반영 증거)
INSERT INTO refund_settlement_link (
    refund_id, settlement_id, created_at
) VALUES (
             @rfd_a,
             @stl_a,
             NOW(6)
         );

-- 11) AUDIT_LOG (예시 1개)
INSERT INTO audit_log (
    request_id, occurred_at,
    actor_type, actor_id,
    action, entity_type, entity_id,
    status_before, status_after,
    merchant_id, meta_json
) VALUES (
             @req_audit_settlement_created_a,
             NOW(6),
             'ADMIN',
             'ADMIN_1',
             'SETTLEMENT_CREATED',
             'SETTLEMENT',
             @stl_a,
             NULL,
             'READY',
             'MERCHANT_1001',
             JSON_OBJECT('seed', true, 'case', 'A', 'comment', '정산 생성 + 환불 반영됨(link 존재)')
         );

-- ---------------------------------------------------------
-- CASE B (refund approved but NOT reflected yet: no link)
-- ---------------------------------------------------------
SET @ord_b = '0b0b0b0b-0b0b-0b0b-0b0b-0b0b0b0b0b0b';
SET @pay_b = '42222222-2222-2222-2222-222222222222';
SET @stl_b = '99999999-9999-9999-9999-999999999990';
SET @rfd_b = 'bbbbbbbb-1111-2222-3333-bbbbbbbbbbbc';

-- V2 seed와 batch_key / base_date 충돌 방지용
SET @base_date_b = '2026-03-11';
SET @batch_key_b = '2026-03-11';
SET @settlement_no_b = 'SET-20260311-001';

SET @req_pay_created_b   = '12121212-1212-1212-1212-121212121212';
SET @req_pay_captured_b  = '13131313-1313-1313-1313-131313131313';
SET @req_pay_confirmed_b = 'abababab-0000-0000-0000-0000000000bb';
SET @req_idem_b          = '14141414-1414-1414-1414-141414141414';

SET @req_batch_b         = '15151515-1515-1515-1515-151515151515';

SET @req_rfd_requested_b = '16161616-1616-1616-1616-161616161616';
SET @req_rfd_approved_b  = 'abababab-abab-abab-abab-abababababab';

SET @req_audit_refund_approved_b = '17171717-1717-1717-1717-171717171717';

-- 0) ORDERS (SoT)
INSERT INTO orders (
    order_id, merchant_id, buyer_id, item_name,
    amount, currency, status,
    created_at, updated_at
) VALUES (
             @ord_b, 'MERCHANT_2001', 'BUYER_2001', '테스트 상품(B)',
             25000, 'KRW', 'PAID',
             NOW(6), NOW(6)
         );

-- 1) PAYMENT
INSERT INTO payment (
    payment_id, order_id, merchant_id, buyer_id,
    currency, requested_amount, captured_amount,
    status, created_at, updated_at
) VALUES (
             @pay_b, @ord_b, 'MERCHANT_2001', 'BUYER_2001',
             'KRW', 25000, 25000,
             'CAPTURED', NOW(6), NOW(6)
         );

-- 2) PAYMENT_EVENT (CREATED → CAPTURED → CONFIRMED)
INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_b,'PAYMENT_CREATED',  @req_pay_created_b,   NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_b,'PAYMENT_CAPTURED', @req_pay_captured_b,  NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_b,'PAYMENT_CONFIRMED',@req_pay_confirmed_b, NOW(6));

-- 3) IDEMPOTENCY_RECORD
INSERT INTO idempotency_record (
    target_type, target_id, idempotency_key,
    payment_id, response_status, request_id, created_at
) VALUES (
             'PAY_ORDER', @ord_b, 'idem-2001-0001',
             @pay_b, 200, @req_idem_b, NOW(3)
         );

-- 4) SETTLEMENT_BATCH (uk_settlement_batch_key 충돌 방지: 2026-03-11 사용)
INSERT INTO settlement_batch (
    batch_key, run_id, triggered_by,
    result, request_id, fail_reason, created_at, finished_at
) VALUES (
             @batch_key_b,
             '88888888-8888-8888-8888-888888888888',
             'SYSTEM',
             'OK',
             @req_batch_b,
             NULL,
             NOW(6), NOW(6)
         );
SET @batch_id_b = LAST_INSERT_ID();

-- 5) SETTLEMENT (uk_settlement_merchant_base_date 충돌 방지: base_date 2026-03-11)
INSERT INTO settlement (
    settlement_id, settlement_no, batch_id,
    merchant_id, status, base_date,
    gross, fee, vat, net,
    paid_requested_by, paid_requested_at,
    paid_approved_by, paid_approved_at,
    created_at, updated_at
) VALUES (
             @stl_b,
             @settlement_no_b,
             @batch_id_b,
             'MERCHANT_2001',
             'READY',
             @base_date_b,
             25000, 2500, 250, 22250,
             NULL, NULL,
             NULL, NULL,
             NOW(6), NOW(6)
         );

-- 6) SETTLEMENT_LINE (PAYMENT only)
INSERT INTO settlement_line (
    settlement_id, payment_id, line_type, amount, created_at
) VALUES (
             @stl_b,
             @pay_b,
             'PAYMENT',
             25000,
             NOW(6)
         );

-- 7) REFUND (APPROVED but NOT reflected yet: no refund_settlement_link)
SET @rfd_decided_at_b = NOW(6);

INSERT INTO refund (
    refund_id, payment_id,
    merchant_id, buyer_id,
    amount, currency,
    status, reason_text,
    requested_at, decided_at,
    created_at, updated_at
) VALUES (
             @rfd_b,
             @pay_b,
             'MERCHANT_2001',
             'BUYER_2001',
             5000, 'KRW',
             'APPROVED',
             '부분 환불 요청(B) - 승인 후 다음 정산에 반영 예정',
             NOW(6), @rfd_decided_at_b,
             NOW(6), NOW(6)
         );

INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_b,
             'REFUND_REQUESTED',
             NULL,
             'REQUESTED',
             @req_rfd_requested_b,
             'BUYER',
             'BUYER_2001',
             NOW(6)
         );

INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_b,
             'REFUND_APPROVED',
             'REQUESTED',
             'APPROVED',
             @req_rfd_approved_b,
             'ADMIN',
             'ADMIN_2',
             @rfd_decided_at_b
         );

-- 8) AUDIT_LOG (refund approved but link 없음)
INSERT INTO audit_log (
    request_id, occurred_at,
    actor_type, actor_id,
    action, entity_type, entity_id,
    status_before, status_after,
    merchant_id, meta_json
) VALUES (
             @req_audit_refund_approved_b,
             NOW(6),
             'ADMIN',
             'ADMIN_2',
             'REFUND_APPROVED',
             'REFUND',
             @rfd_b,
             'REQUESTED',
             'APPROVED',
             'MERCHANT_2001',
             JSON_OBJECT('seed', true, 'case', 'B', 'comment', '환불 승인됐지만 refund_settlement_link 없음(미반영)')
         );

-- refund_settlement_link는 의도적으로 없음 (REFUND_ADJUSTMENT_PENDING 재현용)