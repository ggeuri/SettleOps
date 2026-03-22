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

    searchParams.set("page", String(params.page ?? 0));
    searchParams.set("size", String(params.size ?? 20));

    const query = searchParams.toString();
    return requestJson(`/api/admin/refunds?${query}`);
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