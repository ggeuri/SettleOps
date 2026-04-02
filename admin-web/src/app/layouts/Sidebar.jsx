// src/components/layout/Sidebar.jsx
import { NavLink, useLocation } from "react-router-dom";

/*
사용 방법

1) 단일 주소만 활성화
- 현재 경로가 to와 정확히 일치할 때만 active 처리
- 예:
  exactItem("거래 생성", "/consumer/orders/new")

2) 그룹 주소 활성화
- 목록 경로 + 하위 상세 경로까지 active 처리
- 예:
  groupItem("내 주문/결제 내역", "/consumer/orders")
  -> /consumer/orders
  -> /consumer/orders/123
  둘 다 활성화

*) exactItem 경로와 정확히 일치하는 경우에는 groupItem보다 exactItem이 우선한다.
*/

const exactItem = (label, to) => ({
  label,
  to,
  isActive: (pathname) => pathname === to,
});

const groupItem = (label, to, exactPaths = []) => ({
  label,
  to,
  isActive: (pathname) => {
    if (exactPaths.includes(pathname)) return false;
    return pathname === to || pathname.startsWith(`${to}/`);
  },
});

const consumerExactPaths = ["/consumer/orders/new"];

const menuGroups = [
  {
    title: "Consumer",
    items: [
      exactItem("거래 생성", "/consumer/orders/new"),
      groupItem("내 주문/결제 내역", "/consumer/orders", consumerExactPaths),
    ],
  },
  {
    title: "Merchant",
    items: [
      groupItem("결제 조회", "/merchant/payments"),
      groupItem("정산 리스트", "/merchant/settlements"),
      groupItem("환불 현황", "/merchant/refunds"),
    ],
  },
  {
    title: "Admin",
    items: [
      groupItem("Trace / Audit", "/admin/audit"),
      groupItem("배치 실행", "/admin/settlement-batches"),
      groupItem("정산 관리", "/admin/settlements"),
      groupItem("Hold 큐", "/admin/holds"),
      groupItem("환불 큐", "/admin/refunds"),
    ],
  },
];

function getLinkClass(active) {
  return active ? "sidebar__link sidebar__link--active" : "sidebar__link";
}

export default function Sidebar() {
  const { pathname } = useLocation();

  return (
    <div className="sidebar">
      <div className="sidebar__logo">
        <img
          src="/union-bl.svg"
          alt=""
          width={14}
          height={14}
          className="sidebar__logo-icon"
          aria-hidden="true"
        />
        <span className="sidebar__logo-text">SettleOps</span>
      </div>

      <nav className="sidebar__nav">
        {menuGroups.map((groupInfo) => (
          <div key={groupInfo.title} className="sidebar__group">
            <p className="sidebar__group-title">{groupInfo.title}</p>

            <div className="sidebar__menu">
              {groupInfo.items.map((menu) => {
                const active = menu.isActive(pathname);

                return (
                  <NavLink
                    key={menu.to}
                    to={menu.to}
                    className={getLinkClass(active)}
                  >
                    {menu.label}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
    </div>
  );
}