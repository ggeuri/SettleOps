// /admin-web/src/app/router/MerchantRoutes.jsx

import { Routes, Route, Navigate } from "react-router-dom";

import PaymentListPage from "../../pages/merchant/PaymentListPage.jsx";
import PaymentDetailPage from "../../pages/merchant/PaymentDetailPage.jsx";
import SettlementListPage from "../../pages/merchant/SettlementListPage.jsx";
import SettlementDetailPage from "../../pages/merchant/SettlementDetailPage.jsx";
import RefundPage from "../../pages/merchant/RefundPage.jsx";

export default function MerchantRoutes() {
  return (
    <Routes>
      <Route index element={<Navigate to="/merchant/payments" replace />} />

      <Route path="payments" element={<PaymentListPage />} />
      <Route path="payments/:paymentId" element={<PaymentDetailPage />} />

      <Route path="settlements" element={<SettlementListPage />} />
      <Route
        path="settlements/:settlementId"
        element={<SettlementDetailPage />}
      />

      <Route path="refunds" element={<RefundPage />} />
    </Routes>
  );
}