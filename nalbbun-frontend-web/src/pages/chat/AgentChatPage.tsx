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
      <ChatWorkspace title="에이전트 채팅" description="chat-agent 화면의 SSE 채팅 React로 구성." defaultCategory="TRAVEL" defaultMessage="부산 2박 3일 여행 코스를 짜줘" />
    </div>
  );
}
