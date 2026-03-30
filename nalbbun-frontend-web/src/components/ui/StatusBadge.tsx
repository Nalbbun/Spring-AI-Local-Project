export function StatusBadge({ label, tone = 'default' }: { label: string; tone?: 'default' | 'success' | 'warning' | 'danger' | 'info' }) {
  return <span className={`status-badge ${tone}`}>{label}</span>;
}
