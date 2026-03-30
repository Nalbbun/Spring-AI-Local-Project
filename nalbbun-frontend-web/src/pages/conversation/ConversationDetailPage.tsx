import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AppCard } from '../../components/ui/AppCard';
import { conversationApi } from '../../services/conversationApi';
import { JsonBlock } from '../../components/ui/JsonBlock';

export function ConversationDetailPage() {
  const { conversationId = '' } = useParams();
  const [detail, setDetail] = useState<any>(null);

  useEffect(() => {
    if (conversationId) {
      conversationApi.detail(decodeURIComponent(conversationId)).then(setDetail).catch(() => setDetail(null));
    }
  }, [conversationId]);

  return (
    <div className="page-stack">
      <AppCard title="대화 상세" description={decodeURIComponent(conversationId || '')}><JsonBlock value={detail} /></AppCard>
      <div className="two-column-grid">
        <AppCard title="최근 메시지"><JsonBlock value={detail?.recentMessages ?? []} /></AppCard>
        <AppCard title="요약 / 노트"><JsonBlock value={{ summaries: detail?.categorySummaries ?? {}, notes: detail?.importantNotes ?? [] }} /></AppCard>
      </div>
    </div>
  );
}
