import { Routes, Route } from "react-router-dom";
import OrderCreatePage from "../../pages/consumer/OrderCreatePage.jsx";

export default function ConsumerRoutes() {
    return (
        <Routes>
            <Route path="orders/new" element={<OrderCreatePage />} />
        </Routes>
    );
}