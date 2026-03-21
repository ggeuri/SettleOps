import { useLocation, useNavigate } from "react-router-dom";

export default function RequireLoginNotice({
  title = "인증 필요",
  message = "로그인이 필요합니다. 개발용 로그인 페이지에서 세션을 생성해주세요.",
  buttonText = "개발용 로그인 페이지로 이동",
  to = "/auth/dev-login",
  replace = false,
  className = "",
}) {
  const navigate = useNavigate();
  const location = useLocation();

  function handleMoveToLogin() {
    navigate(to, {
      replace,
      state: {
        from: location.pathname + location.search,
      },
    });
  }

  const rootClassName = ["guard-notice", className].filter(Boolean).join(" ");

  return (
    <div className={rootClassName} role="alert" aria-live="polite">
      <div className="guard-notice__title">{title}</div>
      <div className="guard-notice__description">{message}</div>

      {to && (
        <div className="action-panel" style={{ marginTop: "12px" }}>
          <button
            type="button"
            className="btn btn--primary"
            onClick={handleMoveToLogin}
          >
            {buttonText}
          </button>
        </div>
      )}
    </div>
  );
}