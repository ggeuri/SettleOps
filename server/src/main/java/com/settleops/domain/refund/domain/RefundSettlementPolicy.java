package com.settleops.domain.refund.domain;

public class RefundSettlementPolicy {
    // DB 조회를 직접 하면 안됨 domain이 infra 의존하게 됨
    // 규칙만.. 입력받아서 판정

    //isRefundAdjustmentPending(settlementId)
    //→ “APPROVED refund 존재 && refund_settlement_link 없으면 true”
    //→ 조인 추론 금지, link 테이블만 SoT로 사용
}
