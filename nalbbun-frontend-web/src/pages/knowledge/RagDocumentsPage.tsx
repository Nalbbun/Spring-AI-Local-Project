import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { useEventLog } from '../../hooks/useEventLog';
import { ragApi } from '../../services/settingsApi';

const defaultTextForm = { category: 'DEV', sourceId: '', title: '', content: '' };
const defaultUrlForm = { category: 'DEV', sourceId: '', title: '', url: '' };

function hasPdfFallbackFlag(value: any): boolean {
  if (!value) return false;
  if (value === true) return true;
  if (Array.isArray(value)) {
    return value.some((item) => hasPdfFallbackFlag(item));
  }
  if (typeof value === 'object') {
    if (value.pdfFallbackUsed === true) return true;
    if (value.metadata?.pdfFallbackUsed === true) return true;
    return Object.values(value).some((item) => hasPdfFallbackFlag(item));
  }
  return false;
}


export function RagDocumentsPage() {
  const [status, setStatus] = useState<any>(null);
  const [dbInfo, setDbInfo] = useState<any>(null);
  const [sources, setSources] = useState<any[]>([]);
  const [category, setCategory] = useState('DEV');
  const [textForm, setTextForm] = useState(defaultTextForm);
  const [urlForm, setUrlForm] = useState(defaultUrlForm);
  const [file, setFile] = useState<File | null>(null);
  const [multiFiles, setMultiFiles] = useState<FileList | null>(null);
  const [embedding, setEmbedding] = useState<any>(null);
  const [embeddingModels, setEmbeddingModels] = useState<any>(null);
  const [lastResult, setLastResult] = useState<any>(null);
  const [opStatus, setOpStatus] = useState('대기 중');
  const logs = useEventLog('rag-documents-log', ['RAG 관리 작업 로그가 누적됩니다.']);
  const pdfFallbackUsed = useMemo(() => hasPdfFallbackFlag(lastResult), [lastResult]);

  const load = async () => {
    const [statusData, dbData, sourceData, embeddingConfig, modelList] = await Promise.all([
      ragApi.getStatus(),
      ragApi.getDbInfo(),
      ragApi.getSources(category),
      ragApi.getEmbeddingConfig(),
      ragApi.getEmbeddingModels()
    ]);
    setStatus(statusData);
    setDbInfo(dbData);
    setSources(sourceData);
    setEmbedding(embeddingConfig);
    setEmbeddingModels(modelList);
  };

  useEffect(() => {
    load().catch((error) => logs.append('RAG 화면 조회 실패', error instanceof Error ? error.message : String(error)));
  }, [category]);

  const syncSharedFields = (patch: { category?: string; sourceId?: string; title?: string }) => {
    setTextForm((prev) => ({ ...prev, ...patch }));
    setUrlForm((prev) => ({ ...prev, ...patch }));
  };

  const run = async (label: string, action: () => Promise<unknown>) => {
    setOpStatus(`${label} 실행 중`);
    logs.append(`${label} 시작`);
    try {
      const response = await action();
      setLastResult(response);
      setOpStatus(`${label} 완료`);
      logs.append(`${label} 완료`);
      if (hasPdfFallbackFlag(response)) {
        logs.append(`${label} 안내`, 'PDF 레이아웃 추출 실패로 기본 PDF fallback reader가 사용되었습니다.');
      }
      await load();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setLastResult(null);
      setOpStatus(message);
      logs.append(`${label} 실패`, message);
    }
  };

  const uploadSingle = async () => {
    if (!file) return;
    const form = new FormData();
    form.append('file', file);
    form.append('category', textForm.category);
    form.append('sourceId', textForm.sourceId);
    form.append('title', textForm.title);
    await run('단일 파일 업로드', () => ragApi.ingestFile(form));
  };

  const uploadMulti = async () => {
    if (!multiFiles?.length) return;
    const form = new FormData();
    Array.from(multiFiles).forEach((f) => form.append('files', f));
    form.append('category', textForm.category);
    form.append('sourceId', textForm.sourceId);
    form.append('title', textForm.title);
    await run('멀티 파일 업로드', () => ragApi.ingestFiles(form));
  };

  return (
    <div className="page-stack">
      <AppCard title="RAG 운영 현황" description="상태, 저장소, 임베딩, 인입, 소스 목록, 로그를 한 화면으로 다시 배치했습니다.">
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Enabled</span><strong>{String(status?.enabled ?? false)}</strong></div>
          <div className="stat-box"><span>Vector Store</span><strong>{status?.vectorStore ?? '-'}</strong></div>
          <div className="stat-box"><span>TopK</span><strong>{status?.topK ?? '-'}</strong></div>
          <div className="stat-box"><span>Source Count</span><strong>{sources.length}</strong></div>
        </div>
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="RAG 상태"><JsonBlock value={status} /></AppCard>
        <AppCard title="RAG DB 정보"><JsonBlock value={dbInfo} /></AppCard>
      </div>

      <div className="two-column-grid">
        <AppCard title="임베딩 설정 현황" description="레거시 설정에서 빠졌던 임베딩 후보 모델과 현재 설정을 다시 붙였습니다.">
          <JsonBlock value={embedding} />
        </AppCard>
        <AppCard title="임베딩 모델 후보">
          <JsonBlock value={embeddingModels} />
        </AppCard>
      </div>

      <AppCard title="문서 인입" description="텍스트 / URL / 단일 파일 / 멀티 파일 인입을 운영 순서대로 배치했습니다.">
        <div className="form-grid three">
          <label className="field-label">카테고리<select value={textForm.category} onChange={(e) => syncSharedFields({ category: e.target.value })}><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select></label>
          <label className="field-label">Source ID<input value={textForm.sourceId} onChange={(e) => syncSharedFields({ sourceId: e.target.value })} /></label>
          <label className="field-label">Title<input value={textForm.title} onChange={(e) => syncSharedFields({ title: e.target.value })} /></label>
        </div>
        <div className="two-column-grid top-gap">
          <AppCard title="텍스트 인입">
            <label className="field-label">내용<textarea rows={8} value={textForm.content} onChange={(e) => setTextForm((prev) => ({ ...prev, content: e.target.value }))} /></label>
            <div className="button-row"><button onClick={() => run('텍스트 인입', () => ragApi.ingestText(textForm))}>텍스트 업로드</button></div>
          </AppCard>
          <AppCard title="URL 인입">
            <label className="field-label">URL<input value={urlForm.url} onChange={(e) => setUrlForm((prev) => ({ ...prev, url: e.target.value }))} /></label>
            <div className="button-row"><button onClick={() => run('URL 인입', () => ragApi.ingestUrl(urlForm))}>URL 업로드</button></div>
          </AppCard>
        </div>
        <div className="form-grid two top-gap">
          <label className="field-label">단일 파일<input type="file" onChange={(e) => setFile(e.target.files?.[0] || null)} /></label>
          <label className="field-label">멀티 파일<input type="file" multiple onChange={(e) => setMultiFiles(e.target.files)} /></label>
        </div>
        <div className="button-row"><button onClick={uploadSingle}>단일 업로드</button><button className="secondary" onClick={uploadMulti}>멀티 업로드</button></div>
        <div className="status-line">{opStatus}</div>
      </AppCard>

      <AppCard
        title="업로드 결과"
        actions={pdfFallbackUsed ? <span className="status-badge warning">PDF Fallback 사용됨</span> : undefined}
        description="최근 문서 인입 결과와 manifest/files 정보를 확인합니다."
      >
        <div className="status-line">{pdfFallbackUsed ? '일부 PDF는 기본 PDF reader fallback으로 처리되었습니다.' : '최근 업로드 결과를 표시합니다.'}</div>
        <JsonBlock value={lastResult ?? { status: '대기 중', message: '아직 업로드 결과가 없습니다.' }} />
      </AppCard>

      <AppCard title="소스 목록" actions={<div className="toolbar"><select value={category} onChange={(e) => setCategory(e.target.value)}><option value="">전체</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select><button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button></div>}>
        <DataTable rows={sources} columns={[
          { key: 'source', title: 'Source', render: (row) => row.source ?? row.sourceId ?? row.id ?? '-' },
          { key: 'category', title: '카테고리', render: (row) => row.category ?? '-' },
          { key: 'title', title: '제목', render: (row) => row.title ?? '-' },
          { key: 'chunks', title: 'Chunk 수', render: (row) => row.chunkCount ?? '-' },
          {
            key: 'actions',
            title: '작업',
            render: (row) => (
              <div className="button-row compact">
                <button className="secondary" onClick={() => run(`소스 재색인 (${row.source ?? row.sourceId ?? row.id})`, () => ragApi.reindexSource({ category: row.category, source: row.source, version: row.version }))}>재색인</button>
                <button className="danger" onClick={() => run(`소스 삭제 (${row.source ?? row.sourceId ?? row.id})`, () => ragApi.purgeSource({ category: row.category, source: row.source, version: row.version }))}>삭제</button>
              </div>
            )
          }
        ]} />
      </AppCard>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
