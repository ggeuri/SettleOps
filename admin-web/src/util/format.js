// /admin-web/src/utils/format.js
// 사용: import { formatDateTime, formatDateTimeWithSeconds, formatKrw } from "../../utils/format.js";

// yyyy-MM-dd HH:mm
export function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  if (typeof value === "string") {
    return value.replace("T", " ").slice(0, 16);
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

// yyyy-MM-dd HH:mm:ss
export function formatDateTimeWithSeconds(value) {
  if (!value) {
    return "-";
  }

  if (typeof value === "string") {
    return value.replace("T", " ").slice(0, 19);
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const seconds = String(date.getSeconds()).padStart(2, "0");

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// 금액표기 포멧
export function formatKrw(value) {
  return Number(value ?? 0).toLocaleString();
}