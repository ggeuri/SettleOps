import { Routes, Route } from "react-router-dom";
import AdminAuditPage from "../../pages/AdminAuditPage.jsx";

export default function AdminRoutes() {
  return (
    <Routes>
      <Route path="audit" element={<AdminAuditPage />} />
    </Routes>
  );
}