export function LogPanel({ lines }: { lines: string[] }) {
  if (!lines.length) {
    return <div className="log-panel">로그가 없습니다.</div>;
  }

  return (
    <div className="log-panel">
      {lines.map((line, index) => (
        <div key={`${index}-${line.slice(0, 24)}`} className="log-line">
          {line}
        </div>
      ))}
    </div>
  );
}
