// /admin-web/src/app/router/MerchantRoutes.jsx

import { Routes, Route, Navigate } from "react-router-dom";
import PaymentListPage from "../../pages/merchant/PaymentListPage.jsx";
import PaymentDetailPage from "../../pages/merchant/PaymentDetailPage.jsx";
import SettlementListPage from "../../pages/merchant/SettlementListPage.jsx";
import SettlementDetailPage from "../../pages/merchant/SettlementDetailPage.jsx";

function MerchantPaymentListPage() {
  return <div>결제 조회 페이지</div>;
}

function MerchantPaymentDetailPage() {
  return <div>결제 상세 페이지</div>;
}

function MerchantRefundPage() {
  return <div>환불 현황 페이지</div>;
}

export default function MerchantRoutes() {
  return (
    <Routes>
      <Route index element={<Navigate to="/merchant/payments" replace />} />
      <Route path="payments" element={<PaymentListPage />} />
      <Route path="payments/:paymentId" element={<PaymentDetailPage />} />
      <Route path="settlements" element={<SettlementListPage />} />
      <Route path="settlements/:settlementId" element={<SettlementDetailPage />} />
      <Route path="refunds" element={<MerchantRefundPage />} />
    </Routes>
  );
}