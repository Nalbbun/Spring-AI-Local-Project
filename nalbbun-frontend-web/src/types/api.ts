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
  createdAt?: string;
}

export interface PromptEntry {
  id: string;
  name: string;
  category?: string | null;
  description?: string;
  systemPrompt: string;
  isDefault?: boolean;
  active?: boolean;
  createdAt?: string;
}

export interface PromptSummary {
  store: string;
  total: number;
  activeCount: number;
}

export interface MemorySummary {
  storeType?: string;
  conversationCount?: number;
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

export interface RagSourceItem {
  sourceId?: string;
  category?: string;
  sourceType?: string;
  title?: string;
  createdAt?: string;
  chunkCount?: number;
}

export interface AgentExecutionEvent {
  type?: string;
  step?: string;
  message?: string;
  payload?: unknown;
}
