import { buildSseUrl } from './apiClient';

export type ChatCategory = 'GENERAL' | 'DEV' | 'MICE' | 'TRAVEL';

export function buildChatStreamUrl(params: { message: string; conversationId?: string; category?: ChatCategory; promptId?: string }) {
  return buildSseUrl('/api/chat/stream', {
    message: params.message,
    conversationId: params.conversationId || '',
    category: params.category || 'GENERAL',
    promptId: params.promptId || ''
  });
}
