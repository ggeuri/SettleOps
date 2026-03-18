import { Routes, Route } from "react-router-dom";
import SettlementListPage from "../../pages/merchant/SettlementListPage.jsx";
import SettlementDetailPage from "../../pages/merchant/SettlementDetailPage.jsx";

export default function MerchantRoutes() {
    return (
        <Routes>
            <Route path="settlements" element={<SettlementListPage />} />
            <Route 
                path="settlements/:settlementId"
                element={<SettlementDetailPage />} 
                />
        </Routes>
    );
}