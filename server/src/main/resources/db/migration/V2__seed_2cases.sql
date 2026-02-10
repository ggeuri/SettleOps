/* =========================================================
   SettleOps - Seed Master Script (2 cases)
   Case A: refund reflected in settlement (link exists)
   Case B: refund approved but NOT reflected yet (no link)
   ========================================================= */

-- ---------------------------------------------------------
-- CASE A
-- ---------------------------------------------------------

/* 0) PAYMENT */
INSERT INTO payment (
  payment_id, order_id, merchant_id, buyer_id,
  currency, requested_amount, captured_amount,
  status, created_at, updated_at
) VALUES (
  'PAY_1001', 'ORD_1001', 'MERCHANT_1001', 'BUYER_1001',
  'KRW', 10000, 10000,
  'CAPTURED', NOW(), NOW()
);

/* 1) PAYMENT_EVENT */
INSERT INTO payment_event (event_id, payment_id, event_type, request_id, occurred_at)
VALUES ('11111111-1111-1111-1111-111111111111','PAY_1001','PAYMENT_CREATED','dddddddd-dddd-dddd-dddd-dddddddddddd', NOW());

INSERT INTO payment_event (event_id, payment_id, event_type, request_id, occurred_at)
VALUES ('22222222-2222-2222-2222-222222222222','PAY_1001','PAYMENT_CAPTURED','eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NOW());

/* 2) IDEMPOTENCY_RECORD */
INSERT INTO idempotency_record (
  target_type, target_id, idempotency_key,
  payment_id, response_status, request_id, created_at
) VALUES (
  'PAY_ORDER', 'ORD_1001', 'idem-1001-0001',
  'PAY_1001', 200, 'ffffffff-ffff-ffff-ffff-ffffffffffff', NOW(3)
);

/* 3) SETTLEMENT_BATCH */
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
  NOW(), NOW()
);
SET @batch_id_a = LAST_INSERT_ID();

/* 4) SETTLEMENT (READY) */
INSERT INTO settlement (
  settlement_id, settlement_no, batch_id,
  merchant_id, status, base_date,
  gross, fee, vat, net,
  paid_requested_by, paid_requested_at,
  paid_approved_by, paid_approved_at,
  created_at, updated_at
) VALUES (
  '44444444-4444-4444-4444-444444444444',
  'SET-20260210-001',
  @batch_id_a,
  'MERCHANT_1001',
  'READY',
  '2026-02-10',
  10000, 1000, 100, 8900,
  NULL, NULL,
  NULL, NULL,
  NOW(), NOW()
);

/* 5) SETTLEMENT_LINE (PAYMENT) */
INSERT INTO settlement_line (
  settlement_id, payment_id, line_type, amount, created_at
) VALUES (
  '44444444-4444-4444-4444-444444444444',
  'PAY_1001',
  'PAYMENT',
  10000,
  NOW()
);

/* 6) HOLD (HOLD_ACTIVE) */
INSERT INTO hold (
  hold_id, settlement_id,
  status, requested_reason_code, requested_comment,
  created_by, created_at, updated_at
) VALUES (
  '55555555-5555-5555-5555-555555555555',
  '44444444-4444-4444-4444-444444444444',
  'HOLD_ACTIVE',
  'MANUAL_REVIEW',
  '테스트 홀드(A)',
  'ADMIN_1',
  NOW(), NOW()
);

/* 7) HOLD_EVENT (HOLD_APPROVED) */
INSERT INTO hold_event (
  hold_id, event_type, request_id,
  actor_type, actor_id,
  occurred_at, meta_json
) VALUES (
  '55555555-5555-5555-5555-555555555555',
  'HOLD_APPROVED',
  'gggggggg-gggg-gggg-gggg-gggggggggggg',
  'ADMIN',
  'ADMIN_1',
  NOW(),
  JSON_OBJECT('seed', true, 'case', 'A')
);

/* 8) REFUND (APPROVED) */
INSERT INTO refund (
  refund_id, payment_id,
  merchant_id, buyer_id,
  amount, currency,
  status, reason_text,
  requested_at, decided_at,
  created_at, updated_at
) VALUES (
  'RFD_1001',
  'PAY_1001',
  'MERCHANT_1001',
  'BUYER_1001',
  10000, 'KRW',
  'APPROVED',
  '테스트 환불(A)',
  NOW(), NOW(),
  NOW(), NOW()
);

/* 9) REFUND_EVENT (REFUND_APPROVED) */
INSERT INTO refund_event (
  refund_event_id, refund_id,
  event_type, status_before, status_after,
  request_id, actor_type, actor_id, occurred_at
) VALUES (
  'RFE_1001',
  'RFD_1001',
  'REFUND_APPROVED',
  'REQUESTED',
  'APPROVED',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  'ADMIN',
  'ADMIN_1',
  NOW()
);

/* 10) REFUND_SETTLEMENT_LINK (exists: reflected) */
INSERT INTO refund_settlement_link (
  refund_settlement_link_id, refund_id, settlement_id, created_at
) VALUES (
  'RSL_1001',
  'RFD_1001',
  '44444444-4444-4444-4444-444444444444',
  NOW()
);

