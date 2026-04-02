import { beginGlobalLoading, endGlobalLoading, notifyGlobal } from '../lib/uiFeedback';
type ApiEnvelope<T> = {
  success?: boolean;
  data?: T;
  error?: { code?: string; message?: string } | null;
  timestamp?: string;
  meta?: Record<string, unknown>;
};

const API_BASE_URL = String(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').trim().replace(/\/$/, '');
const CONVERSATION_ID_STORAGE_KEY = 'nalbbun.current.conversation-id';

function buildUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

function isApiEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  return Boolean(value) && typeof value === 'object' && (
    'success' in (value as Record<string, unknown>) ||
    'data' in (value as Record<string, unknown>) ||
    'error' in (value as Record<string, unknown>)
  );
}

function getConversationId() {
  if (typeof window === 'undefined') return '';
  return String(window.localStorage.getItem(CONVERSATION_ID_STORAGE_KEY) || '').trim();
}

function buildHeaders(extraHeaders?: Record<string, string>) {
  const headers = new Headers(extraHeaders || {});
  const conversationId = getConversationId();
  if (conversationId) {
    headers.set('X-Conversation-Id', conversationId);
  }
  return headers;
}

async function parseResponse<T>(response: Response): Promise<T> {
  const contentType = response.headers.get('content-type') || '';

  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    if (contentType.includes('application/json')) {
      const errorBody = await response.json().catch(() => null);
      if (isApiEnvelope(errorBody) && errorBody.error?.message) {
        message = errorBody.error.message;
      } else if (errorBody && typeof errorBody === 'object' && 'message' in errorBody) {
        message = String((errorBody as Record<string, unknown>).message);
      }
    } else {
      const text = await response.text().catch(() => '');
      if (text) message = text;
    }
    throw new Error(message);
  }

  if (contentType.includes('application/json')) {
    const json = await response.json();
    if (isApiEnvelope(json)) {
      if (json.success === false) {
        throw new Error(json.error?.message || '요청 처리 중 오류가 발생했습니다.');
      }
      return (json.data as T) ?? (json as T);
    }
    return json as T;
  }

  return response.text() as unknown as T;
}

export function setCurrentConversationId(conversationId?: string) {
  if (typeof window === 'undefined') return;
  const normalized = String(conversationId || '').trim();
  if (normalized) {
    window.localStorage.setItem(CONVERSATION_ID_STORAGE_KEY, normalized);
  } else {
    window.localStorage.removeItem(CONVERSATION_ID_STORAGE_KEY);
  }
}

async function withGlobalRequest<T>(message: string | undefined, runner: () => Promise<T>, successMessage?: string): Promise<T> {
  beginGlobalLoading(message);
  try {
    const result = await runner();
    if (successMessage) notifyGlobal(successMessage, 'success');
    return result;
  } catch (error) {
    const detail = error instanceof Error ? error.message : String(error);
    notifyGlobal(detail, 'error');
    throw error;
  } finally {
    endGlobalLoading();
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  return withGlobalRequest<T>(undefined, async () => {
    const response = await fetch(buildUrl(path), {
      credentials: 'include',
      headers: buildHeaders()
    });
    return parseResponse<T>(response);
  });
}

export async function apiSend<T>(path: string, method: 'POST'|'PUT'|'DELETE', body?: unknown): Promise<T> {
  const actionLabel = method === 'DELETE' ? '삭제 중입니다...' : '저장 중입니다...';
  return withGlobalRequest<T>(actionLabel, async () => {
    const response = await fetch(buildUrl(path), {
      method,
      credentials: 'include',
      headers: body instanceof FormData ? buildHeaders() : buildHeaders({ 'Content-Type': 'application/json' }),
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined
    });
    return parseResponse<T>(response);
  }, method === 'DELETE' ? '삭제가 완료되었습니다.' : '저장이 완료되었습니다.');
}

export async function apiPostForm<T>(path: string, form: FormData): Promise<T> {
  return withGlobalRequest<T>('업로드 중입니다...', async () => {
    const response = await fetch(buildUrl(path), {
      method: 'POST',
      credentials: 'include',
      headers: buildHeaders(),
      body: form
    });
    return parseResponse<T>(response);
  }, '업로드가 완료되었습니다.');
}

export function buildSseUrl(path: string, params: Record<string, string>) {
  const search = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== ''));
  const conversationId = getConversationId();
  if (conversationId && !search.get('conversationId')) {
    search.set('conversationId', conversationId);
  }
  const query = search.toString();
  return `${buildUrl(path)}${query ? `?${query}` : ''}`;
}
