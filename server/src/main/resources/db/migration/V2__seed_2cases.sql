/* =========================================================
   SettleOps - Seed Master Script (2 cases) [LOCKED CORE 호환]
   Case A: refund reflected in settlement (link exists)
   Case B: refund approved but NOT reflected yet (no link)
   - 최신 DDL에 맞게 "필수로 깨지는 부분만" 수정
   - refund는 REQUESTED 포함(요청 -> 승인 전이 재현)
   ========================================================= */

-- ---------------------------------------------------------
-- CASE A
-- ---------------------------------------------------------
SET @pay_a = '11111111-1111-1111-1111-111111111111';
SET @stl_a = '44444444-4444-4444-4444-444444444444';
SET @hold_a = '55555555-5555-5555-5555-555555555555';
SET @rfd_a = 'aaaaaaaa-1111-2222-3333-aaaaaaaaaaaa';

-- 0) PAYMENT
INSERT INTO payment (
    payment_id, order_id, merchant_id, buyer_id,
    currency, requested_amount, captured_amount,
    status, created_at, updated_at
) VALUES (
             @pay_a, 'ORD_1001', 'MERCHANT_1001', 'BUYER_1001',
             'KRW', 10000, 10000,
             'CAPTURED', NOW(6), NOW(6)
         );

-- 1) PAYMENT_EVENT  (event_id 컬럼 없음, PK는 AI)
INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_a,'PAYMENT_CREATED','dddddddd-dddd-dddd-dddd-dddddddddddd', NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_a,'PAYMENT_CAPTURED','eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NOW(6));

-- 2) IDEMPOTENCY_RECORD (payment_id UUID)
INSERT INTO idempotency_record (
    target_type, target_id, idempotency_key,
    payment_id, response_status, request_id, created_at
) VALUES (
             'PAY_ORDER', 'ORD_1001', 'idem-1001-0001',
             @pay_a, 200, 'ffffffff-ffff-ffff-ffff-ffffffffffff', NOW(3)
         );

-- 3) SETTLEMENT_BATCH
INSERT INTO settlement_batch (
    batch_key, run_id, triggered_by,
    result, request_id, fail_reason, created_at, finished_at
) VALUES (
             '2026-02-10',
             '33333333-3333-3333-3333-333333333333',
             'SYSTEM',
             'OK',
             'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
             NULL,
             NOW(6), NOW(6)
         );
SET @batch_id_a = LAST_INSERT_ID();

-- 4) SETTLEMENT (READY)
INSERT INTO settlement (
    settlement_id, settlement_no, batch_id,
    merchant_id, status, base_date,
    gross, fee, vat, net,
    paid_requested_by, paid_requested_at,
    paid_approved_by, paid_approved_at,
    created_at, updated_at
) VALUES (
             @stl_a,
             'SET-20260210-001',
             @batch_id_a,
             'MERCHANT_1001',
             'READY',
             '2026-02-10',
             10000, 1000, 100, 8900,
             NULL, NULL,
             NULL, NULL,
             NOW(6), NOW(6)
         );

-- 5) SETTLEMENT_LINE (PAYMENT)
INSERT INTO settlement_line (
    settlement_id, payment_id, line_type, amount, created_at
) VALUES (
             @stl_a,
             @pay_a,
             'PAYMENT',
             10000,
             NOW(6)
         );

-- 6) HOLD (HOLD_ACTIVE)
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

-- 7) HOLD_EVENT (meta_json NOT NULL)
INSERT INTO hold_event (
    hold_id, event_type, request_id,
    actor_type, actor_id,
    occurred_at, meta_json
) VALUES (
             @hold_a,
             'HOLD_APPROVED',
             'gggggggg-gggg-gggg-gggg-gggggggggggg',
             'ADMIN',
             'ADMIN_1',
             NOW(6),
             JSON_OBJECT('seed', true, 'case', 'A')
         );

-- 8) REFUND: REQUESTED로 생성 후 APPROVED로 전이
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
             'REQUESTED',
             '테스트 환불(A)',
             NOW(6), NULL,
             NOW(6), NOW(6)
         );

-- REFUND_EVENT: REQUESTED (refund_event_id는 AI)
INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_a,
             'REFUND_REQUESTED',
             NULL,
             'REQUESTED',
             'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
             'BUYER',
             'BUYER_1001',
             NOW(6)
         );

-- APPROVED로 전이
UPDATE refund
SET status='APPROVED',
    decided_at=NOW(6),
    updated_at=NOW(6)
WHERE refund_id=@rfd_a;

-- REFUND_EVENT: APPROVED
INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_a,
             'REFUND_APPROVED',
             'REQUESTED',
             'APPROVED',
             'bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc',
             'ADMIN',
             'ADMIN_1',
             NOW(6)
         );

