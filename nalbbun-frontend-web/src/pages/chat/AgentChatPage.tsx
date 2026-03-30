import { ChatWorkspace } from '../../components/chat/ChatWorkspace';
import { AppCard } from '../../components/ui/AppCard';
import { useState } from 'react';
import { agentApi } from '../../services/settingsApi';
import { JsonBlock } from '../../components/ui/JsonBlock';

export function AgentChatPage() {
  const [result, setResult] = useState<any>(null);
  const [question, setQuestion] = useState('제주도 3박 4일 여행 일정을 추천해줘');
  const [status, setStatus] = useState('대기 중');

  const runAgent = async () => {
    setStatus('실행 중');
    try {
      const response = await agentApi.run({ category: 'TRAVEL', message: question });
      setResult(response);
      setStatus('완료');
    } catch (error) {
      setResult({ error: error instanceof Error ? error.message : String(error) });
      setStatus('오류');
    }
  };

  return (
    <div className="page-stack">
      <ChatWorkspace title="에이전트 채팅" description="legacy chat-agent 화면의 SSE 채팅과 에이전트 실행 테스트를 React로 재구성했습니다." defaultCategory="TRAVEL" defaultMessage="부산 2박 3일 여행 코스를 짜줘" />
      <AppCard title="에이전트 실행 테스트" actions={<span className="status-badge info">{status}</span>}>
        <label className="field-label">질문<textarea rows={4} value={question} onChange={e => setQuestion(e.target.value)} /></label>
        <div className="button-row">
          <button onClick={runAgent}>에이전트 실행</button>
          <button className="secondary" onClick={() => setResult(null)}>화면 지우기</button>
          <button className="secondary" onClick={() => agentApi.clearMemory().catch(() => undefined)}>대화 초기화</button>
        </div>
        <JsonBlock value={result} />
      </AppCard>
    </div>
  );
}
