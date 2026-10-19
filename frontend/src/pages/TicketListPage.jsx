import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api";
import { PriorityBadge, StatusBadge, formatDate } from "../components/Badges";
import { useAuth } from "../AuthContext";

export default function TicketListPage() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadTickets();
  }, []);

  async function loadTickets() {
    setLoading(true);
    try {
      const response = await api.get("/tickets");
      setTickets(response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not load tickets.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Tickets</h1>
          <p className="text-sm text-slate-500">
            {tickets.length} ticket{tickets.length === 1 ? "" : "s"}
          </p>
        </div>
        {user.role === "REQUESTER" && (
          <Link
            to="/tickets/new"
            className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
          >
            New ticket
          </Link>
        )}
      </div>

      {error && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        {loading ? (
          <p className="p-6 text-sm text-slate-500">Loading tickets...</p>
        ) : tickets.length === 0 ? (
          <div className="p-10 text-center">
            <p className="text-sm text-slate-600">
              {user.role === "REQUESTER" ? "No tickets yet." : "No tickets to show."}
            </p>
            {user.role === "REQUESTER" && (
              <Link to="/tickets/new" className="mt-2 inline-block text-sm font-medium text-slate-900 underline">
                Create the first one
              </Link>
            )}
          </div>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">#</th>
                <th className="px-4 py-3">Title</th>
                <th className="px-4 py-3">Priority</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Requester</th>
                <th className="px-4 py-3">Assignee</th>
                <th className="px-4 py-3">Created</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {tickets.map((ticket) => (
                <tr key={ticket.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-slate-500">{ticket.id}</td>
                  <td className="px-4 py-3">
                    <Link to={`/tickets/${ticket.id}`} className="font-medium text-slate-900 hover:underline">
                      {ticket.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <PriorityBadge priority={ticket.priority} />
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={ticket.status} />
                  </td>
                  <td className="px-4 py-3 text-slate-600">{ticket.requesterName}</td>
                  <td className="px-4 py-3 text-slate-600">{ticket.assigneeName || "Unassigned"}</td>
                  <td className="px-4 py-3 text-slate-500">{formatDate(ticket.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
