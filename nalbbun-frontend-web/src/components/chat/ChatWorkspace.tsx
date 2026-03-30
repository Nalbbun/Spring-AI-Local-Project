import { useMemo, useRef, useState } from 'react';
import { AppCard } from '../ui/AppCard';
import { buildChatStreamUrl, type ChatCategory } from '../../services/chatApi';
import { LogPanel } from '../ui/LogPanel';

interface StreamEventLine {
  type: string;
  raw: string;
  parsed?: any;
}

export function ChatWorkspace({ title, description, defaultCategory, defaultMessage }: { title: string; description: string; defaultCategory: ChatCategory; defaultMessage: string }) {
  const [message, setMessage] = useState(defaultMessage);
  const [conversationId, setConversationId] = useState('');
  const [promptId, setPromptId] = useState('');
  const [events, setEvents] = useState<StreamEventLine[]>([]);
  const [answer, setAnswer] = useState('');
  const [status, setStatus] = useState('대기 중');
  const eventSourceRef = useRef<EventSource | null>(null);
  const url = useMemo(() => buildChatStreamUrl({ message, conversationId, category: defaultCategory, promptId }), [message, conversationId, defaultCategory, promptId]);

  const appendEvent = (type: string, raw: string) => {
    let parsed: any = undefined;
    try { parsed = JSON.parse(raw); } catch { parsed = undefined; }
    setEvents(prev => [...prev, { type, raw, parsed }]);
    if (parsed?.content) {
      setAnswer(prev => prev + parsed.content);
    } else if (typeof parsed === 'string') {
      setAnswer(prev => prev + parsed);
    }
  };

  const start = () => {
    if (eventSourceRef.current) eventSourceRef.current.close();
    setEvents([]);
    setAnswer('');
    setStatus('연결 중');
    const es = new EventSource(url);
    eventSourceRef.current = es;
    es.onopen = () => setStatus('스트리밍 중');
    es.onmessage = (event) => appendEvent('message', event.data);
    ['resolver','status','token','result','done','error'].forEach(type => {
      es.addEventListener(type, (event) => {
        const data = (event as MessageEvent).data;
        appendEvent(type, data);
        if (type === 'done') {
          setStatus('완료');
          es.close();
        }
        if (type === 'error') {
          setStatus('오류');
        }
      });
    });
    es.onerror = () => {
      setStatus('연결 종료');
      es.close();
    };
  };

  const stop = () => {
    eventSourceRef.current?.close();
    setStatus('중지됨');
  };

  return (
    <div className="page-stack">
      <AppCard title={title} description={description} actions={<span className="status-badge info">{status}</span>}>
        <div className="form-grid two">
          <label className="field-label">질문<textarea value={message} onChange={e => setMessage(e.target.value)} rows={5} /></label>
          <div className="form-grid two-inner">
            <label className="field-label">Conversation ID<input value={conversationId} onChange={e => setConversationId(e.target.value)} placeholder="없으면 백엔드가 생성" /></label>
            <label className="field-label">Prompt ID<input value={promptId} onChange={e => setPromptId(e.target.value)} placeholder="선택적으로 사용" /></label>
          </div>
        </div>
        <div className="button-row">
          <button onClick={start}>스트림 시작</button>
          <button className="secondary" onClick={stop}>중지</button>
          <button className="secondary" onClick={() => { setEvents([]); setAnswer(''); }}>화면 지우기</button>
        </div>
        <div className="inline-code">{url}</div>
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="최종 응답"><div className="result-panel">{answer || '응답 대기 중'}</div></AppCard>
        <AppCard title="이벤트 로그"><LogPanel lines={events.map(e => `[${e.type}] ${e.raw}`)} /></AppCard>
      </div>
    </div>
  );
}
