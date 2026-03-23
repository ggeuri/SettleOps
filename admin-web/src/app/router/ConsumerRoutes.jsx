// admin-web/src/app/router/ConsumerRoutes.jsx

import { Routes, Route } from "react-router-dom";
import OrderCreatePage from "../../pages/consumer/OrderCreatePage.jsx";
import OrderDetailPage from "../../pages/consumer/OrderDetailPage.jsx";
import OrderListPage from "../../pages/consumer/OrderListPage.jsx";

export default function ConsumerRoutes() {
    return (
        <Routes>
            <Route path="orders/new" element={<OrderCreatePage />} />
            <Route path="orders/:orderId" element={<OrderDetailPage />} />
            <Route path="orders" element={<OrderListPage />} />
        </Routes>
    );
}