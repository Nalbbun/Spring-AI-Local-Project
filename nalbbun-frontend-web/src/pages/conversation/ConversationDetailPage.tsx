import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { AppCard } from '../../components/ui/AppCard';
import { conversationApi } from '../../services/conversationApi';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { DataTable } from '../../components/ui/DataTable';
import type { MemorySnapshotRecord } from '../../types/api';

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

export function ConversationDetailPage() {
  const { conversationId = '' } = useParams();
  const resolvedConversationId = decodeURIComponent(conversationId || '');
  const [detail, setDetail] = useState<any>(null);
  const [snapshots, setSnapshots] = useState<MemorySnapshotRecord[]>([]);
  const [status, setStatus] = useState('조회 중');

  const load = async () => {
    if (!resolvedConversationId) return;
    const [detailData, snapshotData] = await Promise.all([
      conversationApi.detail(resolvedConversationId),
      conversationApi.listSnapshots(resolvedConversationId).catch(() => [])
    ]);
    setDetail(detailData);
    setSnapshots(snapshotData);
    setStatus('조회 완료');
  };

  useEffect(() => {
    load().catch((error) => {
      setDetail(null);
      setStatus(error instanceof Error ? error.message : String(error));
    });
  }, [resolvedConversationId]);

  return (
    <div className="page-stack">
      <AppCard title="대화 상세" description={resolvedConversationId} actions={<div className="button-row compact"><button className="secondary" onClick={() => conversationApi.createSnapshot(resolvedConversationId, `manual-${new Date().toISOString()}`).then(load)}>스냅샷 생성</button><button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button></div>}>
        <div className="status-line">{status}</div>
        <JsonBlock value={detail} />
      </AppCard>
      <div className="two-column-grid">
        <AppCard title="최근 메시지"><JsonBlock value={detail?.recentMessages ?? []} /></AppCard>
        <AppCard title="요약 / 노트"><JsonBlock value={{ summaries: detail?.categorySummaries ?? {}, notes: detail?.importantNotes ?? [] }} /></AppCard>
      </div>
      <AppCard title="Memory Snapshot" description="현재 대화 상태를 저장하고, 필요 시 해당 시점으로 복원할 수 있습니다.">
        <DataTable rows={snapshots} columns={[
          { key: 'createdAt', title: '생성 시각', render: (row) => formatDate(row.createdAt) },
          { key: 'label', title: '라벨', render: (row) => row.label },
          { key: 'messageCount', title: '메시지 수', render: (row) => row.snapshot?.recentMessages?.length ?? 0 },
          { key: 'actions', title: '작업', render: (row) => <div className="button-row compact"><button className="secondary" onClick={() => conversationApi.restoreSnapshot(resolvedConversationId, row.snapshotId).then(load)}>복원</button><button className="danger" onClick={() => conversationApi.deleteSnapshot(resolvedConversationId, row.snapshotId).then(load)}>삭제</button></div> }
        ]} />
      </AppCard>
    </div>
  );
}
