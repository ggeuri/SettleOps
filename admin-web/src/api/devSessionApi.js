// /admin-web/src/api/devSessionApi.js

import { requestJson } from "./http.js";

export function loginConsumerSession(buyerId) {
  return requestJson(`/api/dev/login-consumer?buyerId=${encodeURIComponent(buyerId)}`, {
    method: "POST",
  });
}

export function loginMerchantSession(merchantId) {
  return requestJson(`/api/dev/login-merchant?merchantId=${encodeURIComponent(merchantId)}`, {
    method: "POST",
  });
}

export function loginAdminSession(adminId) {
  return requestJson(`/api/dev/login-admin?adminId=${encodeURIComponent(adminId)}`, {
    method: "POST",
  });
}

export function logoutSession() {
  return requestJson(`/api/dev/logout`, {
    method: "POST",
  });
}