import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { settingsApi } from '../../services/settingsApi';

export function ModelManagementPage() {
  const [ollamaConfig, setOllamaConfig] = useState<any>(null);
  const [modelPriority, setModelPriority] = useState<any>({});
  const [models, setModels] = useState<any[]>([]);
  const [source, setSource] = useState('RUNNING');
  const [status, setStatus] = useState('대기 중');
  const [actionModelName, setActionModelName] = useState('');
  const [actionPull, setActionPull] = useState('false');
  const [actionKeepAlive, setActionKeepAlive] = useState('24h');

  const load = async () => {
    const [cfg, priority, modelList] = await Promise.all([
      settingsApi.getOllamaConfig(),
      settingsApi.getModelPriority(),
      settingsApi.browseModels(source)
    ]);
    setOllamaConfig(cfg);
    setModelPriority(priority);
    setModels(modelList);
  };

  useEffect(() => { load().catch(() => undefined); }, [source]);

  const savePriority = async () => {
    setStatus('저장 중');
    try {
      await settingsApi.saveModelPriority(modelPriority);
      setStatus('모델 저장 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const runAction = async () => {
    setStatus('실행 중');
    try {
      const result = await settingsApi.modelAction({ modelName: actionModelName, pull: actionPull === 'true', keepAlive: actionKeepAlive });
      setStatus(`실행 완료: ${JSON.stringify(result)}`);
      await load();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <AppCard title="모델 우선순위" description="legacy settings 페이지의 카테고리별 모델 설정을 React 폼으로 분리했습니다." actions={<button onClick={savePriority}>저장</button>}>
        <div className="form-grid three">
          {['generalModel','devModel','miceModel','travelSearchModel','travelPlanModel'].map(key => (
            <label key={key} className="field-label">{key}<input value={modelPriority?.[key] ?? ''} onChange={e => setModelPriority((prev: any) => ({ ...prev, [key]: e.target.value }))} /></label>
          ))}
        </div>
        <div className="status-line">{status}</div>
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="Ollama 설정"><JsonBlock value={ollamaConfig} /></AppCard>
        <AppCard title="런타임 모델 설정"><JsonBlock value={modelPriority} /></AppCard>
      </div>

      <AppCard title="모델 브라우저 / Pull / Run" description="RUNNING / INSTALLED / ALL 기준 조회와 액션 실행 기능을 분리했습니다.">
        <div className="toolbar">
          <select value={source} onChange={e => setSource(e.target.value)}>
            <option value="RUNNING">RUNNING</option>
            <option value="INSTALLED">INSTALLED</option>
            <option value="ALL">ALL</option>
          </select>
          <button className="secondary" onClick={() => load().catch(() => undefined)}>모델 목록 조회</button>
        </div>
        <DataTable
          rows={models}
          columns={[
            { key: 'name', title: '모델명', render: row => row.name ?? row.model ?? '-' },
            { key: 'state', title: '상태', render: row => row.state ?? '-' },
            { key: 'size', title: 'Size', render: row => row.size ?? '-' },
            { key: 'updated', title: '수정일', render: row => row.modifiedAt ?? row.updatedAt ?? '-' }
          ]}
        />
        <div className="form-grid three top-gap">
          <label className="field-label">모델명<input value={actionModelName} onChange={e => setActionModelName(e.target.value)} /></label>
          <label className="field-label">작업 유형<select value={actionPull} onChange={e => setActionPull(e.target.value)}><option value="false">PS 로드만</option><option value="true">Pull 설치</option></select></label>
          <label className="field-label">keepAlive<input value={actionKeepAlive} onChange={e => setActionKeepAlive(e.target.value)} /></label>
        </div>
        <div className="button-row"><button onClick={runAction}>실행</button></div>
      </AppCard>
    </div>
  );
}
