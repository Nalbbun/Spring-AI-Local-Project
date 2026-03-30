import { apiGet, apiSend } from './apiClient';
import type { ConversationListResult, ConversationSnapshot, MemorySummary } from '../types/api';

export const conversationApi = {
  summary: () => apiGet<MemorySummary>('/api/memory/summary'),
  list: async () => {
    const result = await apiGet<ConversationListResult>('/api/memory/conversations');
    return result.conversationIds ?? [];
  },
  detail: (conversationId: string) => apiGet<ConversationSnapshot>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`),
  remove: (conversationId: string) => apiSend<void>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, 'DELETE')
};
