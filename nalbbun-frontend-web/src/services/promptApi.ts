import { apiGet, apiSend } from './apiClient';
import type { PromptEntry, PromptEntryHistory, PromptSummary } from '../types/api';

export const promptApi = {
  listEntries: (category?: string) => apiGet<PromptEntry[]>(category ? `/api/prompt-entries?category=${encodeURIComponent(category)}` : '/api/prompt-entries'),
  summary: () => apiGet<PromptSummary>('/api/prompt-entries/summary'),
  create: (payload: Record<string, unknown>) => apiSend<PromptEntry>('/api/prompt-entries', 'POST', payload),
  update: (id: string, payload: Record<string, unknown>) => apiSend<PromptEntry>(`/api/prompt-entries/${encodeURIComponent(id)}`, 'PUT', payload),
  remove: (id: string) => apiSend<void>(`/api/prompt-entries/${encodeURIComponent(id)}`, 'DELETE'),
  setDefault: (id: string) => apiSend<PromptEntry>(`/api/prompt-entries/${encodeURIComponent(id)}/default`, 'POST'),
  history: (id: string) => apiGet<PromptEntryHistory[]>(`/api/prompt-entries/${encodeURIComponent(id)}/history`),
  rollback: (id: string, historyId: number) => apiSend<PromptEntry>(`/api/prompt-entries/${encodeURIComponent(id)}/rollback/${historyId}`, 'POST'),
  seed: () => apiSend<{ seeded: number; total: number }>('/api/prompt-entries/seed', 'POST')
};
