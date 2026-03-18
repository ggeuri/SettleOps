import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import SettlementListSummarySection from "../../components/settlement/SettlementListSummarySection.jsx";
import SettlementListFilterSection from "../../components/settlement/SettlementListFilterSection.jsx";
import SettlementListTable from "../../components/settlement/SettlementListTable.jsx";

const MOCK_SETTLEMENTS = [
  {
    settlementId: "SET-20260318-0001",
    merchantId: "MRC_1001",
    status: "READY",
    gross: 125000,
    fee: 3750,
    vat: 375,
    net: 120875,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 10:30:00",
  },
  {
    settlementId: "SET-20260318-0002",
    merchantId: "MRC_1001",
    status: "HOLD_ACTIVE",
    gross: 98000,
    fee: 2940,
    vat: 294,
    net: 94766,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 11:05:00",
  },
  {
    settlementId: "SET-20260318-0003",
    merchantId: "MRC_1001",
    status: "PAY_REQUESTED",
    gross: 301000,
    fee: 9030,
    vat: 903,
    net: 291067,
    baseDate: "2026-03-18",
    createdAt: "2026-03-18 12:20:00",
  },
  {
    settlementId: "SET-20260317-0004",
    merchantId: "MRC_1001",
    status: "PAID",
    gross: 77000,
    fee: 2310,
    vat: 231,
    net: 74459,
    baseDate: "2026-03-17",
    createdAt: "2026-03-17 16:10:00",
  },
];

export default function SettlementListPage() {
  const navigate = useNavigate();

  const [filters, setFilters] = useState({
    merchantId: "MRC_1001",
    status: "전체",
    keyword: "",
  });

  const filteredSettlements = useMemo(() => {
    return MOCK_SETTLEMENTS.filter((item) => {
      const matchMerchant =
        !filters.merchantId || item.merchantId.includes(filters.merchantId);

      const matchStatus =
        filters.status === "전체" ? true : item.status === filters.status;

      const keyword = filters.keyword.trim().toLowerCase();
      const matchKeyword =
        !keyword ||
        item.settlementId.toLowerCase().includes(keyword) ||
        item.merchantId.toLowerCase().includes(keyword);

      return matchMerchant && matchStatus && matchKeyword;
    });
  }, [filters]);

  const summary = useMemo(() => {
    return {
      totalCount: filteredSettlements.length,
      readyCount: filteredSettlements.filter((item) => item.status === "READY").length,
      holdCount: filteredSettlements.filter((item) => item.status === "HOLD_ACTIVE").length,
      paidCount: filteredSettlements.filter((item) => item.status === "PAID").length,
    };
  }, [filteredSettlements]);

  function handleChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  function handleReset() {
    setFilters({
      merchantId: "MRC_1001",
      status: "전체",
      keyword: "",
    });
  }

  function handleRowClick(settlementId) {
    navigate(`/merchant/settlements/${settlementId}`);
  }

  return (
    <PageLayout
      title="정산 리스트"
      description="판매자 기준 정산 내역을 조회합니다. row 클릭 시 정산 상세(U5)로 이동합니다."
    >
      <SettlementListSummarySection
        totalCount={summary.totalCount}
        readyCount={summary.readyCount}
        holdCount={summary.holdCount}
        extraLabel="PAID"
        extraCount={summary.paidCount}
      />

      <SettlementListFilterSection
        merchantId={filters.merchantId}
        status={filters.status}
        keyword={filters.keyword}
        onChange={handleChange}
        onReset={handleReset}
        merchantIdPlaceholder="MRC_1001"
      />

      <SettlementListTable
        items={filteredSettlements}
        onRowClick={handleRowClick}
        toolbarTitle="정산 목록"
      />
    </PageLayout>
  );
}