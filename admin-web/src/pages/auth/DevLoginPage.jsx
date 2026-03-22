// /admin-web/src/pages/auth/DevLoginPage.jsx

import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import PageLayout from "../../components/layout/PageLayout.jsx";
import {
  loginConsumerSession,
  loginMerchantSession,
  loginAdminSession,
  logoutSession,
} from "../../api/devSessionApi.js";

const CONSUMER_IDS = ["BUYER_1001", "BUYER_2001", "BUYER_3001"];
const MERCHANT_IDS = ["MERCHANT_1001", "MERCHANT_2001", "MERCHANT_3001"];
const ADMIN_IDS = ["ADMIN_1001", "ADMIN_2001", "ADMIN_3001"];

export default function DevLoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const rawFrom = location.state?.from;
  const redirectTo =
    rawFrom && rawFrom !== "/auth/dev-login" ? rawFrom : "/";

  const [consumerId, setConsumerId] = useState(CONSUMER_IDS[0]);
  const [merchantId, setMerchantId] = useState(MERCHANT_IDS[0]);
  const [adminId, setAdminId] = useState(ADMIN_IDS[0]);

  const [loadingKey, setLoadingKey] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  async function handleLoginConsumer(targetBuyerId) {
    try {
      setLoadingKey(`consumer:${targetBuyerId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginConsumerSession(targetBuyerId);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error?.body?.message || "Consumer 세션 생성에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  async function handleLoginMerchant(targetMerchantId) {
    try {
      setLoadingKey(`merchant:${targetMerchantId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginMerchantSession(targetMerchantId);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error?.body?.message || "Merchant 세션 생성에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  async function handleLoginAdmin(targetAdminId) {
    try {
      setLoadingKey(`admin:${targetAdminId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginAdminSession(targetAdminId);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error?.body?.message || "Admin 세션 생성에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  async function handleLogout() {
    try {
      setLoadingKey("logout");
      setErrorMessage("");
      setSuccessMessage("");

      await logoutSession();
      setSuccessMessage("세션이 정리되었습니다.");
    } catch (error) {
      setErrorMessage(error?.body?.message || "세션 종료에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  return (
    <PageLayout
      title="개발용 로그인"
      description="개발 환경에서 역할별 로그인 세션을 생성하거나 종료하는 페이지입니다."
    >
      {errorMessage && (
        <div className="guard-notice" style={{ marginBottom: "16px" }}>
          <div className="guard-notice__title">요청 실패</div>
          <div className="guard-notice__description">{errorMessage}</div>
        </div>
      )}

      {successMessage && (
        <div className="guard-notice" style={{ marginBottom: "16px" }}>
          <div className="guard-notice__title">처리 완료</div>
          <div className="guard-notice__description">{successMessage}</div>
        </div>
      )}

      <div className="guard-notice" style={{ marginBottom: "16px" }}>
        <div className="guard-notice__title">공통 안내</div>
        <div className="guard-notice__description">
          역할을 선택하면 해당 세션을 생성한 뒤, 접근을 시도했던 기존 페이지로 자동 이동합니다.
          로그아웃은 현재 세션을 정리합니다.
        </div>
      </div>

      <div className="page-grid-2">
        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Consumer - buyerId</div>
            </div>

            <div style={{ marginBottom: "12px" }}>
              <input
                className="input"
                value={consumerId}
                onChange={(e) => setConsumerId(e.target.value)}
                placeholder="buyerId 입력"
                disabled={loadingKey !== ""}
              />
            </div>

            <div
              className="action-panel"
              style={{ display: "grid", gap: "12px", gridTemplateColumns: "repeat(3, 1fr)" }}
            >
              {CONSUMER_IDS.map((buyerId) => (
                <button
                  key={buyerId}
                  type="button"
                  className="btn btn--secondary"
                  onClick={() => setConsumerId(buyerId)}
                  disabled={loadingKey !== ""}
                >
                  {buyerId}
                </button>
              ))}
            </div>

            <div className="action-panel" style={{ marginTop: "12px" }}>
              <button
                type="button"
                className="btn btn--primary"
                style={{ width: "100%" }}
                onClick={() => handleLoginConsumer(consumerId)}
                disabled={loadingKey !== "" || !consumerId.trim()}
              >
                {loadingKey === `consumer:${consumerId}`
                  ? "세션 생성 중..."
                  : "Consumer 로그인"}
              </button>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Merchant - merchantId</div>
            </div>

            <div style={{ marginBottom: "12px" }}>
              <input
                className="input"
                value={merchantId}
                onChange={(e) => setMerchantId(e.target.value)}
                placeholder="merchantId 입력"
                disabled={loadingKey !== ""}
              />
            </div>

            <div
              className="action-panel"
              style={{ display: "grid", gap: "12px", gridTemplateColumns: "repeat(3, 1fr)" }}
            >
              {MERCHANT_IDS.map((item) => (
                <button
                  key={item}
                  type="button"
                  className="btn btn--secondary"
                  onClick={() => setMerchantId(item)}
                  disabled={loadingKey !== ""}
                >
                  {item}
                </button>
              ))}
            </div>

            <div className="action-panel" style={{ marginTop: "12px" }}>
              <button
                type="button"
                className="btn btn--primary"
                style={{ width: "100%" }}
                onClick={() => handleLoginMerchant(merchantId)}
                disabled={loadingKey !== "" || !merchantId.trim()}
              >
                {loadingKey === `merchant:${merchantId}`
                  ? "세션 생성 중..."
                  : "Merchant 로그인"}
              </button>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Admin - adminId</div>
            </div>

            <div style={{ marginBottom: "12px" }}>
              <input
                className="input"
                value={adminId}
                onChange={(e) => setAdminId(e.target.value)}
                placeholder="adminId 입력"
                disabled={loadingKey !== ""}
              />
            </div>

            <div
              className="action-panel"
              style={{ display: "grid", gap: "12px", gridTemplateColumns: "repeat(3, 1fr)" }}
            >
              {ADMIN_IDS.map((item) => (
                <button
                  key={item}
                  type="button"
                  className="btn btn--secondary"
                  onClick={() => setAdminId(item)}
                  disabled={loadingKey !== ""}
                >
                  {item}
                </button>
              ))}
            </div>

            <div className="action-panel" style={{ marginTop: "12px" }}>
              <button
                type="button"
                className="btn btn--primary"
                style={{ width: "100%" }}
                onClick={() => handleLoginAdmin(adminId)}
                disabled={loadingKey !== "" || !adminId.trim()}
              >
                {loadingKey === `admin:${adminId}`
                  ? "세션 생성 중..."
                  : "Admin 로그인"}
              </button>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Logout</div>
            </div>

            <div className="action-panel" style={{ display: "grid", gap: "12px" }}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleLogout}
                disabled={loadingKey !== ""}
              >
                {loadingKey === "logout" ? "세션 종료 중..." : "현재 세션 로그아웃"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
}