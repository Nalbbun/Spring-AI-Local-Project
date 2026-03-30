import { apiGet, apiPostForm, apiSend } from './apiClient';

export const settingsApi = {
  getConfig: () => apiGet<any>('/debug/api/config'),
  resetConfig: () => apiSend<any>('/debug/api/config/reset', 'POST'),
  getOllamaConfig: () => apiGet<any>('/debug/api/ollama/config'),
  saveOllamaConfig: (payload: any) => apiSend<any>('/debug/api/ollama/config', 'POST', payload),
  resetOllamaConfig: () => apiSend<any>('/debug/api/ollama/config/reset', 'POST'),
  checkConnection: () => apiGet<any>('/debug/api/ollama/connection'),
  resetConnection: () => apiSend<any>('/debug/api/ollama/connection/reset', 'POST'),
  browseModels: (source = 'RUNNING') => apiGet<any[]>(`/debug/api/ollama/models?source=${encodeURIComponent(source)}`),
  modelAction: (payload: any) => apiSend<any>('/debug/api/ollama/models/action', 'POST', payload),
  getModelPriority: () => apiGet<any>('/api/model-priority'),
  saveModelPriority: (payload: any) => apiSend<any>('/api/model-priority', 'POST', payload),
  resetModelPriority: () => apiSend<any>('/api/model-priority/reset', 'POST'),
  getRagStatus: () => apiGet<any>('/debug/api/rag/status'),
  getRagDbInfo: () => apiGet<any>('/debug/api/rag/db-info'),
  getRagConfig: () => apiGet<any>('/debug/api/rag/config'),
  saveRagConfig: (payload: any) => apiSend<any>('/debug/api/rag/config', 'POST', payload)
};

export const ragApi = {
  getStatus: () => apiGet<any>('/debug/api/rag/status'),
  getDbInfo: () => apiGet<any>('/debug/api/rag/db-info'),
  getSources: (category = '') => apiGet<any[]>(`/debug/api/rag/sources?category=${encodeURIComponent(category)}`),
  ingestText: (payload: any) => apiSend<any>('/debug/api/rag/ingest-text', 'POST', payload),
  ingestUrl: (payload: any) => apiSend<any>('/debug/api/rag/ingest-url', 'POST', payload),
  ingestFile: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-file', form),
  ingestFiles: (form: FormData) => apiPostForm<any>('/debug/api/rag/ingest-files', form),
  purgeSource: (payload: any) => apiSend<any>('/debug/api/rag/source/purge', 'POST', payload),
  reindexSource: (payload: any) => apiSend<any>('/debug/api/rag/source/reindex', 'POST', payload),
  getEmbeddingConfig: () => apiGet<any>('/debug/api/rag/embedding/config'),
  saveEmbeddingConfig: (payload: any) => apiSend<any>('/debug/api/rag/embedding/config', 'POST', payload),
  resetEmbeddingConfig: () => apiSend<any>('/debug/api/rag/embedding/config/reset', 'POST'),
  getEmbeddingModels: () => apiGet<any[]>('/debug/api/rag/embedding/models')
};

export const agentApi = {
  run: (payload: any) => apiSend<any>('/api/agent/run', 'POST', payload),
  clearMemory: () => apiSend<any>('/debug/api/memory/clear', 'POST'),
  webSearch: (payload: any) => apiSend<any>('/api/search/web', 'POST', payload),
  getAgentConfig: () => apiGet<any>('/debug/api/config'),
  getOllamaConfig: () => apiGet<any>('/debug/api/ollama/config')
};
