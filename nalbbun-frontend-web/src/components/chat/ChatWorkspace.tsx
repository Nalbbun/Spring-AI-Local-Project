import { useEffect, useMemo, useRef, useState } from 'react';
import { AppCard } from '../ui/AppCard';
import { buildChatStreamUrl, type ChatCategory } from '../../services/chatApi';
import { LogPanel } from '../ui/LogPanel';
import { promptApi } from '../../services/promptApi';
import { conversationApi } from '../../services/conversationApi';

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

function parseAgentSteps(events: StreamEventLine[]): AgentStepView[] {
  const steps: AgentStepView[] = [];
  const stepMap = new Map<string, AgentStepView>();

  for (const event of events) {
    const parsed = event.parsed;
    const agentName = parsed?.agent || parsed?.name || parsed?.step || event.type;
    const status = String(parsed?.status || parsed?.state || event.type || 'running');
    const description = String(parsed?.message || parsed?.description || parsed?.content || event.display || event.raw);

    if (!agentName) continue;
    const item = { name: String(agentName), status, description };
    stepMap.set(item.name, item);
  }

  stepMap.forEach((value) => steps.push(value));
  return steps;
}

function extractContent(raw: string, parsed: any): string {
  if (typeof parsed === 'string') return parsed;
  const candidate = parsed?.content ?? parsed?.message ?? parsed?.text ?? parsed?.token ?? parsed?.delta ?? raw;
  return typeof candidate === 'string' ? candidate : raw;
}

function formatEventDisplay(type: string, raw: string, parsed: any): string {
  if (type === 'token') {
    return extractContent(raw, parsed);
  }

  if (parsed && typeof parsed === 'object') {
    if (type === 'agent') {
      const agent = parsed.agent || parsed.name || 'agent';
      const status = parsed.status || parsed.state || 'info';
      const message = parsed.message || parsed.description || raw;
      return `[${agent} | ${status}] ${message}`;
    }

    const message = parsed.message || parsed.description || parsed.content;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
  }

  return raw;
}

function buildEventLines(events: StreamEventLine[]): string[] {
  const lines: string[] = [];
  let tokenBuffer = '';

  const flushTokens = () => {
    if (tokenBuffer) {
      lines.unshift(`[token-stream] ${tokenBuffer}`);
      tokenBuffer = '';
    }
  };

  for (let i = events.length - 1; i >= 0; i -= 1) {
    const event = events[i];
    if (event.type === 'token') {
      tokenBuffer += event.display;
      continue;
    }

    flushTokens();
    lines.unshift(`[${event.type}] ${event.display}`);
  }

  flushTokens();
  return lines;
}