-- 9) REFUND_SETTLEMENT_LINK (refund_id가 PK, link_id 없음)
INSERT INTO refund_settlement_link (
    refund_id, settlement_id, created_at
) VALUES (
             @rfd_a,
             @stl_a,
             NOW(6)
         );

-- 10) AUDIT_LOG
INSERT INTO audit_log (
    request_id, occurred_at,
    actor_type, actor_id,
    action, entity_type, entity_id,
    status_before, status_after,
    merchant_id, meta_json
) VALUES (
             'cccccccc-cccc-cccc-cccc-cccccccccccc',
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
-- CASE B (환불 승인됐지만 정산에 아직 미반영: link 없음)
-- ---------------------------------------------------------
SET @pay_b = '22222222-2222-2222-2222-222222222222';
SET @stl_b = '99999999-9999-9999-9999-999999999999';
SET @rfd_b = 'bbbbbbbb-1111-2222-3333-bbbbbbbbbbbb';

-- 0) PAYMENT
INSERT INTO payment (
    payment_id, order_id, merchant_id, buyer_id,
    currency, requested_amount, captured_amount,
    status, created_at, updated_at
) VALUES (
             @pay_b, 'ORD_2001', 'MERCHANT_2001', 'BUYER_2001',
             'KRW', 25000, 25000,
             'CAPTURED', NOW(6), NOW(6)
         );

-- 1) PAYMENT_EVENT
INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_b,'PAYMENT_CREATED','12121212-1212-1212-1212-121212121212', NOW(6));

INSERT INTO payment_event (payment_id, event_type, request_id, occurred_at)
VALUES (@pay_b,'PAYMENT_CAPTURED','13131313-1313-1313-1313-131313131313', NOW(6));

-- 2) IDEMPOTENCY_RECORD
INSERT INTO idempotency_record (
    target_type, target_id, idempotency_key,
    payment_id, response_status, request_id, created_at
) VALUES (
             'PAY_ORDER', 'ORD_2001', 'idem-2001-0001',
             @pay_b, 200, '14141414-1414-1414-1414-141414141414', NOW(3)
         );

-- 3) SETTLEMENT_BATCH
INSERT INTO settlement_batch (
    batch_key, run_id, triggered_by,
    result, request_id, fail_reason, created_at, finished_at
) VALUES (
             '2026-02-11',
             '88888888-8888-8888-8888-888888888888',
             'SYSTEM',
             'OK',
             '15151515-1515-1515-1515-151515151515',
             NULL,
             NOW(6), NOW(6)
         );
SET @batch_id_b = LAST_INSERT_ID();

-- 4) SETTLEMENT
INSERT INTO settlement (
    settlement_id, settlement_no, batch_id,
    merchant_id, status, base_date,
    gross, fee, vat, net,
    paid_requested_by, paid_requested_at,
    paid_approved_by, paid_approved_at,
    created_at, updated_at
) VALUES (
             @stl_b,
             'SET-20260211-001',
             @batch_id_b,
             'MERCHANT_2001',
             'READY',
             '2026-02-11',
             25000, 2500, 250, 22250,
             NULL, NULL,
             NULL, NULL,
             NOW(6), NOW(6)
         );

-- 5) SETTLEMENT_LINE (PAYMENT만 반영)
INSERT INTO settlement_line (
    settlement_id, payment_id, line_type, amount, created_at
) VALUES (
             @stl_b,
             @pay_b,
             'PAYMENT',
             25000,
             NOW(6)
         );

-- 6) REFUND: REQUESTED -> APPROVED, BUT NOT LINKED
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
             'REQUESTED',
             '부분 환불 요청(B) - 승인 후 다음 정산에 반영 예정',
             NOW(6), NULL,
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
             '16161616-1616-1616-1616-161616161616',
             'BUYER',
             'BUYER_2001',
             NOW(6)
         );

UPDATE refund
SET status='APPROVED',
    decided_at=NOW(6),
    updated_at=NOW(6)
WHERE refund_id=@rfd_b;

INSERT INTO refund_event (
    refund_id,
    event_type, status_before, status_after,
    request_id, actor_type, actor_id, occurred_at
) VALUES (
             @rfd_b,
             'REFUND_APPROVED',
             'REQUESTED',
             'APPROVED',
             'abababab-abab-abab-abab-abababababab',
             'ADMIN',
             'ADMIN_2',
             NOW(6)
         );

-- 7) AUDIT_LOG (refund approved, not reflected)
INSERT INTO audit_log (
    request_id, occurred_at,
    actor_type, actor_id,
    action, entity_type, entity_id,
    status_before, status_after,
    merchant_id, meta_json
) VALUES (
             '17171717-1717-1717-1717-171717171717',
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

-- refund_settlement_link는 의도적으로 없음