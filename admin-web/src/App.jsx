import { Routes, Route, Navigate } from "react-router-dom";
import AppLayout from "./layouts/AppLayout";
import AdminRoutes from "./app/router/AdminRoutes.jsx";
import ConsumerRoutes from "./app/router/ConsumerRoutes.jsx";
<<<<<<< HEAD
import MerchantRoutes from "./app/router/MerchantRoutes.jsx";
=======
import CommonUiSamplePage from "./pages/_sample/CommonUiSamplePage.jsx";

>>>>>>> origin/feat/frontend-layout-routing

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        {/* 기본 진입 */}
        <Route path="/" element={<Navigate to="/consumer/orders/new" replace />} />

        {/* /admin 아래는 AdminRoutes가 처리 */}
        <Route path="/admin/*" element={<AdminRoutes />} />

<<<<<<< HEAD
      {/* merchant */}
      <Route path="/merchant/*" element={<MerchantRoutes />}/>

      {/* 기본 진입 */}
      <Route path="/" element={<Navigate to="/consumer/orders/new" replace />} />
=======
        {/* /consumer 아래는 ConsumerRoutes가 처리 */}
        <Route path="/consumer/*" element={<ConsumerRoutes />} />
        
        {/* 공통 UI 샘플 미리보기 */}
        <Route path="/sample/ui" element={<CommonUiSamplePage />} />
      </Route>
>>>>>>> origin/feat/frontend-layout-routing

      {/* 404 */}
      <Route path="*" element={<div>Not Found</div>} />
    </Routes>
  );
}