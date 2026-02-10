/* =========================================================
   SettleOps - Flyway V99 FK ON (Integration Only)
   - 팀 DB 합치기 “직전”에만 실행
   - 로컬 개발에서는 절대 실행 금지: 아래 블록 주석 유지
   - ON DELETE는 운영/증빙 관점에서 RESTRICT 권장
   ========================================================= */

 /*
-- =========================================================
-- [FK-ON@INTEGRATION]  통합 직전용
-- =========================================================

-- 1) payment_event -> payment  (증빙 이벤트 보호: RESTRICT)
ALTER TABLE payment_event
  ADD CONSTRAINT fk_payment_event_payment
    FOREIGN KEY (payment_id) REFERENCES payment(payment_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 2) settlement -> settlement_batch
ALTER TABLE settlement
  ADD CONSTRAINT fk_settlement_batch
    FOREIGN KEY (batch_id) REFERENCES settlement_batch(batch_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 3) settlement_line -> settlement / payment
ALTER TABLE settlement_line
  ADD CONSTRAINT fk_settlement_line_settlement
    FOREIGN KEY (settlement_id) REFERENCES settlement(settlement_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

ALTER TABLE settlement_line
  ADD CONSTRAINT fk_settlement_line_payment
    FOREIGN KEY (payment_id) REFERENCES payment(payment_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 4) hold -> settlement
ALTER TABLE hold
  ADD CONSTRAINT fk_hold_settlement
    FOREIGN KEY (settlement_id) REFERENCES settlement(settlement_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 5) hold_event -> hold
ALTER TABLE hold_event
  ADD CONSTRAINT fk_hold_event_hold
    FOREIGN KEY (hold_id) REFERENCES hold(hold_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 6) refund -> payment
ALTER TABLE refund
  ADD CONSTRAINT fk_refund_payment
    FOREIGN KEY (payment_id) REFERENCES payment(payment_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 7) refund_event -> refund
ALTER TABLE refund_event
  ADD CONSTRAINT fk_rfe_refund
    FOREIGN KEY (refund_id) REFERENCES refund(refund_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- 8) refund_settlement_link -> refund / settlement
ALTER TABLE refund_settlement_link
  ADD CONSTRAINT fk_rsl_refund
    FOREIGN KEY (refund_id) REFERENCES refund(refund_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

ALTER TABLE refund_settlement_link
  ADD CONSTRAINT fk_rsl_settlement
    FOREIGN KEY (settlement_id) REFERENCES settlement(settlement_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;

-- =========================================================
-- [OPTION] 통합 전 “강화” 섹션 (원하면 주석 해제)
-- =========================================================

-- payment.status / amount rule 강화(원하면)
-- ALTER TABLE payment
--   ADD CONSTRAINT chk_payment_status
--     CHECK (status IN ('CREATED','CAPTURED')),
--   ADD CONSTRAINT chk_payment_amount_nonneg
--     CHECK (requested_amount > 0 AND captured_amount >= 0),
--   ADD CONSTRAINT chk_payment_amount_status
--     CHECK (
--       (status = 'CREATED' AND captured_amount = 0)
--       OR
--       (status = 'CAPTURED' AND captured_amount = requested_amount AND captured_amount > 0)
--     );

-- payment_event.event_type 고정(원하면)
-- ALTER TABLE payment_event
--   ADD CONSTRAINT chk_payment_event_type
--     CHECK (event_type IN ('PAYMENT_CREATED','PAYMENT_CAPTURED','PAYMENT_CONFIRMED'));

-- settlement.status 고정(원하면)
-- ALTER TABLE settlement
--   ADD CONSTRAINT chk_settlement_status
--     CHECK (status IN ('READY','HOLD_ACTIVE','PAY_REQUESTED','PAID'));

-- hold.status / hold_event type 고정(원하면)
-- ALTER TABLE hold
--   ADD CONSTRAINT chk_hold_status
--     CHECK (status IN ('HOLD_REQUESTED','HOLD_ACTIVE','RELEASED'));

-- ALTER TABLE hold_event
--   ADD CONSTRAINT chk_hold_event_type
--     CHECK (event_type IN ('HOLD_REQUESTED','HOLD_APPROVED','HOLD_RELEASED')),
--   ADD CONSTRAINT chk_hold_event_actor_type
--     CHECK (actor_type IN ('ADMIN','SYSTEM'));

-- refund.status / refund_event type 고정(원하면)
-- ALTER TABLE refund
--   ADD CONSTRAINT chk_refund_status
--     CHECK (status IN ('REQUESTED','APPROVED','REJECTED'));

-- ALTER TABLE refund_event
--   ADD CONSTRAINT chk_rfe_event_type
--     CHECK (event_type IN ('REFUND_REQUESTED','REFUND_APPROVED','REFUND_REJECTED')),
--   ADD CONSTRAINT chk_rfe_actor_type
--     CHECK (actor_type IN ('MERCHANT','ADMIN'));

-- =========================================================
-- [OPTION] JSON NOT NULL 강화 (너가 말한 “나중에 켤 것” 템플릿)
-- - 서비스가 meta_json 기본 {}를 항상 넣는 규칙을 갖춘 뒤에만 권장
-- =========================================================

-- ALTER TABLE hold_event
--   MODIFY meta_json JSON NOT NULL;

-- ALTER TABLE audit_log
--   MODIFY meta_json JSON NOT NULL;

 */
