package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.enums.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SettlementRepositoryImplTest {

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 status와 merchantId 조건을 함께 적용한다")
    void searchAdminSettlements_filtersByStatusAndMerchantId() {
        // given
        Settlement target1 = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                10000L
        );
        Settlement target2 = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 9),
                9000L
        );
        Settlement otherMerchant = createSettlement(
                "merchant-2",
                LocalDate.of(2026, 3, 10),
                11000L
        );
        Settlement otherStatus = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 8),
                12000L
        );

        settlementRepository.saveAll(List.of(target1, target2, otherMerchant, otherStatus));
        entityManager.flush();

        forceStatus(otherStatus.getSettlementId(), SettlementStatus.PAY_REQUESTED);
        entityManager.clear();

        // when
        Page<AdminSettlementListItemResponse> result = settlementRepository.searchAdminSettlements(
                SettlementStatus.READY,
                "merchant-1",
                PageRequest.of(0, 20)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(AdminSettlementListItemResponse::merchantId)
                .containsOnly("merchant-1");

        assertThat(result.getContent())
                .extracting(AdminSettlementListItemResponse::status)
                .containsOnly(SettlementStatus.READY);
    }

    @Test
    @DisplayName("관리자 정산 리스트 조회는 baseDate desc 순으로 정렬된다")
    void searchAdminSettlements_ordersByBaseDateDesc() {
        // given
        Settlement newest = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 11),
                12000L
        );
        Settlement middle = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 10),
                11000L
        );
        Settlement oldest = createSettlement(
                "merchant-1",
                LocalDate.of(2026, 3, 9),
                9000L
        );

        settlementRepository.saveAll(List.of(middle, newest, oldest));
        entityManager.flush();
        entityManager.clear();

        // when
        Page<AdminSettlementListItemResponse> result = settlementRepository.searchAdminSettlements(
                null,
                "merchant-1",
                PageRequest.of(0, 20)
        );

        // then
        assertThat(result.getContent()).hasSize(3);

        List<AdminSettlementListItemResponse> content = result.getContent();

        // 1순위: baseDate desc
        assertThat(content.get(0).baseDate()).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(content.get(1).baseDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(content.get(2).baseDate()).isEqualTo(LocalDate.of(2026, 3, 9));
    }

    private Settlement createSettlement(
            String merchantId,
            LocalDate baseDate,
            long net
    ) {
        return Settlement.createReady(
                UUID.randomUUID().toString(),
                "SET-" + baseDate.toString().replace("-", "") + "-" + merchantId + "-" + UUID.randomUUID().toString().substring(0, 8),
                1L,
                merchantId,
                baseDate,
                net,
                0L,
                0L,
                net
        );
    }

    private void forceStatus(String settlementId, SettlementStatus status) {
        jdbcTemplate.update(
                "UPDATE settlement SET status = ? WHERE settlement_id = ?",
                status.name(),
                settlementId
        );
    }
}