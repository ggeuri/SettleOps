import { Routes, Route, Navigate } from "react-router-dom";
import AdminRoutes from "./app/router/AdminRoutes.jsx";
import ConsumerRoutes from "./app/router/ConsumerRoutes.jsx";

export default function App() {
  return (
    <Routes>
      {/* /admin 아래는 AdminRoutes가 처리 */}
      <Route path="/admin/*" element={<AdminRoutes />} />

      {/* C1 */}
      <Route path="/consumer/*" element={<ConsumerRoutes />} />

      {/* 기본 진입 */}
      <Route path="/" element={<Navigate to="/consumer/orders/new" replace />} />

      {/* 임시 홈 */}
      <Route path="/" element={<Navigate to="/admin/audit" replace />} />

      {/* 404 */}
      <Route path="*" element={<div>Not Found</div>} />
    </Routes>
  );
}