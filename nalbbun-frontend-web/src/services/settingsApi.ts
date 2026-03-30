import { apiGet, apiPostForm, apiSend } from './apiClient';
import type { DebugOllamaConfig, DebugOllamaConnectionInfo, DebugRuntimeConfig, ModelPriorityResponse, OllamaModelInfo, RagStatusResponse } from '../types/api';

export const settingsApi = {
  getConfig: () => apiGet<DebugRuntimeConfig>('/debug/api/config'),
  saveConfig: (payload: Partial<DebugRuntimeConfig>) => apiSend<DebugRuntimeConfig>('/debug/api/config', 'POST', payload),
  resetConfig: () => apiSend<DebugRuntimeConfig>('/debug/api/config/reset', 'POST'),

  getOllamaConfig: () => apiGet<DebugOllamaConfig>('/debug/api/ollama/config'),
  saveOllamaConfig: (payload: Partial<DebugOllamaConfig>) => apiSend<DebugOllamaConfig>('/debug/api/ollama/config', 'POST', payload),
  resetOllamaConfig: () => apiSend<DebugOllamaConfig>('/debug/api/ollama/config/reset', 'POST'),

  checkConnection: () => apiGet<DebugOllamaConnectionInfo>('/debug/api/ollama/connection'),
  saveConnection: (payload: { baseUrl?: string; ollamaBaseUrl?: string }) => apiSend<DebugOllamaConnectionInfo>('/debug/api/ollama/connection', 'POST', payload),
  resetConnection: () => apiSend<DebugOllamaConnectionInfo>('/debug/api/ollama/connection/reset', 'POST'),

  browseModels: (source = 'RUNNING') => apiGet<OllamaModelInfo[]>(`/debug/api/ollama/models?source=${encodeURIComponent(source)}`),
  modelAction: (payload: { model: string; pull?: boolean; keepAlive?: string }) => apiSend<any>('/debug/api/ollama/models/action', 'POST', payload),

  getModelPriority: async () => {
    const response = await apiGet<ModelPriorityResponse>('/api/model-priority');
    return response.priorities ?? {};
  },
  saveModelPriority: async (payload: Record<string, string>) => {
    const response = await apiSend<ModelPriorityResponse>('/api/model-priority', 'POST', payload);
    return response.priorities ?? {};
  },
  resetModelPriority: async () => {
    const response = await apiSend<ModelPriorityResponse>('/api/model-priority/reset', 'POST');
    return response.priorities ?? {};
  },

  getRagStatus: () => apiGet<RagStatusResponse>('/debug/api/rag/status'),
  getRagDbInfo: () => apiGet<any>('/debug/api/rag/db-info'),
  getRagConfig: () => apiGet<RagStatusResponse>('/debug/api/rag/status'),
  saveRagConfig: (payload: any) => apiSend<RagStatusResponse>('/debug/api/rag/config', 'POST', payload)
};

export const ragApi = {
  getStatus: () => apiGet<RagStatusResponse>('/debug/api/rag/status'),
  getDbInfo: () => apiGet<any>('/debug/api/rag/db-info'),
  getSources: (category: string) => apiGet<any[]>(`/debug/api/rag/sources?category=${encodeURIComponent(category)}`),
  ingestText: (payload: any) => apiSend<any>('/debug/api/rag/ingest-text', 'POST', payload),
  ingestUrl: (payload: any) => apiSend<any>('/debug/api/rag/ingest-url', 'POST', payload),
  ingestFile: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-file', form),
  ingestFiles: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-files', form),
  purgeSource: (payload: any) => apiSend<any>('/debug/api/rag/source/purge', 'POST', payload),
  reindexSource: (payload: any) => apiSend<any>('/debug/api/rag/source/reindex', 'POST', payload),
  getEmbeddingConfig: () => apiGet<any>('/debug/api/rag/embedding/config'),
  saveEmbeddingConfig: (payload: any) => apiSend<any>('/debug/api/rag/embedding/config', 'POST', payload),
  resetEmbeddingConfig: () => apiSend<any>('/debug/api/rag/embedding/config/reset', 'POST'),
  getEmbeddingModels: async () => {
    const response = await apiGet<{ currentModel?: string; models?: string[] }>('/debug/api/rag/embedding/models');
    return response;
  }
};

export const agentApi = {
  run: (payload: any) => apiSend<any>('/api/agent/execute', 'POST', payload),
  clearMemory: () => apiSend<any>('/debug/api/memory/clear', 'POST'),
  webSearch: (payload: any) => apiSend<any>('/api/search/web', 'POST', payload),
  getAgentConfig: () => apiGet<DebugRuntimeConfig>('/debug/api/config'),
  getOllamaConfig: () => apiGet<DebugOllamaConfig>('/debug/api/ollama/config')
};
