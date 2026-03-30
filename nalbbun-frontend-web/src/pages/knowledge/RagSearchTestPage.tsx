import { useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { ChatWorkspace } from '../../components/chat/ChatWorkspace';
import { ragApi } from '../../services/settingsApi';

export function RagSearchTestPage() {
  const [embedding, setEmbedding] = useState<any>(null);
  const [models, setModels] = useState<any[]>([]);
  const [status, setStatus] = useState('대기 중');

  const load = async () => {
    try {
      const [cfg, modelList] = await Promise.all([ragApi.getEmbeddingConfig(), ragApi.getEmbeddingModels()]);
      setEmbedding(cfg);
      setModels(modelList);
      setStatus('조회 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <ChatWorkspace title="RAG 검색 테스트" description="실제 검색 결과는 RAG 채팅 스트림과 함께 확인하고, 여기서는 임베딩 설정과 후보 모델을 점검합니다." defaultCategory="DEV" defaultMessage="RAG 검색 테스트를 위해 관련 문서를 찾아 요약해줘" />
      <AppCard title="임베딩 설정" actions={<button className="secondary" onClick={load}>새로고침</button>}>
        <div className="status-line">{status}</div>
        <div className="two-column-grid">
          <JsonBlock value={embedding} />
          <JsonBlock value={models} />
        </div>
      </AppCard>
    </div>
  );
}
