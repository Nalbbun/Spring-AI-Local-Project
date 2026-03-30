import { ChatWorkspace } from '../../components/chat/ChatWorkspace';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { useAsyncData } from '../../hooks/useAsyncData';
import { settingsApi } from '../../services/settingsApi';

export function RagChatPage() {
  const ragStatus = useAsyncData(() => settingsApi.getRagStatus());
  const dbInfo = useAsyncData(() => settingsApi.getRagDbInfo());

  return (
    <div className="page-stack">
      <ChatWorkspace title="RAG 채팅" description="chat-rag 화면의 스트리밍 채팅과 RAG 상태 확인 기능을 분리한 화면" defaultCategory="DEV" defaultMessage="현재 업로드된 문서를 기준으로 시스템 구조를 요약해줘" />
      <div className="two-column-grid">
        <AppCard title="RAG 상태" actions={<button className="secondary" onClick={() => ragStatus.refresh().catch(() => undefined)}>새로고침</button>}><JsonBlock value={ragStatus.data} /></AppCard>
        <AppCard title="RAG DB 정보" actions={<button className="secondary" onClick={() => dbInfo.refresh().catch(() => undefined)}>새로고침</button>}><JsonBlock value={dbInfo.data} /></AppCard>
      </div>
    </div>
  );
}
