import { Routes, Route } from "react-router-dom";
import OrderCreatePage from "../../pages/consumer/OrderCreatePage.jsx";
import MyOrdersPage from "../../pages/consumer/MyOrdersPage.jsx";

export default function ConsumerRoutes() {
    return (
        <Routes>
            <Route path="orders/new" element={<OrderCreatePage />} />
            <Route path="orders" element={<MyOrdersPage />} />
        </Routes>
    );
}