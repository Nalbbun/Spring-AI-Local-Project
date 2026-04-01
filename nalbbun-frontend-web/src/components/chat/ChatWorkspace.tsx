import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent } from 'react';
import { AppCard } from '../ui/AppCard';
import { buildChatStreamUrl, type ChatCategory } from '../../services/chatApi';
import { setCurrentConversationId } from '../../services/apiClient';
import { LogPanel } from '../ui/LogPanel';
import { promptApi } from '../../services/promptApi';
import { conversationApi } from '../../services/conversationApi';
import { settingsApi } from '../../services/settingsApi';
import type { ConversationListItem } from '../../types/api';

interface StreamEventLine {
  type: string;
  raw: string;
  parsed?: any;
  display: string;
}

interface PromptOption {
  id: string;
  name: string;
  category?: string | null;
  isDefault?: boolean;
}

interface AgentStepView {
  name: string;
  status: string;
  description: string;
}

interface RagStepView {
  name: string;
  status: string;
  message: string;
}

interface RagDiagnosticsView {
  enabled: boolean;
  applied: boolean;
  reason: string;
  traceMessage: string;
  candidateCount: number;
  hitCount: number;
  retrievalElapsedMs: number;
  filterExpression: string;
  similarityThreshold: number;
  topK: number;
  rerankApplied: boolean;
  retrievalMode: string;
  steps: RagStepView[];
  documents: Array<{ title?: string; source?: string; version?: string; score?: number; preview?: string }>;
}

interface ConversationOption {
  value: string;
  label: string;
  source: 'settings' | 'recent';
  categories?: string[];
}

function parseAgentSteps(events: StreamEventLine[]): AgentStepView[] {
  const steps: AgentStepView[] = [];
  const stepMap = new Map<string, AgentStepView>();

  for (const event of events) {
    if (event.type === 'token' || event.type === 'message' || event.type === 'result' || event.type === 'done') {
      continue;
    }

    const parsed = event.parsed;
    const agentName = parsed?.agent || parsed?.name || parsed?.step || (event.type === 'error' ? 'error' : event.type);
    const status = String(parsed?.status || parsed?.state || event.type || 'running');
    const description = String(parsed?.message || parsed?.description || parsed?.content || event.display || event.raw);

    if (!agentName) continue;
    const item = { name: String(agentName), status, description };
    stepMap.set(item.name, item);
  }

  stepMap.forEach((value) => steps.push(value));
  return steps;
}

function normalizeRaw(raw: string): string {
  if (typeof raw !== 'string') return String(raw ?? '');
  const trimmed = raw.trim();
  if (!trimmed || trimmed === 'undefined' || trimmed === 'null') {
    return '';
  }
  return raw;
}

function extractContent(raw: string, parsed: any): string {
  if (typeof parsed === 'string') return parsed;
  const candidate = parsed?.content ?? parsed?.message ?? parsed?.text ?? parsed?.token ?? parsed?.delta ?? normalizeRaw(raw);
  return typeof candidate === 'string' ? candidate : normalizeRaw(raw);
}

function formatEventDisplay(type: string, raw: string, parsed: any): string {
  const normalizedRaw = normalizeRaw(raw);

  if (type === 'token') {
    return extractContent(normalizedRaw, parsed);
  }

  if (parsed && typeof parsed === 'object') {
    if (type === 'agent') {
      const agent = parsed.agent || parsed.name || 'agent';
      const status = parsed.status || parsed.state || 'info';
      const message = parsed.message || parsed.description || normalizedRaw || JSON.stringify(parsed);
      return `[${agent} | ${status}] ${message}`;
    }

    if (type === 'error') {
      const errorMessage = parsed.message || parsed.error || parsed.description || parsed.cause || parsed.detail;
      if (typeof errorMessage === 'string' && errorMessage.trim()) {
        return errorMessage;
      }
      try {
        return JSON.stringify(parsed);
      } catch {
        return '알 수 없는 오류';
      }
    }

    const message = parsed.message || parsed.description || parsed.content;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
  }

  if (type === 'error') {
    return normalizedRaw || '스트리밍 오류가 발생했습니다. 마지막 이벤트와 서버 로그를 확인하세요.';
  }

  return normalizedRaw || raw;
}

