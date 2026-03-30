export function PlaceholderTable({ columns, rows }: { columns: string[]; rows: Array<Record<string, string>> }) {
  return (
    <table className="table">
      <thead>
        <tr>{columns.map(column => <th key={column}>{column}</th>)}</tr>
      </thead>
      <tbody>
        {rows.map((row, index) => (
          <tr key={index}>{columns.map(column => <td key={column}>{row[column] ?? '-'}</td>)}</tr>
        ))}
      </tbody>
    </table>
  );
}
