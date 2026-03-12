package com.settleops.domain.settlement.application;

import com.settleops.domain.settlement.dto.AdminSettlementDetailResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import com.settleops.domain.settlement.infra.SettlementLineRepository;
import com.settleops.domain.settlement.infra.SettlementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SettlementDetailQueryServiceImplTest {

    @Autowired SettlementDetailQueryService settlementDetailQueryService;

    @Autowired SettlementRepository settlementRepository;

    @Autowired SettlementLineRepository settlementLineRepository;

    @Test
    @DisplayName("관리자 정산 상세 조회 시 존재하지 않는 settlementId면 404를 반환한다")
    void getAdminSettlementDetail_notFound_then404() {
        assertThatThrownBy(() -> settlementDetailQueryService.getAdminSettlementDetail("not-exists-id"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    @DisplayName("관리자 정산 상세 조회 시 settlement 요약과 createdAt asc 기준 line 목록을 반환한다")
    void getAdminSettlementDetail_success() {
        // given
        String settlementId = UUID.randomUUID().toString();

        Settlement settlement = Settlement.createReady(
                settlementId,
                "SET-20260310-merchant-1",
                1L,
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L,
                0L,
                0L,
                10000L
        );
        settlementRepository.save(settlement);

        SettlementLine line1 = SettlementLine.of(
                settlementId,
                "payment-1",
                SettlementLineType.PAYMENT,
                7000L
        );
        SettlementLine line2 = SettlementLine.of(
                settlementId,
                "payment-2",
                SettlementLineType.PAYMENT,
                3000L
        );
        settlementLineRepository.saveAll(List.of(line1, line2));

        //when
        AdminSettlementDetailResponse response =
                settlementDetailQueryService.getAdminSettlementDetail(settlementId);
        // then
        assertThat(response).isNotNull();
        assertThat(response.settlementId()).isEqualTo(settlementId);
        assertThat(response.merchantId()).isEqualTo("merchant-1");
        assertThat(response.baseDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(response.status()).isEqualTo(SettlementStatus.READY);
        assertThat(response.gross()).isEqualTo(10000L);
        assertThat(response.fee()).isEqualTo(0L);
        assertThat(response.vat()).isEqualTo(0L);
        assertThat(response.net()).isEqualTo(10000L);

        assertThat(response.lines()).hasSize(2);
        assertThat(response.lines())
                .extracting(line -> line.paymentId())
                .containsExactly("payment-1", "payment-2");

        assertThat(response.lines())
                .extracting(line -> line.type())
                .containsExactly(SettlementLineType.PAYMENT, SettlementLineType.PAYMENT);

        assertThat(response.lines())
                .extracting(line -> line.amount())
                .containsExactly(7000L, 3000L);

        assertThat(response.hold()).isNotNull();
        assertThat(response.hold().exists()).isFalse();

        assertThat(response.refund()).isNotNull();
        assertThat(response.refund().hasApprovedRefund()).isFalse();
        assertThat(response.refund().refundAdjustmentPending()).isFalse();
    }
}
