import { Routes, Route } from "react-router-dom";
import AdminAuditPage from "../../pages/AdminAuditPage.jsx";
import BatchPage from "../../pages/admin/BatchPage.jsx"
import SettlementManagePage from "../../pages/admin/SettlementManagePage.jsx"
import SettlementDetailAdminPage from "../../pages/admin/SettlementDetailAdminPage.jsx"

export default function AdminRoutes() {
  return (
    <Routes>
      <Route path="audit" element={<AdminAuditPage />} />
      <Route path="settlement-batches" element={<BatchPage />}/>
      <Route path="settlements" element={<SettlementManagePage />}/>
      <Route
        path="settlements/:settlementId"
        element={<SettlementDetailAdminPage />}
      />
    </Routes>
  );
}