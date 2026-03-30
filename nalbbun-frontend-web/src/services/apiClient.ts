const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

function buildUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') || '';
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new Error(text || `${response.status} ${response.statusText}`);
  }
  if (contentType.includes('application/json')) {
    return response.json() as Promise<T>;
  }
  return response.text() as unknown as T;
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(buildUrl(path));
  return parseResponse<T>(response);
}

export async function apiSend<T>(path: string, method: 'POST'|'PUT'|'DELETE', body?: unknown): Promise<T> {
  const response = await fetch(buildUrl(path), {
    method,
    headers: body instanceof FormData ? undefined : { 'Content-Type': 'application/json' },
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined
  });
  return parseResponse<T>(response);
}

export async function apiPostForm<T>(path: string, form: FormData): Promise<T> {
  const response = await fetch(buildUrl(path), { method: 'POST', body: form });
  return parseResponse<T>(response);
}

export function buildSseUrl(path: string, params: Record<string, string>) {
  const query = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '')).toString();
  return `${buildUrl(path)}${query ? `?${query}` : ''}`;
}