export function ChatWorkspace({ title, description, defaultCategory, defaultMessage }: { title: string; description: string; defaultCategory: ChatCategory; defaultMessage: string }) {
  const [message, setMessage] = useState(defaultMessage);
  const [conversationId, setConversationId] = useState('');
  const [promptId, setPromptId] = useState('');
  const [promptOptions, setPromptOptions] = useState<PromptOption[]>([]);
  const [events, setEvents] = useState<StreamEventLine[]>([]);
  const [answer, setAnswer] = useState('');
  const [status, setStatus] = useState('대기 중');
  const [memory, setMemory] = useState<any>(null);
  const [memoryStatus, setMemoryStatus] = useState('조회 전');
  const eventSourceRef = useRef<EventSource | null>(null);
  const startedAtRef = useRef<number | null>(null);
  const url = useMemo(() => buildChatStreamUrl({ message, conversationId, category: defaultCategory, promptId }), [message, conversationId, defaultCategory, promptId]);
  const agentSteps = useMemo(() => parseAgentSteps(events), [events]);
  const eventLines = useMemo(() => buildEventLines(events), [events]);

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
  }, [defaultCategory]);

  const appendEvent = (type: string, raw: string) => {
    let parsed: any = undefined;
    try {
      parsed = JSON.parse(raw);
    } catch {
      parsed = undefined;
    }

    const display = formatEventDisplay(type, raw, parsed);
    setEvents((prev) => [...prev, { type, raw, parsed, display }]);

    const content = extractContent(raw, parsed);
    if (content && ['message', 'token', 'result'].includes(type)) {
      setAnswer((prev) => prev + String(content));
    }
  };

  const start = () => {
    if (!message.trim()) {
      setStatus('질문을 입력하세요.');
      return;
    }
    if (eventSourceRef.current) eventSourceRef.current.close();
    setEvents([]);
    setAnswer('');
    setStatus('연결 중');
    startedAtRef.current = Date.now();
    const es = new EventSource(url);
    eventSourceRef.current = es;
    es.onopen = () => setStatus('스트리밍 중');
    es.onmessage = (event) => appendEvent('message', event.data);
    ['resolver', 'status', 'token', 'result', 'done', 'error', 'agent'].forEach((type) => {
      es.addEventListener(type, (event) => {
        const data = (event as MessageEvent).data;
        appendEvent(type, data);
        if (type === 'done') {
          const elapsed = startedAtRef.current ? `${((Date.now() - startedAtRef.current) / 1000).toFixed(1)}s` : '-';
          setStatus(`완료 (${elapsed})`);
          es.close();
        }
        if (type === 'error') {
          setStatus('오류');
        }
      });
    });
    es.onerror = () => {
      setStatus((prev) => (prev.startsWith('완료') ? prev : '연결 종료'));
      es.close();
    };
  };

  const stop = () => {
    eventSourceRef.current?.close();
    setStatus('중지됨');
  };

  const loadMemory = async () => {
    if (!conversationId.trim()) {
      setMemoryStatus('Conversation ID를 입력하거나 한 번 대화한 뒤 조회하세요.');
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
    } catch (error) {
      setMemoryStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <AppCard title={title} description={description} actions={<span className="status-badge info">{status}</span>}>
        <div className="form-grid two">
          <label className="field-label">질문<textarea value={message} onChange={(e) => setMessage(e.target.value)} rows={6} /></label>
          <div className="form-grid two-inner">
            <label className="field-label">Conversation ID<input value={conversationId} onChange={(e) => setConversationId(e.target.value)} placeholder="없으면 백엔드가 생성" /></label>
            <label className="field-label">Prompt ID / 프롬프트 선택
              <select value={promptId} onChange={(e) => setPromptId(e.target.value)}>
                <option value="">기본 프롬프트</option>
                {promptOptions.map((item) => (
                  <option key={item.id} value={item.id}>{`${item.isDefault ? '⭐ ' : ''}${item.name}${item.category ? ` [${item.category}]` : ''}`}</option>
                ))}
              </select>
            </label>
          </div>
        </div>
        <div className="button-row">
          <button onClick={start}>스트림 시작</button>
          <button className="secondary" onClick={stop}>중지</button>
          <button className="secondary" onClick={() => { setEvents([]); setAnswer(''); }}>화면 지우기</button>
          <button className="secondary" onClick={loadMemory}>현재 메모리 조회</button>
          <button className="danger" onClick={clearMemory}>대화 메모리 초기화</button>
        </div>
        <div className="inline-code">{url}</div>
      </AppCard>

      {agentSteps.length > 0 && (
        <AppCard title="진행 단계" description="에이전트/스트리밍 이벤트를 단계형으로 요약합니다.">
          <div className="agent-step-stack">
            {agentSteps.map((step) => (
              <div key={step.name} className={`agent-step-card ${step.status.includes('error') ? 'error' : step.status.includes('done') || step.status.includes('complete') ? 'complete' : 'running'}`}>
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

      <div className="two-column-grid">
        <AppCard title="최종 응답"><div className="result-panel">{answer || '응답 대기 중'}</div></AppCard>
        <AppCard title="이벤트 로그"><LogPanel lines={eventLines} /></AppCard>
      </div>

      <AppCard title="현재 대화 메모리" description="레거시 화면처럼 conversation 기반 메모리를 바로 확인할 수 있게 복원했습니다.">
        <div className="status-line">{memoryStatus}</div>
        <div className="json-block">{memory ? JSON.stringify(memory, null, 2) : '메모리 데이터가 없습니다.'}</div>
      </AppCard>
    </div>
  );
}
