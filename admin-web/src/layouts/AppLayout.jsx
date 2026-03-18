// src/layouts/AppLayout.jsx
import { Outlet } from "react-router-dom";
import Sidebar from "../layouts/Sidebar";
import AppHeader from "../layouts/AppHeader";
import "../style/layout.css";

export default function AppLayout() {
  return (
    <div className="app-layout">
      <aside className="app-layout__sidebar">
        <Sidebar />
      </aside>

      <div className="app-layout__main">
        <header className="app-layout__header">
          <AppHeader />
        </header>

        <main className="app-layout__content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}