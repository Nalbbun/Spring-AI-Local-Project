import { apiGet, apiSend } from './apiClient';
import type {
  ConversationListItem,
  ConversationListResult,
  ConversationSnapshot,
  MemorySnapshotRecord,
  MemorySummary
} from '../types/api';

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
  remove: (conversationId: string) => apiSend<void>(`/api/memory/conversations/${encodeURIComponent(conversationId)}`, 'DELETE'),
  listSnapshots: (conversationId: string) => apiGet<MemorySnapshotRecord[]>(`/api/memory/conversations/${encodeURIComponent(conversationId)}/snapshots`),
  createSnapshot: (conversationId: string, label?: string) => apiSend<MemorySnapshotRecord>(`/api/memory/conversations/${encodeURIComponent(conversationId)}/snapshots`, 'POST', { label }),
  restoreSnapshot: (conversationId: string, snapshotId: number) => apiSend<ConversationSnapshot>(`/api/memory/conversations/${encodeURIComponent(conversationId)}/snapshots/${snapshotId}/restore`, 'POST'),
  deleteSnapshot: (conversationId: string, snapshotId: number) => apiSend<void>(`/api/memory/conversations/${encodeURIComponent(conversationId)}/snapshots/${snapshotId}`, 'DELETE')
};
