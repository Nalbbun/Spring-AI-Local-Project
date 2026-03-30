import { apiGet, apiSend } from './apiClient';
import type { ApiKeyEntry, ProviderStatus } from '../types/api';

export const keyApi = {
  providers: () => apiGet<ProviderStatus[]>('/api/api-keys/providers'),
  list: (provider?: string) => apiGet<ApiKeyEntry[]>(provider ? `/api/api-keys?provider=${encodeURIComponent(provider)}` : '/api/api-keys'),
  create: (payload: Record<string, unknown>) => apiSend<ApiKeyEntry>('/api/api-keys', 'POST', payload),
  update: (id: string, payload: Record<string, unknown>) => apiSend<ApiKeyEntry>(`/api/api-keys/${encodeURIComponent(id)}`, 'PUT', payload),
  remove: (id: string) => apiSend<void>(`/api/api-keys/${encodeURIComponent(id)}`, 'DELETE'),
  activate: (id: string) => apiSend<ApiKeyEntry>(`/api/api-keys/${encodeURIComponent(id)}/activate`, 'POST'),
  reveal: (id: string) => apiGet<{ keyValue: string }>(`/api/api-keys/${encodeURIComponent(id)}/reveal`)
};
