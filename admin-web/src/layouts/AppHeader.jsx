// src/components/layout/AppHeader.jsx
import { useLocation } from "react-router-dom";

const titleMap = {
  "/consumer/orders/new": "거래 생성",
  "/consumer/payments": "내 주문/결제 내역",
  "/merchant/payments": "결제 조회",
  "/merchant/refunds": "환불 현황",
  "/admin/audit": "Trace / Audit",
  "/admin/batches": "배치 실행",
  "/admin/settlements": "정산 관리",
  "/admin/holds": "Hold 큐",
  "/admin/refunds": "환불 큐",
};

export default function AppHeader() {
  const location = useLocation();

  const pageTitle = titleMap[location.pathname] ?? "SettleOps Admin";

  return (
    <div className="app-header">
      <div className="app-header__left">
        <h1 className="app-header__title">{pageTitle}</h1>
        <p className="app-header__meta">{location.pathname}</p>
      </div>

      <div className="app-header__right">
        <span className="app-header__badge">DEV</span>
      </div>
    </div>
  );
}