/* 11) AUDIT_LOG */
INSERT INTO audit_log (
  request_id, occurred_at,
  actor_type, actor_id,
  action, entity_type, entity_id,
  status_before, status_after,
  merchant_id, meta_json
) VALUES (
  'cccccccc-cccc-cccc-cccc-cccccccccccc',
  NOW(),
  'ADMIN',
  'ADMIN_1',
  'SETTLEMENT_CREATED',
  'SETTLEMENT',
  '44444444-4444-4444-4444-444444444444',
  NULL,
  'READY',
  'MERCHANT_1001',
  JSON_OBJECT('seed', true, 'case', 'A', 'comment', '정산 생성 + 환불 반영됨(link 존재)')
);

-- ---------------------------------------------------------
-- CASE B (환불 승인됐지만 정산에 아직 미반영: link 없음)
-- ---------------------------------------------------------

/* 0) PAYMENT */
INSERT INTO payment (
  payment_id, order_id, merchant_id, buyer_id,
  currency, requested_amount, captured_amount,
  status, created_at, updated_at
) VALUES (
  'PAY_2001', 'ORD_2001', 'MERCHANT_2001', 'BUYER_2001',
  'KRW', 25000, 25000,
  'CAPTURED', NOW(), NOW()
);

/* 1) PAYMENT_EVENT */
INSERT INTO payment_event (event_id, payment_id, event_type, request_id, occurred_at)
VALUES ('66666666-6666-6666-6666-666666666666','PAY_2001','PAYMENT_CREATED','12121212-1212-1212-1212-121212121212', NOW());

INSERT INTO payment_event (event_id, payment_id, event_type, request_id, occurred_at)
VALUES ('77777777-7777-7777-7777-777777777777','PAY_2001','PAYMENT_CAPTURED','13131313-1313-1313-1313-131313131313', NOW());

/* 2) IDEMPOTENCY_RECORD */
INSERT INTO idempotency_record (
  target_type, target_id, idempotency_key,
  payment_id, response_status, request_id, created_at
) VALUES (
  'PAY_ORDER', 'ORD_2001', 'idem-2001-0001',
  'PAY_2001', 200, '14141414-1414-1414-1414-141414141414', NOW(3)
);

/* 3) SETTLEMENT_BATCH (다른 기준일로 하나 더) */
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
  NOW(), NOW()
);
SET @batch_id_b = LAST_INSERT_ID();

/* 4) SETTLEMENT */
INSERT INTO settlement (
  settlement_id, settlement_no, batch_id,
  merchant_id, status, base_date,
  gross, fee, vat, net,
  paid_requested_by, paid_requested_at,
  paid_approved_by, paid_approved_at,
  created_at, updated_at
) VALUES (
  '99999999-9999-9999-9999-999999999999',
  'SET-20260211-001',
  @batch_id_b,
  'MERCHANT_2001',
  'READY',
  '2026-02-11',
  25000, 2500, 250, 22250,
  NULL, NULL,
  NULL, NULL,
  NOW(), NOW()
);

/* 5) SETTLEMENT_LINE (PAYMENT만 반영) */
INSERT INTO settlement_line (
  settlement_id, payment_id, line_type, amount, created_at
) VALUES (
  '99999999-9999-9999-9999-999999999999',
  'PAY_2001',
  'PAYMENT',
  25000,
  NOW()
);

/* 6) REFUND (APPROVED, BUT NOT LINKED) */
INSERT INTO refund (
  refund_id, payment_id,
  merchant_id, buyer_id,
  amount, currency,
  status, reason_text,
  requested_at, decided_at,
  created_at, updated_at
) VALUES (
  'RFD_2001',
  'PAY_2001',
  'MERCHANT_2001',
  'BUYER_2001',
  5000, 'KRW',
  'APPROVED',
  '부분 환불 승인(B) - 다음 정산에 반영 예정',
  NOW(), NOW(),
  NOW(), NOW()
);

/* 7) REFUND_EVENT */
INSERT INTO refund_event (
  refund_event_id, refund_id,
  event_type, status_before, status_after,
  request_id, actor_type, actor_id, occurred_at
) VALUES (
  'RFE_2001',
  'RFD_2001',
  'REFUND_APPROVED',
  'REQUESTED',
  'APPROVED',
  '16161616-1616-1616-1616-161616161616',
  'ADMIN',
  'ADMIN_2',
  NOW()
);

/* 8) AUDIT_LOG (refund approved, not reflected) */
INSERT INTO audit_log (
  request_id, occurred_at,
  actor_type, actor_id,
  action, entity_type, entity_id,
  status_before, status_after,
  merchant_id, meta_json
) VALUES (
  '17171717-1717-1717-1717-171717171717',
  NOW(),
  'ADMIN',
  'ADMIN_2',
  'REFUND_APPROVED',
  'REFUND',
  'RFD_2001',
  'REQUESTED',
  'APPROVED',
  'MERCHANT_2001',
  JSON_OBJECT('seed', true, 'case', 'B', 'comment', '환불 승인됐지만 refund_settlement_link 없음(미반영)')
);
