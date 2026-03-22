// /admin-web/src/api/meApi.js

import { requestJson } from "./http.js";

export function getMe() {
  return requestJson("/api/me");
}