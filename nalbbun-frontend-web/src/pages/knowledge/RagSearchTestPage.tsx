import { useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { useEventLog } from '../../hooks/useEventLog';
import { ragApi } from '../../services/settingsApi';
import type { RagSearchDocument, RagSearchResult } from '../../types/api';

const CATEGORY_OPTIONS = ['GENERAL', 'DEV', 'MICE', 'TRAVEL'] as const;

export function RagSearchTestPage() {
  const [category, setCategory] = useState<string>('DEV');
  const [query, setQuery] = useState('RAG 검색 테스트를 위한 문서 분석 및 요약 단계별 가이드');
  const [source, setSource] = useState('');
  const [version, setVersion] = useState('');
  const [status, setStatus] = useState('대기 중');
  const [result, setResult] = useState<RagSearchResult | null>(null);
  const logs = useEventLog('rag-search-test-log', ['RAG 검색 테스트 로그가 누적됩니다.']);

  const search = async () => {
    if (!query.trim()) {
      setStatus('검색어를 입력하세요.');
      return;
    }
    setStatus('검색 중');
    logs.append('RAG 검색 시작', { category, query, source, version });
    try {
      const data = await ragApi.search({ category, query, source, version });
      setResult(data);
      setStatus(`조회 완료 (hits=${data?.documents?.length ?? 0})`);
      logs.append('RAG 검색 완료', `applied=${data?.applied} hits=${data?.documents?.length ?? 0} reason=${data?.reason ?? '-'}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(message);
      logs.append('RAG 검색 실패', message);
    }
  };

  const documents: RagSearchDocument[] = result?.documents ?? [];

  return (
    <div className="page-stack">
      <AppCard title="RAG 검색 테스트" description="실제 /debug/api/rag/search 결과를 바로 확인하는 테스트 화면입니다.">
        <div className="form-grid four">
          <label className="field-label">카테고리
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              {CATEGORY_OPTIONS.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <label className="field-label">Source (선택)
            <input value={source} onChange={(e) => setSource(e.target.value)} placeholder="예: dev-manual" />
          </label>
          <label className="field-label">Version (선택)
            <input value={version} onChange={(e) => setVersion(e.target.value)} placeholder="예: v1" />
          </label>
          <label className="field-label">상태
            <input value={status} readOnly />
          </label>
        </div>
        <label className="field-label top-gap">검색어
          <textarea rows={4} value={query} onChange={(e) => setQuery(e.target.value)} />
        </label>
        <div className="button-row">
          <button onClick={search}>검색 실행</button>
          <button className="secondary" onClick={() => { setResult(null); setStatus('대기 중'); }}>결과 지우기</button>
        </div>
      </AppCard>

      <AppCard title="검색 결과 문서" description={`hits=${documents.length} | applied=${String(result?.applied ?? '-')}`}>
        <DataTable<RagSearchDocument>
          rows={documents}
          columns={[
            { key: 'idx', title: '#', render: (_row, index) => index + 1 },
            { key: 'source', title: 'Source', render: (row) => row.source ?? '-' },
            { key: 'version', title: 'Version', render: (row) => row.version ?? '-' },
            { key: 'score', title: 'Score', render: (row) => row.score != null ? Number(row.score).toFixed(4) : '-' },
            { key: 'title', title: '제목', render: (row) => row.title ?? '-' },
            { key: 'text', title: '본문 미리보기', render: (row) => <div style={{ whiteSpace: 'pre-wrap' }}>{row.text ?? '-'}</div> }
          ]}
          emptyText="검색 결과가 없습니다."
        />
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="검색 결과 JSON"><JsonBlock value={result} /></AppCard>
        <AppCard title="검색 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
          <LogPanel lines={logs.lines} />
        </AppCard>
      </div>
    </div>
  );
}
