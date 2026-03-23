import { Routes, Route } from "react-router-dom";
import AdminAuditPage from "../../pages/admin/AdminAuditPage.jsx";
import BatchPage from "../../pages/admin/BatchPage.jsx"
import SettlementManagePage from "../../pages/admin/SettlementManagePage.jsx"
import SettlementDetailAdminPage from "../../pages/admin/SettlementDetailAdminPage.jsx"
import AdminHoldPage from "../../pages/admin/AdminHoldPage.jsx";
import RefundQueuePage from "../../pages/admin/RefundQueuePage.jsx";

export default function AdminRoutes() {
  return (
    <Routes>
      <Route path="audit" element={<AdminAuditPage />} />
      <Route path="settlement-batches" element={<BatchPage />}/>
      <Route path="settlements" element={<SettlementManagePage />}/>
      <Route path="holds" element={<AdminHoldPage />} />
      <Route path="refunds" element={<RefundQueuePage />} />
      <Route
        path="settlements/:settlementId"
        element={<SettlementDetailAdminPage />}
      />
    </Routes>
  );
}