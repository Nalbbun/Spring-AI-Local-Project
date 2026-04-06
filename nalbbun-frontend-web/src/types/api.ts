export interface ApiCatalogEndpoint { method: string; path: string; title: string; notes: string; }
export interface ApiCatalogGroup { name: string; endpoints: ApiCatalogEndpoint[]; }
export interface ApiCatalogResponse { application: string; description: string; groups: ApiCatalogGroup[]; }

export interface ProviderStatus {
  provider: string;
  displayName: string;
  description?: string;
  hasActiveKey: boolean;
  maskedKey?: string;
  keyIssueUrl?: string;
}

export interface ApiKeyEntry {
  id: string;
  provider: string;
  label: string;
  description?: string;
  maskedKey?: string;
  active?: boolean;
  versionNo?: number;
  previousVersionId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromptEntry {
  id: string;
  name: string;
  category?: string | null;
  description?: string;
  systemPrompt: string;
  isDefault?: boolean;
  active?: boolean;
  versionNo?: number;
  previousVersionId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromptSummary {
  store: string;
  total: number;
  activeCount: number;
}

export interface MemorySummary {
  storeType?: string;
  conversationCount?: number;
  conversationIds?: string[];
}

export interface ConversationMessage {
  role?: string;
  category?: string;
  content?: string;
  createdAt?: string;
}

export interface ConversationSnapshot {
  conversationId?: string;
  recentMessages?: ConversationMessage[];
  categorySummaries?: Record<string, { summary?: string; updatedAt?: string }>;
  importantNotes?: Array<{ category?: string; note?: string; createdAt?: string }>;
}

export interface ConversationListItem {
  conversationId: string;
  categories: string[];
  lastUpdated?: string;
  messageCount: number;
}

export interface ConversationListResult {
  conversationIds: string[];
  total: number;
  conversations?: ConversationListItem[];
}

export interface RagSourceItem {
  source?: string;
  sourceId?: string;
  category?: string;
  sourceType?: string;
  title?: string;
  version?: string;
  createdAt?: string;
  updatedAt?: string;
  ingestedAt?: string;
  chunkCount?: number;
  fileCount?: number;
}

export interface RagHealthResponse {
  status?: string;
  details?: Record<string, unknown>;
}

export interface RagSourceCommand {
  category: string;
  source: string;
  version?: string;
  targetVersion?: string;
}

export interface AgentExecutionEvent {
  type?: string;
  step?: string;
  message?: string;
  payload?: unknown;
}

export type ExecutionMode = 'AUTO' | 'CHAT' | 'RAG' | 'AGENT';

export interface DebugRuntimeConfig {
  resolverMode?: string;
  generalParserMode?: string;
  travelParserMode?: string;
  devParserMode?: string;
  miceParserMode?: string;
  generalExecutionMode?: ExecutionMode | string;
  devExecutionMode?: ExecutionMode | string;
  miceExecutionMode?: ExecutionMode | string;
  travelExecutionMode?: ExecutionMode | string;
  memoryStore?: string;
  activeMemoryStore?: string;
  requestedMemoryStore?: string;
  memoryServiceType?: string;
  restartRequired?: boolean;
  restartSupported?: boolean;
  memoryStoreNotice?: string;
  availableMemoryStores?: string[];
  restartRequestedAt?: string;
  lastAppliedAt?: string;
  redisSessionTtlMinutes?: number;
  restartAction?: string;
  fallbackPolicy?: string;
  conversationId?: string;
  ollamaBaseUrl?: string;
}

export interface DebugApiLlmProviderConfig {
  baseUrl?: string;
  defaultModel?: string;
  keyProvider?: string;
  healthCheckPath?: string;
  healthCheckMethod?: 'GET' | 'POST' | string;
  modelsPath?: string;
  modelsMethod?: 'GET' | 'POST' | string;
  sllmPath?: string;
  llmPath?: string;
  embeddingPath?: string;
  rerankPath?: string;
  searchModel?: string;
  answerModel?: string;
  embeddingModel?: string;
  rerankModel?: string;
}

export interface DebugApiLlmConnectionInfo {
  provider?: string;
  baseUrl?: string;
  reachable?: boolean;
  status?: string;
  message?: string;
  defaultModel?: string;
  modelCount?: number;
  keyResolved?: boolean;
  keyProvider?: string;
  healthCheckPath?: string;
  healthCheckMethod?: string;
  modelsPath?: string;
  modelsMethod?: string;
  healthCheckOk?: boolean;
  modelsCheckOk?: boolean;
  resolvedHealthUrl?: string;
  resolvedModelsUrl?: string;
  availableModels?: string[];
  sllmPath?: string;
  llmPath?: string;
  embeddingPath?: string;
  rerankPath?: string;
  searchModel?: string;
  answerModel?: string;
  embeddingModel?: string;
  rerankModel?: string;
  resolvedSllmUrl?: string;
  resolvedLlmUrl?: string;
  resolvedEmbeddingUrl?: string;
  resolvedRerankUrl?: string;
  infoModels?: Record<string, string>;
  infoEndpoints?: Record<string, string>;
}

export interface DebugOllamaConfig {
  modelSource?: string;
  generalModel?: string;
  devModel?: string;
  miceModel?: string;
  travelSearchModel?: string;
  travelPlanModel?: string;
  residentModelList?: string;
  residentKeepAlive?: string;
}

export interface DebugOllamaConnectionInfo {
  baseUrl?: string;
  reachable?: boolean;
  status?: string;
  message?: string;
  runningCount?: number;
  installedCount?: number;
  runningModels?: string[];
}

export interface OllamaModelInfo {
  name?: string;
  model?: string;
  state?: string;
  size?: number;
  modifiedAt?: string;
  updatedAt?: string;
}

export interface ModelPriorityItem {
  priority?: string;
  description?: string;
}

export interface ModelPriorityResponse {
  priorities?: Record<string, ModelPriorityItem>;
}

export interface RagStatusResponse {
  enabled?: boolean;
  vectorStore?: string;
  topK?: number;
  similarityThreshold?: number;
  includeCitations?: boolean;
  categories?: Record<string, boolean>;
  registryBaseDir?: string;
  ingest?: {
    chunkSize?: number;
    minChunkSizeChars?: number;
    minChunkLengthToEmbed?: number;
    maxNumChunks?: number;
    maxUploadFileCount?: number;
  };
}

export interface WebSearchStatus {
  provider?: string;
  primaryEndpoint?: string;
  primaryEndpointAvailable?: boolean;
  legacyDebugEndpoint?: string;
  legacyDebugEndpointAvailable?: boolean;
  localProfile?: boolean;
  debugEnabled?: boolean;
  activeProfiles?: string[];
  tavilyRuntimeStatus?: string;
  openAiRuntimeStatus?: string;
  hasTavilyActiveKey?: boolean;
  status?: string;
  message?: string;
}


export interface RuntimeMeta {
  debugEnabled?: boolean;
  adminConsoleEnabled?: boolean;
  crossOriginSessionSupported?: boolean;
  conversationTransport?: string;
  activeProfiles?: string[];
}

export interface RagSearchDocument {
  source?: string;
  version?: string;
  score?: number;
  title?: string;
  text?: string;
}

export interface RagSearchResult {
  applied?: boolean;
  reason?: string;
  documents?: RagSearchDocument[];
}
