import { Routes, Route } from "react-router-dom";
import ConsumerOrderCreatePage from "../pages/consumer/ConsumerOrderCreatePage.jsx";

export default function ConsumerRoutes() {
    return (
        <Routes>
            <Route path="orders/new" element={<ConsumerOrderCreatePage />} />
        </Routes>
    );
}