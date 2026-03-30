import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AppCard } from '../../components/ui/AppCard';
import { conversationApi } from '../../services/conversationApi';
import { StatusBadge } from '../../components/ui/StatusBadge';

export function ConversationListPage() {
  const [summary, setSummary] = useState<any>(null);
  const [ids, setIds] = useState<string[]>([]);

  const load = async () => {
    const [summaryData, listData] = await Promise.all([conversationApi.summary(), conversationApi.list()]);
    setSummary(summaryData);
    setIds(listData);
  };

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <div className="page-stack">
      <AppCard title="대화 저장소 요약" actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}>
        <div className="stats-grid">
          <div className="stat-box"><span>Store</span><strong>{summary?.storeType ?? '-'}</strong></div>
          <div className="stat-box"><span>Conversations</span><strong>{summary?.conversationCount ?? 0}</strong></div>
          <div className="stat-box"><span>Status</span><strong><StatusBadge label="RUNNING" tone="success" /></strong></div>
        </div>
      </AppCard>

      <AppCard title="대화 목록">
        <div className="list-stack">
          {ids.map(id => (
            <div key={id} className="list-item-row">
              <Link className="list-item-link" to={`/conversation/${encodeURIComponent(id)}`}>{id}</Link>
              <button className="danger" onClick={() => conversationApi.remove(id).then(() => load()).catch(() => undefined)}>삭제</button>
            </div>
          ))}
          {!ids.length && <div className="empty-box">저장된 대화가 없습니다.</div>}
        </div>
      </AppCard>
    </div>
  );
}
