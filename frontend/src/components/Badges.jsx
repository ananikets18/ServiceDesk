const styles = {
  OPEN: "bg-blue-100 text-blue-700",
  IN_PROGRESS: "bg-amber-100 text-amber-700",
  RESOLVED: "bg-green-100 text-green-700",
  CLOSED: "bg-gray-200 text-gray-600",
};

const labels = {
  OPEN: "Open",
  IN_PROGRESS: "In Progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export function StatusBadge({ status }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${styles[status] || styles.OPEN}`}>
      {labels[status] || status}
    </span>
  );
}

const priorityStyles = {
  LOW: "bg-gray-100 text-gray-600",
  MEDIUM: "bg-sky-100 text-sky-700",
  HIGH: "bg-orange-100 text-orange-700",
  CRITICAL: "bg-red-100 text-red-700",
};

export function PriorityBadge({ priority }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${priorityStyles[priority] || ""}`}>
      {priority}
    </span>
  );
}

export function formatDate(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString();
}
