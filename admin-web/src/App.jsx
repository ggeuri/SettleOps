import { Routes, Route, Navigate } from "react-router-dom";
import AppLayout from "./layouts/AppLayout";
import AdminRoutes from "./app/router/AdminRoutes.jsx";
import MerchantRoutes from "./app/router/MerchantRoutes.jsx";
import ConsumerRoutes from "./app/router/ConsumerRoutes.jsx";
import CommonUiSamplePage from "./pages/_sample/CommonUiSamplePage.jsx";

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        {/* 기본 진입 */}
        <Route path="/" element={<Navigate to="/consumer/orders/new" replace />} />

        {/* /admin 아래는 AdminRoutes가 처리 */}
        <Route path="/admin/*" element={<AdminRoutes />} />

        {/* /merchant 아래는 MerchantRoutes가 처리 */}
        <Route path="/merchant/*" element={<MerchantRoutes />} />

        {/* /consumer 아래는 ConsumerRoutes가 처리 */}
        <Route path="/consumer/*" element={<ConsumerRoutes />} />

        {/* 공통 UI 샘플 미리보기 */}
        <Route path="/sample/ui" element={<CommonUiSamplePage />} />
      </Route>

      {/* 404 */}
      <Route path="*" element={<div>Not Found</div>} />
    </Routes>
  );
}