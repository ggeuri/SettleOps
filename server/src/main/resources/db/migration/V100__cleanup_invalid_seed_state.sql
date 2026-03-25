/* =========================================================
   SettleOps - Flyway V5
   File: V100__cleanup_invalid_seed_state.sql

   [목적]
   - 이미 적용된 legacy seed 중, audit 흔적이 없는 invalid hold seed만 우선 정리한다.
   - settlement.status=READY 이면서 hold.status=HOLD_ACTIVE 인 legacy seed 중
     V2 Case A hold를 제거한다.
   - V4 Case A hold는 audit_log(HOLD_APPROVED no-op) 흔적이 확인되어
     본 migration에서 제거하지 않는다.

   [배경]
   - flyway_schema_history 확인 결과:
     * V2__seed_2cases.sql 적용됨
     * V4__introduce_orders_and_update_seed.sql 적용됨
   - 실제 DB 확인 결과:
     * 44444444-4444-4444-4444-444444444444 -> READY / HOLD_ACTIVE
     * 44444444-4444-4444-4444-444444444440 -> READY / HOLD_ACTIVE
   - 추가 확인 결과:
     * hold_id=55555555-5555-5555-5555-555555555555
       -> audit_log 기준 HOLD 관련 흔적 없음
     * hold_id=55555555-5555-5555-5555-555555555550
       -> HOLD_APPROVED no-op audit_log 다건 존재
   - 따라서 A1 Trace/Audit 재현성을 해치지 않기 위해
     이번 migration은 V2 invalid hold만 정리한다.

   [정리 원칙]
   1) 기존 migration(V2/V4)은 수정/삭제하지 않는다.
   2) 후속 migration으로 legacy invalid seed만 정리한다.
   3) A1 Trace/Audit 재현성을 해칠 수 있는 row는 본 migration에서 제거하지 않는다.
   4) HOLD_ACTIVE/HOLD_ACTIVE, HOLD_REQUESTED/PAY_REQUESTED 등
      실제 운영 흐름 row는 건드리지 않는다.

   [주의]
   - reflected 판정 SoT는 refund_settlement_link 기준으로 유지된다.
   - reflected 케이스의 settlement_line(REFUND) 부재는
     본 migration 범위에서 다루지 않으며, 별도 seed 보강 과제로 남긴다.
   ========================================================= */

-- ---------------------------------------------------------
-- 0) 정리 대상 고정
--    - V2 reflected 케이스의 invalid hold만 정리
-- ---------------------------------------------------------
SET @invalid_stl_v2  = '44444444-4444-4444-4444-444444444444';
SET @invalid_hold_v2 = '55555555-5555-5555-5555-555555555555';

-- ---------------------------------------------------------
-- 1) 연결된 hold_event 정리
--    - legacy seed hold 제거에 맞춰 seed성 hold_event도 함께 제거
-- ---------------------------------------------------------
DELETE FROM hold_event
WHERE hold_id = @invalid_hold_v2;

-- ---------------------------------------------------------
-- 2) invalid hold 정리
--    - settlement.status는 READY로 유지
--    - hold만 제거하여 reflected 케이스와 hold 케이스를 분리
-- ---------------------------------------------------------
DELETE FROM hold
WHERE hold_id = @invalid_hold_v2
  AND settlement_id = @invalid_stl_v2;

-- ---------------------------------------------------------
-- 3) 수동 검증용 참고 쿼리
--    - 아래 쿼리는 주석 상태로만 보관
-- ---------------------------------------------------------

-- [검증 1] V2 invalid hold 제거 확인
-- SELECT hold_id, settlement_id, status
-- FROM hold
-- WHERE settlement_id = '44444444-4444-4444-4444-444444444444';

-- 기대 결과:
-- 0 rows

-- [검증 2] settlement는 유지되는지 확인
-- SELECT settlement_id, status
-- FROM settlement
-- WHERE settlement_id = '44444444-4444-4444-4444-444444444444';

-- 기대 결과:
-- settlement_id = 44444444-4444-4444-4444-444444444444
-- status = READY

-- [검증 3] reflected 증거는 유지되는지 확인
-- SELECT refund_id, settlement_id, created_at
-- FROM refund_settlement_link
-- WHERE settlement_id = '44444444-4444-4444-4444-444444444444';

-- 기대 결과:
-- refund_settlement_link row 1건 유지

-- [검증 4] A1 Trace/Audit 보존 원칙 확인
-- SELECT audit_id, action, entity_type, entity_id, occurred_at
-- FROM audit_log
-- WHERE entity_id IN (
--   '44444444-4444-4444-4444-444444444444',
--   '55555555-5555-5555-5555-555555555555'
-- )
-- ORDER BY occurred_at, audit_id;

-- 기대 결과:
-- settlement audit는 남아 있을 수 있음
-- hold audit는 없어야 본 migration 의도와 일치