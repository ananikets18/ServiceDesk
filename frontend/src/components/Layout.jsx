import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const linkClass = ({ isActive }) =>
    `rounded-md px-3 py-2 text-sm font-medium ${
      isActive ? "bg-slate-800 text-white" : "text-slate-300 hover:bg-slate-800 hover:text-white"
    }`;

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link to="/" className="flex items-center gap-2">
            <span className="rounded bg-slate-900 px-2 py-1 text-sm font-bold text-white">SD</span>
            <span className="text-lg font-semibold text-slate-900">ServiceDesk</span>
          </Link>

          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-slate-600 sm:block">
              {user.name} · <span className="font-medium">{user.role}</span>
            </span>
            <button
              onClick={handleLogout}
              className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-100"
            >
              Log out
            </button>
          </div>
        </div>

        <nav className="mx-auto flex max-w-5xl gap-2 bg-slate-900 px-4 py-2">
          <NavLink to="/" end className={linkClass}>
            Tickets
          </NavLink>
          {user.role === "REQUESTER" && (
            <NavLink to="/tickets/new" className={linkClass}>
              New Ticket
            </NavLink>
          )}
          {user.role === "ADMIN" && (
            <NavLink to="/users" className={linkClass}>
              Users
            </NavLink>
          )}
        </nav>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
