package com.settleops.domain.settlement.infra;

import com.settleops.domain.settlement.dto.AdminSettlementListItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementDetailResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementLineItemResponse;
import com.settleops.domain.settlement.dto.MerchantSettlementListItemResponse;
import com.settleops.domain.settlement.entity.Settlement;
import com.settleops.domain.settlement.entity.SettlementBatch;
import com.settleops.domain.settlement.entity.SettlementLine;
import com.settleops.domain.settlement.enums.SettlementLineType;
import com.settleops.domain.settlement.enums.SettlementStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test-db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class SettlementRepositoryImplTest {

    @Autowired
    SettlementRepository settlementRepository;

    @Autowired
    SettlementBatchRepository settlementBatchRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 status와 merchantId 조건을 함께 적용한다")
    void searchAdminSettlements_filtersByStatusAndMerchantId() {
        // given
        LocalDate date1 = LocalDate.of(2099, 12, 31);
        LocalDate date2 = LocalDate.of(2099, 12, 30);
        LocalDate date3 = LocalDate.of(2099, 12, 29);

        Long batchId1 = createBatchId(date1);
        Long batchId2 = createBatchId(date2);
        Long batchId3 = createBatchId(date3);

        Settlement target1 = createSettlement(batchId1, "merchant-1", date1, 10000L);
        Settlement target2 = createSettlement(batchId2, "merchant-1", date2, 9000L);
        Settlement otherMerchant = createSettlement(batchId1, "merchant-2", date1, 11000L);
        Settlement otherStatus = createSettlement(batchId3, "merchant-1", date3, 12000L);

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
    @DisplayName("관리자 정산 리스트 조회는 baseDate desc를 우선 적용한다")
    void searchAdminSettlements_ordersByBaseDateDesc() {
        // given
        LocalDate newestDate = LocalDate.of(2099, 12, 28);
        LocalDate middleDate = LocalDate.of(2099, 12, 27);
        LocalDate oldestDate = LocalDate.of(2099, 12, 26);

        Settlement newest = createSettlement(
                createBatchId(newestDate),
                "merchant-1",
                newestDate,
                12000L
        );
        Settlement middle = createSettlement(
                createBatchId(middleDate),
                "merchant-1",
                middleDate,
                11000L
        );
        Settlement oldest = createSettlement(
                createBatchId(oldestDate),
                "merchant-1",
                oldestDate,
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
        assertThat(content.get(0).baseDate()).isEqualTo(newestDate);
        assertThat(content.get(1).baseDate()).isEqualTo(middleDate);
        assertThat(content.get(2).baseDate()).isEqualTo(oldestDate);
    }

    @Test
    @DisplayName("관리자 정산 리스트 조회 시 같은 baseDate에서는 createdAt desc 순으로 정렬된다")
    void searchAdminSettlements_ordersByCreatedAtDescWithinSameBaseDate() {
        // given
        LocalDate sameBaseDate = LocalDate.of(2099, 12, 25);
        LocalDate olderBaseDate = LocalDate.of(2099, 12, 24);

        Long sameBaseDateBatchId = createBatchId(sameBaseDate);
        Long olderBaseDateBatchId = createBatchId(olderBaseDate);

        Settlement olderCreatedAt = createSettlement(
                sameBaseDateBatchId,
                "merchant-a",
                sameBaseDate,
                10000L
        );
        Settlement newerCreatedAt = createSettlement(
                sameBaseDateBatchId,
                "merchant-b",
                sameBaseDate,
                11000L
        );
        Settlement olderBaseDateSettlement = createSettlement(
                olderBaseDateBatchId,
                "merchant-c",
                olderBaseDate,
                9000L
        );

        settlementRepository.saveAll(List.of(olderCreatedAt, newerCreatedAt, olderBaseDateSettlement));
        entityManager.flush();

        updateCreatedAt(olderCreatedAt.getSettlementId(), LocalDateTime.of(2099, 12, 25, 9, 0));
        updateCreatedAt(newerCreatedAt.getSettlementId(), LocalDateTime.of(2099, 12, 25, 10, 0));
        updateCreatedAt(olderBaseDateSettlement.getSettlementId(), LocalDateTime.of(2099, 12, 24, 11, 0));

        entityManager.clear();

        // when
        Page<AdminSettlementListItemResponse> result = settlementRepository.searchAdminSettlements(
                null,
                null,
                PageRequest.of(0, 20)
        );

        // then
        List<AdminSettlementListItemResponse> content = result.getContent();

        assertThat(content).hasSizeGreaterThanOrEqualTo(3);

        assertThat(content.get(0).baseDate()).isEqualTo(sameBaseDate);
        assertThat(content.get(1).baseDate()).isEqualTo(sameBaseDate);
        assertThat(content.get(2).baseDate()).isEqualTo(olderBaseDate);

        assertThat(content.get(0).createdAt()).isEqualTo(LocalDateTime.of(2099, 12, 25, 10, 0));
        assertThat(content.get(1).createdAt()).isEqualTo(LocalDateTime.of(2099, 12, 25, 9, 0));
    }

    @Test
    @DisplayName("Merchant 정산 리스트 조회 시 해당 merchant의 데이터만 반환한다")
    void searchMerchantSettlements_filtersByMerchantId() {
        // given
        LocalDate date1 = LocalDate.of(2099, 12, 23);
        LocalDate date2 = LocalDate.of(2099, 12, 22);
        LocalDate date3 = LocalDate.of(2099, 12, 21);

        Settlement target1 = createSettlement(
                createBatchId(date1),
                "merchant-1",
                date1,
                10000L
        );
        Settlement target2 = createSettlement(
                createBatchId(date2),
                "merchant-1",
                date2,
                9000L
        );
        Settlement otherMerchant = createSettlement(
                createBatchId(date3),
                "merchant-2",
                date3,
                11000L
        );

        settlementRepository.saveAll(List.of(target1, target2, otherMerchant));
        entityManager.flush();
        entityManager.clear();

        // when
        Page<MerchantSettlementListItemResponse> result = settlementRepository.searchMerchantSettlements(
                "merchant-1",
                PageRequest.of(0, 20)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(MerchantSettlementListItemResponse::settlementId)
                .containsExactly(target1.getSettlementId(), target2.getSettlementId());

        assertThat(result.getContent())
                .extracting(MerchantSettlementListItemResponse::baseDate)
                .containsExactly(date1, date2);
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 merchantId와 settlementId가 일치하면 상세와 line 목록을 반환한다")
    void findMerchantSettlementDetail_returnsDetailWhenMerchantMatches() {
        // given
        LocalDate baseDate = LocalDate.of(2099, 12, 20);

        Settlement settlement = createSettlement(
                createBatchId(baseDate),
                "merchant-1",
                baseDate,
                10000L
        );
        settlementRepository.save(settlement);
        entityManager.flush();

        SettlementLine laterLine = SettlementLine.of(
                settlement.getSettlementId(),
                "payment-2",
                SettlementLineType.PAYMENT,
                3000L
        );
        SettlementLine earlierLine = SettlementLine.of(
                settlement.getSettlementId(),
                "payment-1",
                SettlementLineType.PAYMENT,
                7000L
        );

        entityManager.persist(laterLine);
        entityManager.persist(earlierLine);
        entityManager.flush();

        updateSettlementLineCreatedAt(earlierLine.getSettlementLineId(), LocalDateTime.of(2099, 12, 20, 9, 0));
        updateSettlementLineCreatedAt(laterLine.getSettlementLineId(), LocalDateTime.of(2099, 12, 20, 10, 0));
        entityManager.clear();

        // when
        MerchantSettlementDetailResponse result = settlementRepository.findMerchantSettlementDetail(
                "merchant-1",
                settlement.getSettlementId()
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.settlementId()).isEqualTo(settlement.getSettlementId());
        assertThat(result.baseDate()).isEqualTo(baseDate);
        assertThat(result.status()).isEqualTo(SettlementStatus.READY);
        assertThat(result.net()).isEqualTo(10000L);

        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines())
                .extracting(MerchantSettlementLineItemResponse::paymentId)
                .containsExactly("payment-1", "payment-2");
    }

    @Test
    @DisplayName("Merchant 정산 상세 조회 시 다른 merchant의 settlement이면 null을 반환한다")
    void findMerchantSettlementDetail_returnsNullWhenMerchantDoesNotMatch() {
        // given
        LocalDate baseDate = LocalDate.of(2099, 12, 19);

        Settlement settlement = createSettlement(
                createBatchId(baseDate),
                "merchant-1",
                baseDate,
                10000L
        );
        settlementRepository.save(settlement);
        entityManager.flush();
        entityManager.clear();

        // when
        MerchantSettlementDetailResponse result = settlementRepository.findMerchantSettlementDetail(
                "merchant-2",
                settlement.getSettlementId()
        );

        // then
        assertThat(result).isNull();
    }

    private Settlement createSettlement(
            Long batchId,
            String merchantId,
            LocalDate baseDate,
            long net
    ) {
        return Settlement.createReady(
                UUID.randomUUID().toString(),
                "SET-" + baseDate.toString().replace("-", "") + "-" + merchantId + "-" + UUID.randomUUID().toString().substring(0, 8),
                batchId,
                merchantId,
                baseDate,
                net,
                0L,
                0L,
                net
        );
    }

    private Long createBatchId(LocalDate baseDate) {
        SettlementBatch batch = SettlementBatch.started(
                baseDate,
                UUID.randomUUID().toString(),
                "ADMIN:test",
                UUID.randomUUID().toString()
        );
        SettlementBatch saved = settlementBatchRepository.save(batch);
        entityManager.flush();
        return saved.getBatchId();
    }

    private void forceStatus(String settlementId, SettlementStatus status) {
        jdbcTemplate.update(
                "UPDATE settlement SET status = ? WHERE settlement_id = ?",
                status.name(),
                settlementId
        );
    }

    private void updateCreatedAt(String settlementId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE settlement SET created_at = ? WHERE settlement_id = ?",
                Timestamp.valueOf(createdAt),
                settlementId
        );
    }

    private void updateSettlementLineCreatedAt(Long settlementLineId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE settlement_line SET created_at = ? WHERE settlement_line_id = ?",
                Timestamp.valueOf(createdAt),
                settlementLineId
        );
    }
}