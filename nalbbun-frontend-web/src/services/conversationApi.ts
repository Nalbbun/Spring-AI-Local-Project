import { apiGet, apiSend } from './apiClient';
import type { ConversationListItem, ConversationListResult, ConversationSnapshot, MemorySummary } from '../types/api';

export const conversationApi = {
  summary: () => apiGet<MemorySummary>('/api/memory/summary'),
  listRaw: () => apiGet<ConversationListResult>('/api/memory/conversations'),
  list: async () => {
    const result = await apiGet<ConversationListResult>('/api/memory/conversations');
    if (result.conversations?.length) {
      return result.conversations;
    }
    return (result.conversationIds ?? []).map((conversationId) => ({
      conversationId,
      categories: [],
      lastUpdated: undefined,
      messageCount: 0
    } satisfies ConversationListItem));
  },
  detail: (conversationId: string) => apiGet<ConversationSnapshot>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`),
  remove: (conversationId: string) => apiSend<void>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, 'DELETE')
};
