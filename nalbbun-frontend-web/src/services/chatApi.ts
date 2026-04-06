import { buildSseUrl } from './apiClient';
import type { ExecutionMode } from '../types/api';

export type ChatCategory = 'GENERAL' | 'DEV' | 'MICE' | 'TRAVEL';

export function buildChatStreamUrl(params: {
  message: string;
  conversationId?: string;
  category?: ChatCategory;
  executionMode?: ExecutionMode;
  promptId?: string;
}) {
  return buildSseUrl('/api/chat/stream', {
    message: params.message,
    conversationId: params.conversationId || '',
    category: params.category || 'GENERAL',
    executionMode: params.executionMode || 'AUTO',
    promptId: params.promptId || ''
  });
}
