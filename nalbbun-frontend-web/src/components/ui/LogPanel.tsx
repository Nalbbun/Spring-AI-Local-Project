export function LogPanel({ lines }: { lines: string[] }) {
  return <div className="log-panel">{lines.length ? lines.join('\n') : '로그가 없습니다.'}</div>;
}
