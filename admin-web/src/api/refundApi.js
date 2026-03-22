import { requestJson } from "./http.js";

export function getRefundContext(paymentId) {
    return requestJson(`/api/payments/${paymentId}/refund-context`);
}

export function createRefund(payload) {
    return requestJson("/api/refunds", {
        method: "POST",
        body: JSON.stringify(payload),
    });
}

export function getMyRefunds() {
    return requestJson("/api/me/refunds");
}

export function getAdminRefunds(params = {}) {
    const searchParams = new URLSearchParams();

    if (params.status) searchParams.set("status", params.status);
    if (params.from) searchParams.set("from", params.from);
    if (params.to) searchParams.set("to", params.to);

    const query = searchParams.toString();
    const url = query ? `/api/admin/refunds?${query}` : "/api/admin/refunds";

    return requestJson(url);
}

export function approveRefund(refundId, comment) {
    return requestJson(`/api/admin/refunds/${refundId}/approve`, {
        method: "PATCH",
        body: JSON.stringify({ comment }),
    });
}

export function rejectRefund(refundId, comment) {
    return requestJson(`/api/admin/refunds/${refundId}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ comment }),
    });
}