function parseLatestRagDiagnostics(events: StreamEventLine[]): RagDiagnosticsView | null {
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const event = events[i];
    if (event.type !== 'rag' || !event.parsed || typeof event.parsed !== 'object') {
      continue;
    }
    const parsed = event.parsed as RagDiagnosticsView;
    return {
      enabled: Boolean(parsed.enabled),
      applied: Boolean(parsed.applied),
      reason: String(parsed.reason || ''),
      traceMessage: String(parsed.traceMessage || ''),
      candidateCount: Number(parsed.candidateCount || 0),
      hitCount: Number(parsed.hitCount || 0),
      retrievalElapsedMs: Number(parsed.retrievalElapsedMs || 0),
      filterExpression: String(parsed.filterExpression || ''),
      similarityThreshold: Number(parsed.similarityThreshold || 0),
      topK: Number(parsed.topK || 0),
      rerankApplied: Boolean(parsed.rerankApplied),
      retrievalMode: String(parsed.retrievalMode || ''),
      steps: Array.isArray(parsed.steps) ? parsed.steps.map((step: any) => ({
        name: String(step?.name || ''),
        status: String(step?.status || ''),
        message: String(step?.message || '')
      })) : [],
      documents: Array.isArray(parsed.documents) ? parsed.documents : []
    };
  }
  return null;
}

function buildEventLines(events: StreamEventLine[]): string[] {
  const lines: string[] = [];
  let tokenBuffer = '';

  const flushTokens = () => {
    if (tokenBuffer) {
      lines.push(`[token-stream] ${tokenBuffer}`);
      tokenBuffer = '';
    }
  };

  for (const event of events) {
    if (event.type === 'token') {
      tokenBuffer += event.display;
      continue;
    }

    flushTokens();
    lines.push(`[${event.type}] ${event.display}`);
  }

  flushTokens();
  return lines;
}

function pickRecentCategoryConversations(items: ConversationListItem[], category: ChatCategory) {
  return [...items]
    .filter((item) => item.categories?.includes(category))
    .sort((a, b) => {
      const aTime = a.lastUpdated ? new Date(a.lastUpdated).getTime() : 0;
      const bTime = b.lastUpdated ? new Date(b.lastUpdated).getTime() : 0;
      return bTime - aTime;
    })
    .slice(0, 10);
}

function buildConversationOptions(category: ChatCategory, items: ConversationListItem[], configConversationId?: string) {
  const options: ConversationOption[] = [];
  const used = new Set<string>();

  const pushOption = (value: string, source: 'settings' | 'recent', categories?: string[]) => {
    const normalized = String(value || '').trim();
    if (!normalized || used.has(normalized)) return;
    used.add(normalized);
    const categoryText = categories?.length ? ` [${categories.join(', ')}]` : '';
    const prefix = source === 'settings' ? '설정 기본 ID' : `${category} 최근 ID`;
    options.push({
      value: normalized,
      label: `${prefix} · ${normalized}${categoryText}`,
      source,
      categories
    });
  };

  pushOption(configConversationId || '', 'settings');
  pickRecentCategoryConversations(items, category).forEach((item) => pushOption(item.conversationId, 'recent', item.categories));
  return options;
}

const SPLIT_STORAGE_KEY = 'nalbbun.chat.split.right.width';
const DEFAULT_RIGHT_WIDTH = 42;
const MIN_RIGHT_WIDTH = 28;
const MAX_RIGHT_WIDTH = 62;

