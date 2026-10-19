import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../api";
import { PriorityBadge, StatusBadge, formatDate } from "../components/Badges";
import { useAuth } from "../AuthContext";

const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

export default function TicketDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [ticket, setTicket] = useState(null);
  const [comments, setComments] = useState([]);
  const [agents, setAgents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [assigneeId, setAssigneeId] = useState("");
  const [busy, setBusy] = useState(false);

  const canManage = user.role === "AGENT" || user.role === "ADMIN";

  useEffect(() => {
    loadTicket();
  }, [id]);

  useEffect(() => {
    if (canManage) {
      loadAgents();
    }
  }, [canManage]);

  async function loadTicket() {
    setLoading(true);
    try {
      const [ticketResponse, commentsResponse] = await Promise.all([
        api.get(`/tickets/${id}`),
        api.get(`/tickets/${id}/comments`),
      ]);
      setTicket(ticketResponse.data);
      setComments(commentsResponse.data);
      setAssigneeId(ticketResponse.data.assigneeId || "");
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not load this ticket.");
    } finally {
      setLoading(false);
    }
  }

  async function loadAgents() {
    try {
      const response = await api.get("/users/agents");
      setAgents(response.data);
    } catch {
      // Not critical - the assign dropdown just stays empty.
    }
  }

  async function changeStatus(status) {
    setBusy(true);
    try {
      const response = await api.patch(`/tickets/${id}/status`, { status });
      setTicket(response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not update the status.");
    } finally {
      setBusy(false);
    }
  }

  async function assign() {
    if (!assigneeId) return;
    setBusy(true);
    try {
      const response = await api.patch(`/tickets/${id}/assign`, { assigneeId: Number(assigneeId) });
      setTicket(response.data);
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not assign the ticket.");
    } finally {
      setBusy(false);
    }
  }

  async function addComment(event) {
    event.preventDefault();
    if (!message.trim()) return;
    setBusy(true);
    try {
      const response = await api.post(`/tickets/${id}/comments`, { message });
      setComments((current) => [...current, response.data]);
      setMessage("");
      setError("");
    } catch (err) {
      setError(err.response?.data?.message || "Could not add the comment.");
    } finally {
      setBusy(false);
    }
  }

  if (loading) {
    return <p className="text-sm text-slate-500">Loading ticket...</p>;
  }

  if (!ticket) {
    return (
      <div>
        <div className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
        <Link to="/" className="mt-4 inline-block text-sm font-medium text-slate-900 underline">
          Back to tickets
        </Link>
      </div>
    );
  }

  // Agents may only assign themselves; admins can pick any agent.
  const assignOptions = user.role === "AGENT" ? agents.filter((agent) => agent.id === user.id) : agents;

  return (
    <div>
      <Link to="/" className="text-sm text-slate-600 hover:underline">
        &larr; Back to tickets
      </Link>

      {error && (
        <div className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mt-4 rounded-lg border border-slate-200 bg-white p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-medium text-slate-500">Ticket #{ticket.id}</p>
            <h1 className="text-xl font-semibold text-slate-900">{ticket.title}</h1>
          </div>
          <div className="flex gap-2">
            <PriorityBadge priority={ticket.priority} />
            <StatusBadge status={ticket.status} />
          </div>
        </div>

        <p className="mt-4 whitespace-pre-line text-sm text-slate-700">{ticket.description}</p>

        <dl className="mt-5 grid grid-cols-2 gap-3 border-t border-slate-100 pt-4 text-sm sm:grid-cols-4">
          <div>
            <dt className="text-xs text-slate-500">Requester</dt>
            <dd className="font-medium text-slate-800">{ticket.requesterName}</dd>
          </div>
          <div>
            <dt className="text-xs text-slate-500">Assignee</dt>
            <dd className="font-medium text-slate-800">{ticket.assigneeName || "Unassigned"}</dd>
          </div>
          <div>
            <dt className="text-xs text-slate-500">Created</dt>
            <dd className="text-slate-700">{formatDate(ticket.createdAt)}</dd>
          </div>
          <div>
            <dt className="text-xs text-slate-500">Updated</dt>
            <dd className="text-slate-700">{formatDate(ticket.updatedAt)}</dd>
          </div>
        </dl>
      </div>

      {canManage && (
        <div className="mt-4 grid gap-4 rounded-lg border border-slate-200 bg-white p-5 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Change status</label>
            <div className="flex flex-wrap gap-2">
              {STATUSES.map((status) => (
                <button
                  key={status}
                  disabled={busy || ticket.status === status}
                  onClick={() => changeStatus(status)}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-40"
                >
                  {status.replace("_", " ")}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Assign to</label>
            <div className="flex gap-2">
              <select
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value)}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
              >
                <option value="">Select an agent</option>
                {assignOptions.map((agent) => (
                  <option key={agent.id} value={agent.id}>
                    {agent.name}
                  </option>
                ))}
              </select>
              <button
                onClick={assign}
                disabled={busy || !assigneeId}
                className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-40"
              >
                Assign
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="mt-4 rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-3 text-sm font-semibold text-slate-900">Comments</h2>

        {comments.length === 0 ? (
          <p className="text-sm text-slate-500">No comments yet.</p>
        ) : (
          <ul className="space-y-3">
            {comments.map((comment) => (
              <li key={comment.id} className="rounded border border-slate-100 bg-slate-50 p-3">
                <div className="mb-1 flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium text-slate-700">{comment.authorName}</span>
                  <span>{formatDate(comment.createdAt)}</span>
                </div>
                <p className="whitespace-pre-line text-sm text-slate-700">{comment.message}</p>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={addComment} className="mt-4">
          <textarea
            rows={3}
            maxLength={2000}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            placeholder="Add a comment..."
          />
          <div className="mt-2 flex justify-end">
            <button
              type="submit"
              disabled={busy || !message.trim()}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-40"
            >
              Post comment
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
