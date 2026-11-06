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
    <div className="min-h-screen bg-slate-50 flex flex-col">
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

      <main className="mx-auto max-w-5xl px-4 py-6 flex-1 w-full">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white mt-auto">
        <div className="mx-auto flex max-w-5xl items-center justify-center gap-4 px-4 py-3 text-sm text-slate-500">
          <span>© {new Date().getFullYear()} ServiceDesk. All rights reserved.</span>
          <span className="text-slate-300">|</span>
          <a
            href="https://github.com/ananikets18/ServiceDesk"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-slate-500 hover:text-slate-900 transition-colors"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="h-4 w-4">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.385-1.335-1.755-1.335-1.755-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12c0-6.63-5.37-12-12-12z" />
            </svg>
            View on GitHub
          </a>
        </div>
      </footer>
    </div>
  );
}