export function ChatWorkspace({
  title,
  description,
  defaultCategory,
  defaultMessage
}: {
  title: string;
  description: string;
  defaultCategory: ChatCategory;
  defaultMessage: string;
}) {
  const [message, setMessage] = useState(defaultMessage);
  const [conversationId, setConversationId] = useState('');
  const [promptId, setPromptId] = useState('');
  const [promptOptions, setPromptOptions] = useState<PromptOption[]>([]);
  const [conversationOptions, setConversationOptions] = useState<ConversationOption[]>([]);
  const [events, setEvents] = useState<StreamEventLine[]>([]);
  const [answer, setAnswer] = useState('');
  const [status, setStatus] = useState('대기 중');
  const [memory, setMemory] = useState<any>(null);
  const [memoryStatus, setMemoryStatus] = useState('조회 전');
  const [configConversationId, setConfigConversationId] = useState('');
  const [rightPaneWidth, setRightPaneWidth] = useState(DEFAULT_RIGHT_WIDTH);

  const eventSourceRef = useRef<EventSource | null>(null);
  const startedAtRef = useRef<number | null>(null);
  const eventCountRef = useRef(0);
  const streamCompletedRef = useRef(false);
  const terminalErrorRef = useRef(false);
  const closedByClientRef = useRef(false);
  const dragStateRef = useRef<{ startX: number; startWidth: number } | null>(null);

  const url = useMemo(
    () => buildChatStreamUrl({ message, conversationId, category: defaultCategory, promptId }),
    [message, conversationId, defaultCategory, promptId]
  );

  const agentSteps = useMemo(() => parseAgentSteps(events), [events]);
  const ragDiagnostics = useMemo(() => parseLatestRagDiagnostics(events), [events]);
  const eventLines = useMemo(() => buildEventLines(events), [events]);

  useEffect(() => {
    setCurrentConversationId(conversationId);
  }, [conversationId]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const saved = Number(window.localStorage.getItem(SPLIT_STORAGE_KEY));
    if (!Number.isNaN(saved) && saved >= MIN_RIGHT_WIDTH && saved <= MAX_RIGHT_WIDTH) {
      setRightPaneWidth(saved);
    }
  }, []);

  const persistSplit = (next: number) => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(SPLIT_STORAGE_KEY, String(next));
    }
  };

  useEffect(() => {
    const handleMove = (event: MouseEvent) => {
      if (!dragStateRef.current || typeof window === 'undefined') return;
      const deltaX = dragStateRef.current.startX - event.clientX;
      const percentDelta = (deltaX / window.innerWidth) * 100;
      const next = Math.min(MAX_RIGHT_WIDTH, Math.max(MIN_RIGHT_WIDTH, dragStateRef.current.startWidth + percentDelta));
      setRightPaneWidth(next);
    };

    const handleUp = () => {
      if (!dragStateRef.current) return;
      dragStateRef.current = null;
      persistSplit(rightPaneWidth);
      document.body.classList.remove('split-dragging');
    };

    window.addEventListener('mousemove', handleMove);
    window.addEventListener('mouseup', handleUp);
    return () => {
      window.removeEventListener('mousemove', handleMove);
      window.removeEventListener('mouseup', handleUp);
    };
  }, [rightPaneWidth]);

  const beginSplitDrag = (event: ReactMouseEvent<HTMLDivElement>) => {
    dragStateRef.current = { startX: event.clientX, startWidth: rightPaneWidth };
    document.body.classList.add('split-dragging');
  };

  const loadConversationTargets = async () => {
    try {
      const [runtimeConfig, list] = await Promise.all([
        settingsApi.getConfig().catch(() => ({ conversationId: '' })),
        conversationApi.list().catch(() => [])
      ]);

      const configuredId = String(runtimeConfig?.conversationId || '').trim();
      setConfigConversationId(configuredId);

      const options = buildConversationOptions(defaultCategory, list, configuredId);
      setConversationOptions(options);

      setConversationId((prev) => {
        if (prev && options.some((item) => item.value === prev)) {
          return prev;
        }
        if (configuredId) {
          return configuredId;
        }
        return options[0]?.value || '';
      });
    } catch {
      setConversationOptions([]);
    }
  };

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        closedByClientRef.current = true;
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    promptApi.listEntries(defaultCategory)
      .then((items) => {
        const active = items.filter((item) => item.active !== false);
        setPromptOptions(active as PromptOption[]);
        const defaultPrompt = active.find((item) => item.isDefault);
        if (defaultPrompt) {
          setPromptId(defaultPrompt.id);
        }
      })
      .catch(() => setPromptOptions([]));

    loadConversationTargets().catch(() => undefined);
  }, [defaultCategory]);

  const appendEvent = (type: string, raw: string) => {
    let parsed: any = undefined;
    const normalizedRaw = typeof raw === 'string' ? raw : String(raw ?? '');
    try {
      parsed = JSON.parse(normalizedRaw);
    } catch {
      parsed = undefined;
    }

    const display = formatEventDisplay(type, normalizedRaw, parsed);
    eventCountRef.current += 1;
    setEvents((prev) => [...prev, { type, raw: normalizedRaw, parsed, display }]);

    const content = extractContent(normalizedRaw, parsed);
    if (content && ['message', 'token', 'result'].includes(type)) {
      setAnswer((prev) => prev + String(content));
    }
  };

  const start = () => {
    if (!message.trim()) {
      setStatus('질문을 입력하세요.');
      return;
    }

    if (eventSourceRef.current) {
      closedByClientRef.current = true;
      eventSourceRef.current.close();
    }

    setEvents([]);
    eventCountRef.current = 0;
    setAnswer('');
    setStatus('연결 중');
    startedAtRef.current = Date.now();
    streamCompletedRef.current = false;
    terminalErrorRef.current = false;
    closedByClientRef.current = false;

    const es = new EventSource(url, { withCredentials: true });
    eventSourceRef.current = es;

    const safeClose = () => {
      if (eventSourceRef.current === es) {
        closedByClientRef.current = true;
        es.close();
        eventSourceRef.current = null;
      }
    };

    const markCompleted = () => {
      streamCompletedRef.current = true;
      const elapsed = startedAtRef.current ? `${((Date.now() - startedAtRef.current) / 1000).toFixed(1)}s` : '-';
      setStatus(`완료 (${elapsed})`);
      loadConversationTargets().catch(() => undefined);
      safeClose();
    };

    es.onopen = () => setStatus('스트리밍 중');
    es.onmessage = (event) => appendEvent('message', event.data);

    ['resolver', 'status', 'token', 'result', 'done', 'complete', 'error', 'agent', 'rag'].forEach((type) => {
      es.addEventListener(type, (event) => {
        const data = (event as MessageEvent).data;

        if (type === 'complete' || type === 'done') {
          if (data && String(data).trim()) {
            appendEvent(type, data);
          }
          markCompleted();
          return;
        }

        appendEvent(type, data);

        if (type === 'error') {
          terminalErrorRef.current = true;
          setStatus('오류');
          safeClose();
        }
      });
    });

    es.onerror = (event) => {
      if (streamCompletedRef.current || terminalErrorRef.current || closedByClientRef.current) {
        safeClose();
        return;
      }

      const messageEvent = event as MessageEvent;
      const raw = typeof messageEvent?.data === 'string' ? messageEvent.data : '';

      if (raw && raw.trim() && raw.trim() !== 'undefined') {
        terminalErrorRef.current = true;
        appendEvent('error', raw);
        setStatus('오류');
      } else if (eventCountRef.current === 0) {
        terminalErrorRef.current = true;
        appendEvent('error', '스트리밍 연결이 예기치 않게 종료되었습니다. 서버 SSE 응답 또는 네트워크 상태를 확인하세요.');
        setStatus('연결 오류');
      } else {
        setStatus('연결 종료');
      }

      safeClose();
    };
  };

  const stop = () => {
    if (eventSourceRef.current) {
      closedByClientRef.current = true;
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    setStatus('중지됨');
  };

  const loadMemory = async () => {
    if (!conversationId.trim()) {
      setMemoryStatus('Conversation ID를 선택하거나 한 번 대화한 뒤 조회하세요.');
      return;
    }
    setMemoryStatus('메모리 조회 중');
    try {
      const detail = await conversationApi.detail(conversationId.trim());
      setMemory(detail);
      setMemoryStatus('메모리 조회 완료');
    } catch (error) {
      setMemoryStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const clearMemory = async () => {
    setMemoryStatus('전체 대화 메모리 초기화 요청 중');
    try {
      await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}/debug/api/memory/clear`, { method: 'POST' });
      setMemory(null);
      setMemoryStatus('대화 메모리 초기화 완료');
      loadConversationTargets().catch(() => undefined);
    } catch (error) {
      setMemoryStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack chat-workspace-page">
      <AppCard title={title} description={description} actions={<span className="status-badge info">{status}</span>}>
        <div className="form-grid two">
          <label className="field-label">
            질문
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={6} />
          </label>

          <div className="form-grid two-inner">
            <label className="field-label">
              Conversation ID / 대화 저장 대상
              <select value={conversationId} onChange={(e) => setConversationId(e.target.value)}>
                <option value="">새 Conversation ID 자동 생성</option>
                {conversationOptions.map((item) => (
                  <option key={`${item.source}-${item.value}`} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
              <div className="muted small-text top-gap">
                최상단에는 설정 화면의 기본 Conversation ID를 두고, 아래에는 {defaultCategory} 카테고리 최근 대화를 표시합니다.
              </div>
            </label>

            <label className="field-label">
              Prompt ID / 프롬프트 선택
              <select value={promptId} onChange={(e) => setPromptId(e.target.value)}>
                <option value="">기본 프롬프트</option>
                {promptOptions.map((item) => (
                  <option key={item.id} value={item.id}>
                    {`${item.isDefault ? '⭐ ' : ''}${item.name}${item.category ? ` [${item.category}]` : ''}`}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>

        <div className="list-item-row top-gap">
          <span>현재 설정 기본 ID</span>
          <span className="inline-mini-code">{configConversationId || '(설정 없음)'}</span>
        </div>

        <div className="button-row">
          <button onClick={start}>스트림 시작</button>
          <button className="secondary" onClick={stop}>중지</button>
          <button className="secondary" onClick={() => { setEvents([]); setAnswer(''); }}>화면 지우기</button>
          <button className="secondary" onClick={loadMemory}>현재 메모리 조회</button>
          <button className="secondary" onClick={() => loadConversationTargets().catch(() => undefined)}>대화 ID 새로고침</button>
          <button className="danger" onClick={clearMemory}>대화 메모리 초기화</button>
        </div>

        <div className="inline-code">{url}</div>
      </AppCard>

      <div className="chat-dashboard-layout" style={{ ['--chat-right-width' as any]: `${rightPaneWidth}%` }}>
        <section className="chat-response-column">
          <AppCard title="채팅 정보 상단 / 최종 응답" description="최종 응답과 이벤트 로그를 한 흐름으로 확인합니다.">
            <div className="result-panel chat-answer-panel">{answer || '응답 대기 중'}</div>
          </AppCard>
          <AppCard title="이벤트 로그" description="최종 응답 생성 과정에서 수집된 SSE 이벤트 로그">
            <LogPanel lines={eventLines} />
          </AppCard>
        </section>

        <div className="chat-splitter" onMouseDown={beginSplitDrag} role="separator" aria-orientation="vertical" aria-label="채팅 화면 분할 조절" />

        <section className="chat-side-column">
          {agentSteps.length > 0 && (
            <AppCard title="진행 단계" description="에이전트/스트리밍 이벤트를 단계형으로 요약합니다.">
              <div className="agent-step-stack">
                {agentSteps.map((step) => (
                  <div
                    key={step.name}
                    className={`agent-step-card ${
                      step.status.includes('error')
                        ? 'error'
                        : step.status.includes('done') || step.status.includes('complete')
                          ? 'complete'
                          : 'running'
                    }`}
                  >
                    <div className="agent-step-title-row">
                      <strong>{step.name}</strong>
                      <span className="status-badge">{step.status}</span>
                    </div>
                    <div className="muted small-text">{step.description}</div>
                  </div>
                ))}
              </div>
            </AppCard>
          )}

          {ragDiagnostics && (
            <AppCard title="RAG 단계 상태" description="검색 후보 수집, 재정렬, 프롬프트 반영 여부를 단계별로 표시합니다.">
              <div className="list-item-row">
                <span>상태</span>
                <span className={`status-badge ${ragDiagnostics.applied ? 'success' : ragDiagnostics.enabled ? 'warning' : 'danger'}`}>
                  {ragDiagnostics.applied ? '적용됨' : ragDiagnostics.enabled ? '검색 결과 없음' : '비활성'}
                </span>
              </div>
              <div className="form-grid two top-gap">
                <div className="json-block">{JSON.stringify({
                  reason: ragDiagnostics.reason,
                  candidateCount: ragDiagnostics.candidateCount,
                  hitCount: ragDiagnostics.hitCount,
                  retrievalElapsedMs: ragDiagnostics.retrievalElapsedMs,
                  topK: ragDiagnostics.topK,
                  similarityThreshold: ragDiagnostics.similarityThreshold,
                  rerankApplied: ragDiagnostics.rerankApplied,
                  retrievalMode: ragDiagnostics.retrievalMode
                }, null, 2)}</div>
                <div className="json-block">{ragDiagnostics.filterExpression || 'filterExpression 없음'}</div>
              </div>
              {ragDiagnostics.steps.length > 0 && (
                <div className="agent-step-stack top-gap">
                  {ragDiagnostics.steps.map((step, index) => (
                    <div
                      key={`${step.name}-${index}`}
                      className={`agent-step-card ${
                        step.status.includes('disabled') || step.status.includes('empty') || step.status.includes('skipped')
                          ? 'error'
                          : step.status.includes('ok') || step.status.includes('applied') || step.status.includes('reranked')
                            ? 'complete'
                            : 'running'
                      }`}
                    >
                      <div className="agent-step-title-row">
                        <strong>{step.name}</strong>
                        <span className="status-badge">{step.status}</span>
                      </div>
                      <div className="muted small-text">{step.message}</div>
                    </div>
                  ))}
                </div>
              )}
              <div className="top-gap json-block">{ragDiagnostics.traceMessage}</div>
              <div className="top-gap json-block">{JSON.stringify(ragDiagnostics.documents, null, 2)}</div>
            </AppCard>
          )}

          <AppCard title="현재 대화 메모리" description="conversation 기반 메모리 확인">
            <div className="status-line">{memoryStatus}</div>
            <div className="json-block">{memory ? JSON.stringify(memory, null, 2) : '메모리 데이터가 없습니다.'}</div>
          </AppCard>
        </section>
      </div>
    </div>
  );
}
