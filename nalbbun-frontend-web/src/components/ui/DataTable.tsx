import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  title: string;
  render: (row: T, index: number) => ReactNode;
}

export function DataTable<T>({ columns, rows, emptyText = '데이터가 없습니다.' }: { columns: Column<T>[]; rows: T[]; emptyText?: string }) {
  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>{columns.map(col => <th key={col.key}>{col.title}</th>)}</tr>
        </thead>
        <tbody>
          {!rows.length && (
            <tr><td colSpan={columns.length} className="table-empty">{emptyText}</td></tr>
          )}
          {rows.map((row, index) => (
            <tr key={index}>{columns.map(col => <td key={col.key}>{col.render(row, index)}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
