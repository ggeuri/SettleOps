// src/layouts/AppHeader.jsx
import { useLocation } from "react-router-dom";

function resolvePageTitle(pathname) {
  if (pathname === "/consumer/orders/new") return "거래 생성";
  if (pathname === "/consumer/orders") return "내 주문/결제 내역";
  if (pathname.startsWith("/consumer/orders/") && pathname !== "/consumer/orders/new") {
    return "결제 상세";
  }

  if (pathname === "/merchant/payments") return "결제 조회";
  if (pathname.startsWith("/merchant/payments/")) return "결제 상세";
  if (pathname === "/merchant/refunds") return "환불 현황";

  if (pathname === "/admin/audit") return "Trace / Audit";
  if (pathname === "/admin/batches") return "배치 실행";
  if (pathname === "/admin/settlements") return "정산 관리";
  if (pathname.startsWith("/admin/settlements/")) return "정산 상세";
  if (pathname === "/admin/holds") return "Hold 큐";
  if (pathname.startsWith("/admin/holds/")) return "Hold 상세";
  if (pathname === "/admin/refunds") return "환불 큐";
  if (pathname.startsWith("/admin/refunds/")) return "환불 상세";

  if (pathname === "/sample/ui") return "공통 UI 샘플";

  return "SettleOps Admin";
}

export default function AppHeader() {
  const location = useLocation();
  const pageTitle = resolvePageTitle(location.pathname);

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