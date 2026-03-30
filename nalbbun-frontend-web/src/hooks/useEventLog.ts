import { useCallback, useEffect, useMemo, useState } from 'react';

const MAX_LOG_LINES = 300;

function timestamp() {
  return new Date().toLocaleString('ko-KR', { hour12: false });
}

export function useEventLog(storageKey: string, seed: string[] = []) {
  const [lines, setLines] = useState<string[]>(() => {
    try {
      const raw = localStorage.getItem(storageKey);
      if (!raw) return seed;
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.map(String) : seed;
    } catch {
      return seed;
    }
  });

  useEffect(() => {
    localStorage.setItem(storageKey, JSON.stringify(lines));
  }, [storageKey, lines]);

  const append = useCallback((message: string, detail?: unknown) => {
    const suffix = detail == null
      ? ''
      : typeof detail === 'string'
        ? ` | ${detail}`
        : ` | ${JSON.stringify(detail)}`;
    setLines((prev) => [`[${timestamp()}] ${message}${suffix}`, ...prev].slice(0, MAX_LOG_LINES));
  }, []);

  const clear = useCallback(() => setLines([]), []);

  return useMemo(() => ({ lines, append, clear }), [lines, append, clear]);
}
