import { useCallback } from 'react';
import { ChatWorkspace } from '../../components/chat/ChatWorkspace';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { useAsyncData } from '../../hooks/useAsyncData';
import { settingsApi } from '../../services/settingsApi';

export function RagChatPage() {
  const loadRagStatus = useCallback(() => settingsApi.getRagStatus(), []);
  const loadDbInfo = useCallback(() => settingsApi.getRagDbInfo(), []);

  const ragStatus = useAsyncData(loadRagStatus);
  const dbInfo = useAsyncData(loadDbInfo);

  return (
    <div className="page-stack">
      <ChatWorkspace
        title="RAG 채팅"
        description="채팅과 RAG 상태 조회를 분리하고, 상태 API는 최초 1회 조회 후 수동 새로고침으로만 다시 확인하도록 안정화한 화면"
        defaultCategory="DEV"
        defaultMessage="현재 업로드된 문서를 기준으로 시스템 구조를 요약해줘"
      />
      <div className="two-column-grid">
        <AppCard
          title="RAG 상태"
          actions={<button className="secondary" onClick={() => ragStatus.refresh().catch(() => undefined)} disabled={ragStatus.loading}>새로고침</button>}
        >
          {ragStatus.error ? <div className="error-text">{ragStatus.error}</div> : null}
          <JsonBlock value={ragStatus.data} />
        </AppCard>
        <AppCard
          title="RAG DB 정보"
          actions={<button className="secondary" onClick={() => dbInfo.refresh().catch(() => undefined)} disabled={dbInfo.loading}>새로고침</button>}
        >
          {dbInfo.error ? <div className="error-text">{dbInfo.error}</div> : null}
          <JsonBlock value={dbInfo.data} />
        </AppCard>
      </div>
    </div>
  );
}
