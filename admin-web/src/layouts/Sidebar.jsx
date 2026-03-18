// src/components/layout/Sidebar.jsx
import { NavLink } from "react-router-dom";

const menuGroups = [
  {
    title: "Consumer",
    items: [
      { label: "거래 생성", to: "/consumer/orders/new" },
      { label: "내 주문/결제 내역", to: "/consumer/payments" },
    ],
  },
  {
    title: "Merchant",
    items: [
      { label: "결제 조회", to: "/merchant/payments" },
      { label: "환불 현황", to: "/merchant/refunds" },
    ],
  },
  {
    title: "Admin",
    items: [
      { label: "Trace / Audit", to: "/admin/audit" },
      { label: "배치 실행", to: "/admin/batches" },
      { label: "정산 관리", to: "/admin/settlements" },
      { label: "Hold 큐", to: "/admin/holds" },
      { label: "환불 큐", to: "/admin/refunds" },
    ],
  },
];

export default function Sidebar() {
  return (
    <div className="sidebar">
      <div className="sidebar__logo">SettleOps</div>

      <nav className="sidebar__nav">
        {menuGroups.map((group) => (
          <div key={group.title} className="sidebar__group">
            <p className="sidebar__group-title">{group.title}</p>

            <div className="sidebar__menu">
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    isActive ? "sidebar__link sidebar__link--active" : "sidebar__link"
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>
    </div>
  );
}