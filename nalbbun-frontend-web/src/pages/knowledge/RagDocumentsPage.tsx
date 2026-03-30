import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { ragApi } from '../../services/settingsApi';
import { JsonBlock } from '../../components/ui/JsonBlock';

export function RagDocumentsPage() {
  const [status, setStatus] = useState<any>(null);
  const [dbInfo, setDbInfo] = useState<any>(null);
  const [sources, setSources] = useState<any[]>([]);
  const [category, setCategory] = useState('');
  const [textForm, setTextForm] = useState({ category: 'DEV', sourceId: '', title: '', content: '' });
  const [urlForm, setUrlForm] = useState({ category: 'DEV', sourceId: '', title: '', url: '' });
  const [file, setFile] = useState<File | null>(null);
  const [multiFiles, setMultiFiles] = useState<FileList | null>(null);
  const [opStatus, setOpStatus] = useState('대기 중');

  const load = async () => {
    const [statusData, dbData, sourceData] = await Promise.all([ragApi.getStatus(), ragApi.getDbInfo(), ragApi.getSources(category)]);
    setStatus(statusData);
    setDbInfo(dbData);
    setSources(sourceData);
  };

  useEffect(() => { load().catch(() => undefined); }, [category]);

  const uploadSingle = async () => {
    if (!file) return;
    const form = new FormData();
    form.append('file', file);
    form.append('category', textForm.category);
    form.append('sourceId', textForm.sourceId);
    form.append('title', textForm.title);
    setOpStatus('단일 업로드 중');
    try { await ragApi.ingestFile(form); setOpStatus('업로드 완료'); await load(); } catch (error) { setOpStatus(error instanceof Error ? error.message : String(error)); }
  };

  const uploadMulti = async () => {
    if (!multiFiles?.length) return;
    const form = new FormData();
    Array.from(multiFiles).forEach(f => form.append('files', f));
    form.append('category', textForm.category);
    form.append('sourceId', textForm.sourceId);
    form.append('title', textForm.title);
    setOpStatus('멀티 업로드 중');
    try { await ragApi.ingestFiles(form); setOpStatus('업로드 완료'); await load(); } catch (error) { setOpStatus(error instanceof Error ? error.message : String(error)); }
  };

  return (
    <div className="page-stack">
      <div className="two-column-grid">
        <AppCard title="RAG 상태"><JsonBlock value={status} /></AppCard>
        <AppCard title="RAG DB 정보"><JsonBlock value={dbInfo} /></AppCard>
      </div>

      <AppCard title="문서 인입" description="legacy rag/index 화면의 text/url/file 멀티 인입 기능을 React 폼으로 분리했습니다.">
        <div className="form-grid two">
          <label className="field-label">카테고리<select value={textForm.category} onChange={e => { const value = e.target.value; setTextForm(prev => ({ ...prev, category: value })); setUrlForm(prev => ({ ...prev, category: value })); }}><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select></label>
          <label className="field-label">Source ID<input value={textForm.sourceId} onChange={e => { const value = e.target.value; setTextForm(prev => ({ ...prev, sourceId: value })); setUrlForm(prev => ({ ...prev, sourceId: value })); }} /></label>
        </div>
        <label className="field-label">Title<input value={textForm.title} onChange={e => { const value = e.target.value; setTextForm(prev => ({ ...prev, title: value })); setUrlForm(prev => ({ ...prev, title: value })); }} /></label>
        <div className="two-column-grid">
          <AppCard title="텍스트 인입"><label className="field-label">내용<textarea rows={8} value={textForm.content} onChange={e => setTextForm(prev => ({ ...prev, content: e.target.value }))} /></label><div className="button-row"><button onClick={() => ragApi.ingestText(textForm).then(() => { setOpStatus('텍스트 인입 완료'); load(); }).catch(err => setOpStatus(err.message))}>텍스트 업로드</button></div></AppCard>
          <AppCard title="URL 인입"><label className="field-label">URL<input value={urlForm.url} onChange={e => setUrlForm(prev => ({ ...prev, url: e.target.value }))} /></label><div className="button-row"><button onClick={() => ragApi.ingestUrl(urlForm).then(() => { setOpStatus('URL 인입 완료'); load(); }).catch(err => setOpStatus(err.message))}>URL 업로드</button></div></AppCard>
        </div>
        <div className="form-grid two top-gap">
          <label className="field-label">단일 파일<input type="file" onChange={e => setFile(e.target.files?.[0] || null)} /></label>
          <label className="field-label">멀티 파일<input type="file" multiple onChange={e => setMultiFiles(e.target.files)} /></label>
        </div>
        <div className="button-row"><button onClick={uploadSingle}>단일 업로드</button><button className="secondary" onClick={uploadMulti}>멀티 업로드</button></div>
        <div className="status-line">{opStatus}</div>
      </AppCard>

      <AppCard title="소스 목록" actions={<div className="toolbar"><select value={category} onChange={e => setCategory(e.target.value)}><option value="">전체</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select><button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button></div>}>
        <DataTable rows={sources} columns={[
          { key: 'source', title: 'Source', render: row => row.sourceId ?? row.id ?? '-' },
          { key: 'category', title: '카테고리', render: row => row.category ?? '-' },
          { key: 'title', title: '제목', render: row => row.title ?? '-' },
          { key: 'chunks', title: 'Chunk 수', render: row => row.chunkCount ?? '-' },
          { key: 'actions', title: '작업', render: row => <div className="button-row compact"><button className="secondary" onClick={() => ragApi.reindexSource({ sourceId: row.sourceId ?? row.id }).then(() => load()).catch(() => undefined)}>재색인</button><button className="danger" onClick={() => ragApi.purgeSource({ sourceId: row.sourceId ?? row.id }).then(() => load()).catch(() => undefined)}>삭제</button></div> }
        ]} />
      </AppCard>
    </div>
  );
}
