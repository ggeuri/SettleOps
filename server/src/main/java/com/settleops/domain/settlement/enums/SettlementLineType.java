package com.settleops.domain.settlement.enums;

public enum SettlementLineType {
    PAYMENT(1),   // 매출 정산 (+)
    REFUND(-1);   // 차감 정산 (-)

    private final int sign;

    SettlementLineType(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }
    //정합성 규칙: net = Σ(amount * sign). amount는 항상 양수로 저장
    public long applySign(long amount) {
        return amount * sign;
    }
}