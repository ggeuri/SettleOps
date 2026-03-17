import { requestJson } from "./http.js";

export function createConsumerOrder(payload) {
    return requestJson("/api/consumer/orders", {
        method: "POST",
        body: JSON.stringify(payload),
    });
}