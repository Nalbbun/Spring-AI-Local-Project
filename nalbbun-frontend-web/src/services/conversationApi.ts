import { apiGet, apiSend } from './apiClient';
import type { ConversationSnapshot, MemorySummary } from '../types/api';

export const conversationApi = {
  summary: () => apiGet<MemorySummary>('/api/memory/summary'),
  list: () => apiGet<string[]>('/api/memory/conversations'),
  detail: (conversationId: string) => apiGet<ConversationSnapshot>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`),
  remove: (conversationId: string) => apiSend<void>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, 'DELETE')
};
