// src/layouts/AppHeader.jsx
import { useEffect, useState } from "react";
import { useLocation, Link } from "react-router-dom";
import { getMe } from "../../api/meApi.js";

function resolvePageTitle(pathname) {
  if (pathname === "/consumer/orders/new") return "거래 생성";
  if (pathname === "/consumer/orders") return "내 주문/결제 내역";
  if (pathname.startsWith("/consumer/orders/") && pathname !== "/consumer/orders/new") {
    return "결제 상세";
  }

  if (pathname === "/merchant/payments") return "결제 조회";
  if (pathname.startsWith("/merchant/payments/")) return "결제 상세";
  if (pathname === "/merchant/settlements") return "정산 리스트";
  if (pathname.startsWith("/merchant/settlements/")) return "정산 상세";
  if (pathname === "/merchant/refunds") return "환불 요청 · 현황";

  if (pathname === "/admin/audit") return "Trace / Audit";
  if (pathname === "/admin/settlement-batches") return "배치 실행";
  if (pathname === "/admin/settlements") return "정산 관리";
  if (pathname.startsWith("/admin/settlements/")) return "정산 상세";
  if (pathname === "/admin/holds") return "Hold 큐";
  if (pathname.startsWith("/admin/holds/")) return "Hold 상세";
  if (pathname === "/admin/refunds") return "환불 큐";
  if (pathname.startsWith("/admin/refunds/")) return "환불 상세";

  if (pathname === "/sample/ui") return "공통 UI 샘플";

  return "SettleOps Admin";
}

function resolveRoleLabel(me) {
  if (!me) {
    return "GUEST";
  }

  if (me.role) {
    return me.role;
  }

  if (me.authorities?.includes("ROLE_ADMIN")) {
    return "ADMIN";
  }
  if (me.authorities?.includes("ROLE_MERCHANT")) {
    return "MERCHANT";
  }
  if (me.authorities?.includes("ROLE_CONSUMER")) {
    return "CONSUMER";
  }

  return "USER";
}

function resolvePrincipalId(me) {
  if (!me) {
    return "-";
  }

  return (
    me.adminId ||
    me.merchantId ||
    me.buyerId ||
    me.principalId ||
    me.loginId ||
    me.id ||
    "-"
  );
}

export default function AppHeader() {
  const location = useLocation();
  const pageTitle = resolvePageTitle(location.pathname);

  const [me, setMe] = useState(null);

  useEffect(() => {
    async function loadMe() {
      try {
        const data = await getMe();
        setMe(data);
      } catch {
        setMe(null);
      }
    }

    loadMe();
  }, [location.pathname]);

  const roleLabel = resolveRoleLabel(me);
  const principalId = resolvePrincipalId(me);
  const sessionLabel =
    me && principalId !== "-"
      ? `DEV | ${roleLabel}(${principalId})`
      : "DEV";

  return (
    <div className="app-header">
      <div className="app-header__left">
        <h1 className="app-header__title">{pageTitle}</h1>
        <p className="app-header__meta">{location.pathname}</p>
      </div>

      <div className="app-header__right">
        <Link
          to="/auth/dev-login"
          state={{ from: location.pathname + location.search }}
          className="app-header__badge-link"
        >
          <span className="app-header__badge">{sessionLabel}</span>
        </Link>
      </div>
    </div>
  );
}