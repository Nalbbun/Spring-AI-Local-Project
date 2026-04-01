import { apiGet, apiPostForm, apiSend } from './apiClient';
import type {
  DebugApiLlmConnectionInfo,
  DebugApiLlmProviderConfig,
  DebugOllamaConfig,
  DebugOllamaConnectionInfo,
  DebugRuntimeConfig,
  ModelPriorityResponse,
  OllamaModelInfo,
  RagSearchResult,
  RagStatusResponse,
  RuntimeMeta,
  WebSearchStatus
} from '../types/api';

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

  getLlmProvidersStatus: () => apiGet<Record<string, DebugApiLlmConnectionInfo>>('/debug/api/llm/providers/status'),
  getVllmStatus: () => apiGet<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/vllm'),
  saveVllmConfig: (payload: DebugApiLlmProviderConfig) => apiSend<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/vllm', 'POST', payload),
  resetVllmConfig: () => apiSend<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/vllm/reset', 'POST'),
  getOpenAiStatus: () => apiGet<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/openai'),
  saveOpenAiConfig: (payload: DebugApiLlmProviderConfig) => apiSend<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/openai', 'POST', payload),
  resetOpenAiConfig: () => apiSend<DebugApiLlmConnectionInfo>('/debug/api/llm/providers/openai/reset', 'POST'),

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

const buildOptionalQuery = (params: Record<string, string | undefined>) => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    const normalized = String(value ?? '').trim();
    if (normalized) search.set(key, normalized);
  });
  const query = search.toString();
  return query ? `?${query}` : '';
};

export const ragApi = {
  getStatus: () => apiGet<RagStatusResponse>('/debug/api/rag/status'),
  getHealth: () => apiGet<any>('/debug/api/rag/health'),
  getDbInfo: () => apiGet<any>('/debug/api/rag/db-info'),
  getSources: (category: string, source?: string, version?: string) =>
    apiGet<any[]>(`/debug/api/rag/sources${buildOptionalQuery({ category, source, version })}`),
  getSourceFiles: (category: string, source: string, version: string) =>
    apiGet<any[]>(`/debug/api/rag/source/files${buildOptionalQuery({ category, source, version })}`),
  search: (payload: { category: string; query: string; source?: string; version?: string }) =>
    apiGet<RagSearchResult>(`/debug/api/rag/search${buildOptionalQuery(payload)}`),
  ingestText: (payload: any) => apiSend<any>('/debug/api/rag/ingest-text', 'POST', payload),
  ingestUrl: (payload: any) => apiSend<any>('/debug/api/rag/ingest-url', 'POST', payload),
  ingestFile: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-file', form),
  ingestFiles: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-files', form),
  purgeSource: (payload: any) => apiSend<any>('/debug/api/rag/source/purge', 'POST', payload),
  purgeSourceFile: (payload: any) => apiSend<any>('/debug/api/rag/source/file/purge', 'POST', payload),
  reindexSource: (payload: any) => apiSend<any>('/debug/api/rag/source/reindex', 'POST', payload),
  getEmbeddingConfig: () => apiGet<any>('/debug/api/rag/embedding/config'),
  saveEmbeddingConfig: (payload: any) => apiSend<any>('/debug/api/rag/embedding/config', 'POST', payload),
  resetEmbeddingConfig: () => apiSend<any>('/debug/api/rag/embedding/config/reset', 'POST'),
  getEmbeddingModels: async () => {
    const response = await apiGet<{ currentModel?: string; models?: string[] }>('/debug/api/rag/embedding/models');
    return response;
  }
};

export const runtimeApi = {
  getMeta: () => apiGet<RuntimeMeta>('/api/runtime/meta')
};

export const agentApi = {
  run: (payload: any) => apiSend<any>('/api/agent/execute', 'POST', payload),
  clearMemory: () => apiSend<any>('/debug/api/memory/clear', 'POST'),
  getWebSearchStatus: () => apiGet<WebSearchStatus>('/api/agent/web-search-status'),
  webSearch: async (payload: { query: string }) => {
    try {
      return await apiGet<any>(`/api/agent/web-search-test?query=${encodeURIComponent(payload.query)}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message.includes('404')) {
        return apiGet<any>(`/debug/api/search?query=${encodeURIComponent(payload.query)}`);
      }
      throw error;
    }
  },
  getAgentConfig: () => apiGet<DebugRuntimeConfig>('/debug/api/config'),
  getOllamaConfig: () => apiGet<DebugOllamaConfig>('/debug/api/ollama/config')
};
