import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { conversationApi } from '../../services/conversationApi';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { JsonBlock } from '../../components/ui/JsonBlock';
import type { ConversationListItem, ConversationSnapshot } from '../../types/api';

type CategoryKey = 'ALL' | 'GENERAL' | 'DEV' | 'MICE' | 'TRAVEL_SEARCH' | 'TRAVEL_PLAN';

const CATEGORY_ORDER: CategoryKey[] = ['ALL', 'GENERAL', 'DEV', 'MICE', 'TRAVEL_SEARCH', 'TRAVEL_PLAN'];

function extractCategories(detail?: ConversationSnapshot | null): string[] {
  const found = new Set<string>();

  const summaryKeys = Object.keys(detail?.categorySummaries ?? {}).filter(Boolean);
  summaryKeys.forEach((value) => found.add(value));

  (detail?.importantNotes ?? []).forEach((note) => {
    if (note?.category) found.add(note.category);
  });

  (detail?.recentMessages ?? []).forEach((message) => {
    if (message?.category) found.add(message.category);
  });

  return Array.from(found);
}

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

export function ConversationListPage() {
  const [summary, setSummary] = useState<any>(null);
  const [items, setItems] = useState<ConversationListItem[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [detail, setDetail] = useState<ConversationSnapshot | null>(null);
  const [detailStatus, setDetailStatus] = useState('대화를 선택하면 우측에 상세가 표시됩니다.');
  const [activeCategory, setActiveCategory] = useState<CategoryKey>('ALL');
  const [loadingList, setLoadingList] = useState(false);

  const load = async () => {
    setLoadingList(true);
    try {
      const [summaryData, conversations] = await Promise.all([conversationApi.summary(), conversationApi.list()]);
      setSummary(summaryData);
      setItems(conversations);

      if (!conversations.length) {
        setSelectedId('');
        setDetail(null);
        setDetailStatus('저장된 대화가 없습니다.');
        return;
      }

      const nextId = selectedId && conversations.some((item) => item.conversationId === selectedId)
        ? selectedId
        : conversations[0].conversationId;
      setSelectedId(nextId);
    } finally {
      setLoadingList(false);
    }
  };

  useEffect(() => { load().catch(() => undefined); }, []);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }

    setDetailStatus('대화 상세 조회 중');
    conversationApi.detail(selectedId)
      .then((value) => {
        setDetail(value);
        setDetailStatus('대화 상세 조회 완료');
      })
      .catch((error) => {
        setDetail(null);
        setDetailStatus(error instanceof Error ? error.message : String(error));
      });
  }, [selectedId]);

  const filteredItems = useMemo(() => {
    if (activeCategory === 'ALL') return items;
    return items.filter((item) => item.categories?.includes(activeCategory));
  }, [activeCategory, items]);

  useEffect(() => {
    if (!filteredItems.length) {
      setSelectedId('');
      setDetail(null);
      if (items.length) {
        setDetailStatus('선택한 카테고리에 해당하는 대화가 없습니다.');
      }
      return;
    }

    if (!filteredItems.some((item) => item.conversationId === selectedId)) {
      setSelectedId(filteredItems[0].conversationId);
    }
  }, [filteredItems, items.length, selectedId]);

  const handleRemove = async (conversationId: string) => {
    await conversationApi.remove(conversationId);
    if (selectedId === conversationId) {
      setSelectedId('');
      setDetail(null);
      setDetailStatus('삭제된 대화입니다.');
    }
    await load();
  };

  const categoryCounts = useMemo(() => {
    const counts = Object.fromEntries(CATEGORY_ORDER.map((category) => [category, 0])) as Record<CategoryKey, number>;
    counts.ALL = items.length;
    items.forEach((item) => {
      (item.categories ?? []).forEach((category) => {
        if (category in counts) {
          counts[category as CategoryKey] += 1;
        }
      });
    });
    return counts;
  }, [items]);

  const longestIdLength = useMemo(() => {
    const source = filteredItems.length ? filteredItems : items;
    return Math.max(28, ...source.map((item) => item.conversationId.length));
  }, [filteredItems, items]);

  const leftPanelWidth = useMemo(() => {
    const estimated = longestIdLength * 8 + 170;
    return Math.max(420, Math.min(estimated, 760));
  }, [longestIdLength]);

  return (
    <div className="page-stack">
      <AppCard title="대화 저장소 요약" actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}>
        <div className="stats-grid">
          <div className="stat-box"><span>Store</span><strong>{summary?.storeType ?? '-'}</strong></div>
          <div className="stat-box"><span>Conversations</span><strong>{summary?.conversationCount ?? 0}</strong></div>
          <div className="stat-box"><span>Status</span><strong><StatusBadge label={loadingList ? 'LOADING' : 'RUNNING'} tone={loadingList ? 'warning' : 'success'} /></strong></div>
        </div>
      </AppCard>

      <AppCard title="카테고리 필터" description="백엔드 대화목록 API가 conversationId, categories, lastUpdated, messageCount를 직접 내려줍니다. 목록은 해당 메타데이터 기준으로 즉시 필터됩니다.">
        <div className="toolbar wrap-row">
          {CATEGORY_ORDER.map((category) => (
            <button
              key={category}
              type="button"
              className={activeCategory === category ? '' : 'secondary'}
              onClick={() => setActiveCategory(category)}
            >
              {category} ({categoryCounts[category] ?? 0})
            </button>
          ))}
        </div>
      </AppCard>

      <div className="conversation-split-layout" style={{ gridTemplateColumns: `${leftPanelWidth}px minmax(0, 1fr)` }}>
        <AppCard title="대화 목록" description="좌측은 conversationId 전체 길이를 최대한 확보하고, 우측 상세는 남는 화면을 모두 사용합니다.">
          <div className="list-stack conversation-list-stack">
            {filteredItems.map((item) => (
              <div key={item.conversationId} className={`conversation-list-item ${selectedId === item.conversationId ? 'selected-row' : ''}`}>
                <div className="conversation-list-header">
                  <button className="link-button conversation-id-button" onClick={() => setSelectedId(item.conversationId)} title={item.conversationId}>{item.conversationId}</button>
                  <button className="danger conversation-delete-button" onClick={() => handleRemove(item.conversationId).catch(() => undefined)}>삭제</button>
                </div>
                <div className="conversation-meta-row">
                  <span>메시지 {item.messageCount ?? 0}</span>
                  <span>{formatDate(item.lastUpdated)}</span>
                </div>
                <div className="conversation-category-row">
                  {item.categories?.length ? item.categories.map((category) => (
                    <StatusBadge key={`${item.conversationId}-${category}`} label={category} tone="info" />
                  )) : <span className="muted small-text">카테고리 정보 없음</span>}
                </div>
              </div>
            ))}
            {!filteredItems.length && <div className="empty-box">선택한 조건에 맞는 대화가 없습니다.</div>}
          </div>
        </AppCard>

        <AppCard title="대화 상세" description={selectedId || '선택된 대화 없음'}>
          <div className="status-line">{detailStatus}</div>
          <div className="stats-grid compact-four top-gap">
            <div className="stat-box"><span>Conversation ID</span><strong className="conversation-id-strong">{detail?.conversationId || '-'}</strong></div>
            <div className="stat-box"><span>Categories</span><strong>{extractCategories(detail).join(', ') || '-'}</strong></div>
            <div className="stat-box"><span>Messages</span><strong>{detail?.recentMessages?.length ?? 0}</strong></div>
            <div className="stat-box"><span>Notes</span><strong>{detail?.importantNotes?.length ?? 0}</strong></div>
          </div>
          <div className="two-column-grid top-gap conversation-detail-grid">
            <AppCard title="최근 메시지"><JsonBlock value={detail?.recentMessages ?? []} /></AppCard>
            <AppCard title="카테고리별 요약 / 노트"><JsonBlock value={{ summaries: detail?.categorySummaries ?? {}, notes: detail?.importantNotes ?? [] }} /></AppCard>
          </div>
          <div className="top-gap">
            <AppCard title="원본 JSON"><JsonBlock value={detail} /></AppCard>
          </div>
        </AppCard>
      </div>
    </div>
  );
}
