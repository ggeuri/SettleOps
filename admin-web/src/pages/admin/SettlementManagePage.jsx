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
    merchantId: "MRC_1002",
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
    merchantId: "MRC_1003",
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
  {
    settlementId: "SET-20260317-0005",
    merchantId: "MRC_1002",
    status: "READY",
    gross: 54000,
    fee: 1620,
    vat: 162,
    net: 52218,
    baseDate: "2026-03-17",
    createdAt: "2026-03-17 18:40:00",
  },
];

export default function SettlementManagePage() {
  const navigate = useNavigate();

  const [filters, setFilters] = useState({
    merchantId: "",
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
      payRequestedCount: filteredSettlements.filter((item) => item.status === "PAY_REQUESTED").length,
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
      merchantId: "",
      status: "전체",
      keyword: "",
    });
  }

  function handleRowClick(settlementId) {
    navigate(`/admin/settlements/${settlementId}`);
  }

  return (
    <PageLayout
      title="정산 관리"
      description="운영자가 merchantId / status 기준으로 정산을 조회하고 상세(A4)로 이동합니다."
    >
      <SettlementListSummarySection
        totalCount={summary.totalCount}
        readyCount={summary.readyCount}
        holdCount={summary.holdCount}
        extraLabel="PAY_REQUESTED"
        extraCount={summary.payRequestedCount}
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