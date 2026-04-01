import { useCallback, useEffect, useRef, useState } from 'react';

export function useAsyncData<T>(loader: () => Promise<T>, immediate = true) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const loaderRef = useRef(loader);

  useEffect(() => {
    loaderRef.current = loader;
  }, [loader]);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const value = await loaderRef.current();
      setData(value);
      return value;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!immediate) return;
    refresh().catch(() => undefined);
  }, [immediate, refresh]);

  return { data, setData, loading, error, refresh };
}
