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

  const redirectTo = location.state?.from || "/";
  const [loadingKey, setLoadingKey] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  async function handleLoginConsumer(buyerId) {
    try {
      setLoadingKey(`consumer:${buyerId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginConsumerSession(buyerId);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error?.body?.message || "Consumer 세션 생성에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  async function handleLoginMerchant(merchantId) {
    try {
      setLoadingKey(`merchant:${merchantId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginMerchantSession(merchantId);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setErrorMessage(error?.body?.message || "Merchant 세션 생성에 실패했습니다.");
    } finally {
      setLoadingKey("");
    }
  }

  async function handleLoginAdmin(adminId) {
    try {
      setLoadingKey(`admin:${adminId}`);
      setErrorMessage("");
      setSuccessMessage("");

      await loginAdminSession(adminId);
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
              <div>Consumer</div>
            </div>

            <div className="action-panel" style={{ display: "grid", gap: "12px" , gridTemplateColumns: "repeat(3, 1fr)"}}>
              {CONSUMER_IDS.map((buyerId) => (
                <button
                  key={buyerId}
                  type="button"
                  className="btn btn--primary"
                  onClick={() => handleLoginConsumer(buyerId)}
                  disabled={loadingKey !== ""}
                >
                  {loadingKey === `consumer:${buyerId}`
                    ? "세션 생성 중..."
                    : `${buyerId}`}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Merchant</div>
            </div>

            <div className="action-panel" style={{ display: "grid", gap: "12px" , gridTemplateColumns: "repeat(3, 1fr)"}}>
              {MERCHANT_IDS.map((merchantId) => (
                <button
                  key={merchantId}
                  type="button"
                  className="btn btn--primary"
                  onClick={() => handleLoginMerchant(merchantId)}
                  disabled={loadingKey !== ""}
                >
                  {loadingKey === `merchant:${merchantId}`
                    ? "세션 생성 중..."
                    : `${merchantId}`}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="table-toolbar" style={{ marginBottom: "16px" }}>
              <div>Admin</div>
            </div>

            <div className="action-panel" style={{ display: "grid", gap: "12px" , gridTemplateColumns: "repeat(3, 1fr)"}}>
              {ADMIN_IDS.map((adminId) => (
                <button
                  key={adminId}
                  type="button"
                  className="btn btn--primary"
                  onClick={() => handleLoginAdmin(adminId)}
                  disabled={loadingKey !== ""}
                >
                  {loadingKey === `admin:${adminId}`
                    ? "세션 생성 중..."
                    : `${adminId}`}
                </button>
              ))}
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