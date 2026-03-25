/* =========================================================
   SettleOps - Flyway V100
   File: V100__cleanup_invalid_seed_state.sql

   [목적]
   - legacy reflected seed 중 settlement.status=READY 이면서
     hold.status=HOLD_ACTIVE 인 상태 불일치를 보정한다.
   - hold_event / audit_log 의 insert-only 재현 원칙을 해치지 않기 위해
     hold / hold_event 삭제 대신 settlement 상태를 HOLD_ACTIVE 로 정렬한다.

   [배경]
   - flyway_schema_history 확인 결과:
     * V2__seed_2cases.sql 적용됨
     * V4__introduce_orders_and_update_seed.sql 적용됨
     * V99__fk_on.sql 적용됨
   - 실제 DB 확인 결과:
     * 44444444-4444-4444-4444-444444444444 -> READY / HOLD_ACTIVE
     * 44444444-4444-4444-4444-444444444440 -> READY / HOLD_ACTIVE
   - 추가 확인 결과:
     * V4 reflected hold에는 HOLD_APPROVED no-op audit_log 다건 존재
   - 따라서 이번 migration은 insert-only 원칙과 A1 Trace/Audit 재현성을 보존하면서
     READY / HOLD_ACTIVE 불일치를 상태 보정 방식으로 해소한다.

   [정리 원칙]
   1) 기존 migration(V2/V4)은 수정/삭제하지 않는다.
   2) 후속 migration으로 legacy invalid seed만 보정한다.
   3) hold_event / audit_log 는 insert-only 원칙을 존중하여 삭제하지 않는다.
   4) READY / HOLD_ACTIVE 상태 불일치만 해소하고,
      HOLD_ACTIVE/HOLD_ACTIVE, HOLD_REQUESTED/PAY_REQUESTED 등
      실제 운영 흐름 row는 건드리지 않는다.

   [주의]
   - reflected 판정 SoT는 refund_settlement_link 기준으로 유지된다.
   - reflected 케이스의 settlement_line(REFUND) 부재는
     본 migration 범위에서 다루지 않으며, 별도 seed 보강 과제로 남긴다.
   ========================================================= */

-- ---------------------------------------------------------
-- 0) 정리 대상 고정
--    - reflected seed 중 READY / HOLD_ACTIVE 불일치 2건
-- ---------------------------------------------------------
SET @invalid_stl_v2  = '44444444-4444-4444-4444-444444444444';
SET @invalid_hold_v2 = '55555555-5555-5555-5555-555555555555';

SET @invalid_stl_v4  = '44444444-4444-4444-4444-444444444440';
SET @invalid_hold_v4 = '55555555-5555-5555-5555-555555555550';

-- ---------------------------------------------------------
-- 1) V2 reflected 케이스 상태 보정
--    - hold는 이미 HOLD_ACTIVE 이므로 settlement 상태를 맞춘다.
-- ---------------------------------------------------------
UPDATE settlement s
    JOIN hold h ON h.settlement_id = s.settlement_id
    SET s.status = 'HOLD_ACTIVE',
        s.updated_at = CURRENT_TIMESTAMP(6)
WHERE s.settlement_id = @invalid_stl_v2
  AND s.status = 'READY'
  AND h.hold_id = @invalid_hold_v2
  AND h.status = 'HOLD_ACTIVE';

-- ---------------------------------------------------------
-- 2) V4 reflected 케이스 상태 보정
--    - audit/trace 흔적이 존재하므로 삭제 대신 settlement 상태만 맞춘다.
-- ---------------------------------------------------------
UPDATE settlement s
    JOIN hold h ON h.settlement_id = s.settlement_id
    SET s.status = 'HOLD_ACTIVE',
        s.updated_at = CURRENT_TIMESTAMP(6)
WHERE s.settlement_id = @invalid_stl_v4
  AND s.status = 'READY'
  AND h.hold_id = @invalid_hold_v4
  AND h.status = 'HOLD_ACTIVE';

-- ---------------------------------------------------------
-- 3) 수동 검증용 참고 쿼리
-- ---------------------------------------------------------

-- [검증 1] 상태 불일치 해소 확인
-- SELECT s.settlement_id, s.status AS settlement_status, h.status AS hold_status
-- FROM settlement s
-- JOIN hold h ON h.settlement_id = s.settlement_id
-- WHERE s.settlement_id IN (
--   '44444444-4444-4444-4444-444444444444',
--   '44444444-4444-4444-4444-444444444440'
-- );

-- 기대 결과:
-- 두 row 모두 settlement_status = HOLD_ACTIVE
-- hold_status = HOLD_ACTIVE

-- [검증 2] reflected 증거 유지 확인
-- SELECT refund_id, settlement_id, created_at
-- FROM refund_settlement_link
-- WHERE settlement_id IN (
--   '44444444-4444-4444-4444-444444444444',
--   '44444444-4444-4444-4444-444444444440'
-- )
-- ORDER BY settlement_id;

-- 기대 결과:
-- reflected link 2건 유지

-- [검증 3] hold_event 보존 확인
-- SELECT hold_event_id, hold_id, event_type
-- FROM hold_event
-- WHERE hold_id IN (
--   '55555555-5555-5555-5555-555555555555',
--   '55555555-5555-5555-5555-555555555550'
-- )
-- ORDER BY hold_event_id;

-- 기대 결과:
-- 기존 hold_event 유지

-- [검증 4] audit 보존 확인
-- SELECT audit_id, action, entity_type, entity_id, occurred_at
-- FROM audit_log
-- WHERE entity_id IN (
--   '44444444-4444-4444-4444-444444444444',
--   '44444444-4444-4444-4444-444444444440',
--   '55555555-5555-5555-5555-555555555555',
--   '55555555-5555-5555-5555-555555555550'
-- )
-- ORDER BY occurred_at, audit_id;

-- 기대 결과:
-- 기존 audit 흔적 유지