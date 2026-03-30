import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { settingsApi } from '../../services/settingsApi';
import type { DebugOllamaConfig, DebugOllamaConnectionInfo, ModelPriorityItem, OllamaModelInfo } from '../../types/api';

const configDefaults: DebugOllamaConfig = {
  modelSource: 'RUNNING',
  generalModel: '',
  devModel: '',
  miceModel: '',
  travelSearchModel: '',
  travelPlanModel: '',
  residentModelList: '',
  residentKeepAlive: '24h'
};

const sourceOptions = [
  { value: 'RUNNING', label: 'RUNNING (현재 PS 실행 중)' },
  { value: 'INSTALLED', label: 'INSTALLED (설치된 전체)' },
  { value: 'ALL', label: 'ALL (전체 병합)' }
];

const priorityTargets = ['GENERAL', 'DEV', 'MICE', 'TRAVEL_SEARCH', 'TRAVEL_PLAN'] as const;
const priorityPolicyOptions = [
  { value: 'OLLAMA_FIRST', label: 'OLLAMA_FIRST', description: 'Ollama 우선 → 실패 시 OpenAI fallback' },
  { value: 'OPENAI_FIRST', label: 'OPENAI_FIRST', description: 'OpenAI 우선 → 실패 시 Ollama fallback' },
  { value: 'OLLAMA_ONLY', label: 'OLLAMA_ONLY', description: 'Ollama 전용' },
  { value: 'OPENAI_ONLY', label: 'OPENAI_ONLY', description: 'OpenAI 전용' }
] as const;

const priorityLabelMap: Record<(typeof priorityTargets)[number], string> = {
  GENERAL: 'GENERAL',
  DEV: 'DEV',
  MICE: 'MICE',
  TRAVEL_SEARCH: 'TRAVEL Search',
  TRAVEL_PLAN: 'TRAVEL Plan'
};


export function ModelManagementPage() {
  const [config, setConfig] = useState<DebugOllamaConfig>(configDefaults);
  const [connection, setConnection] = useState<DebugOllamaConnectionInfo | null>(null);
  const [models, setModels] = useState<OllamaModelInfo[]>([]);
  const [source, setSource] = useState('RUNNING');
  const [status, setStatus] = useState('운영 > 모델 관리 화면을 불러오는 중입니다.');
  const [baseUrl, setBaseUrl] = useState('');
  const [actionModelName, setActionModelName] = useState('');
  const [actionPull, setActionPull] = useState('false');
  const [actionKeepAlive, setActionKeepAlive] = useState('24h');
  const [modelPriority, setModelPriority] = useState<Record<string, ModelPriorityItem>>({});
  const logs = useEventLog('operations-model-management-log', ['모델 관리 작업 로그가 여기에 누적됩니다.']);

  const availableModelNames = useMemo(
    () => models.map((item) => item.name || item.model || '').filter(Boolean),
    [models]
  );

  const syncStatus = (message: string, detail?: unknown) => {
    setStatus(message);
    logs.append(message, detail);
  };

  const loadConnection = async () => {
    const [cfg, conn] = await Promise.all([
      settingsApi.getOllamaConfig(),
      settingsApi.checkConnection()
    ]);
    setConfig((prev) => ({ ...prev, ...configDefaults, ...cfg, modelSource: source }));
    setConnection(conn);
    setBaseUrl(conn?.baseUrl ?? cfg?.modelSource ?? '');
    return { cfg, conn };
  };

  const loadModels = async (targetSource = source) => {
    const [priority, modelList] = await Promise.all([
      settingsApi.getModelPriority(),
      settingsApi.browseModels(targetSource)
    ]);
    setModelPriority(priority);
    setModels(modelList);
    return { priority, modelList };
  };

  const loadAll = async (targetSource = source) => {
    syncStatus('연결, 모델, 우선순위 정보를 다시 조회합니다.');
    try {
      const [{ conn }, { modelList }] = await Promise.all([
        loadConnection(),
        loadModels(targetSource)
      ]);
      syncStatus(`조회 완료: ${targetSource} 기준 ${modelList.length}개 모델 / reachable=${String(conn?.reachable ?? false)}`);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  useEffect(() => {
    loadAll(source).catch(() => undefined);
  }, [source]);

  const saveConnection = async () => {
    if (!baseUrl.trim()) {
      syncStatus('Base URL을 입력한 뒤 저장하세요.');
      return;
    }
    syncStatus('Ollama 연결 URL 저장 및 재연결 확인을 시작합니다.', { baseUrl });
    try {
      const result = await settingsApi.saveConnection({ baseUrl: baseUrl.trim() });
      setConnection(result);
      setBaseUrl(result.baseUrl ?? baseUrl.trim());
      syncStatus('연결 URL 저장 완료', result);
      await loadAll(source);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const checkConnection = async () => {
    syncStatus('Ollama 연결 상태를 확인합니다.');
    try {
      const result = await settingsApi.checkConnection();
      setConnection(result);
      setBaseUrl(result.baseUrl ?? baseUrl);
      syncStatus('연결 확인 완료', result);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetConnection = async () => {
    syncStatus('연결 정보를 기본값으로 되돌립니다.');
    try {
      await settingsApi.resetConnection();
      await loadAll(source);
      syncStatus('연결 정보 초기화 완료');
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const saveConfig = async () => {
    syncStatus('카테고리별 모델 구성을 저장합니다.', config);
    try {
      const saved = await settingsApi.saveOllamaConfig(config);
      setConfig({ ...configDefaults, ...saved, modelSource: source });
      syncStatus('카테고리별 모델 저장 완료', saved);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetConfig = async () => {
    syncStatus('카테고리별 모델 구성을 초기화합니다.');
    try {
      const resetResult = await settingsApi.resetOllamaConfig();
      setConfig({ ...configDefaults, ...resetResult, modelSource: source });
      syncStatus('카테고리별 모델 초기화 완료', resetResult);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const savePriority = async () => {
    const payload = Object.fromEntries(priorityTargets.map((target) => [target, modelPriority[target]?.priority || 'OLLAMA_FIRST']));
    syncStatus('런타임 모델 우선순위 정책을 저장합니다.', payload);
    try {
      const saved = await settingsApi.saveModelPriority(payload);
      setModelPriority(saved);
      syncStatus('런타임 모델 우선순위 정책 저장 완료', saved);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetPriority = async () => {
    syncStatus('런타임 모델 우선순위를 초기화합니다.');
    try {
      const resetResult = await settingsApi.resetModelPriority();
      setModelPriority(resetResult);
      syncStatus('런타임 모델 우선순위 초기화 완료', resetResult);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const runAction = async (override?: { model?: string; pull?: boolean }) => {
    const model = (override?.model ?? actionModelName).trim();
    const pull = override?.pull ?? actionPull === 'true';
    if (!model) {
      syncStatus('작업할 모델명을 입력하세요.');
      return;
    }
    const payload = { model, pull, keepAlive: actionKeepAlive || undefined };
    syncStatus(`모델 액션 실행: ${pull ? 'PULL' : 'RUN'} ${model}`, payload);
    try {
      const result = await settingsApi.modelAction(payload);
      syncStatus(`실행 완료: ${result.message || result.action || 'ok'}`, result);
      await loadAll(source);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <AppCard
        title="모델 관리"
        description="운영 사용 순서대로 외부장치 연결, 모델 조회/실행, 카테고리별 모델 설정, 우선순위 설정을 한 화면으로 재구성했습니다."
        actions={<div className="button-row compact"><button className="secondary" onClick={() => loadAll(source).catch(() => undefined)}>전체 새로고침</button></div>}
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Base URL</span><strong>{connection?.baseUrl || '-'}</strong></div>
          <div className="stat-box"><span>Reachable</span><strong>{String(connection?.reachable ?? false)}</strong></div>
          <div className="stat-box"><span>조회 모델 수</span><strong>{models.length}</strong></div>
          <div className="stat-box"><span>조회 대상</span><strong>{source}</strong></div>
        </div>

        <div className="sub-panel top-gap">
          <h3>1. 외부장치 연결</h3>
          <div className="two-column-grid top-gap">
            <div>
              <label className="field-label">Ollama Base URL
                <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="http://127.0.0.1:11434" />
              </label>
              <div className="button-row">
                <button onClick={checkConnection}>연결 확인</button>
                <button onClick={saveConnection}>URL 저장</button>
                <button className="secondary" onClick={resetConnection}>초기화</button>
              </div>
            </div>
            <div className="list-stack">
              <div className="list-item-row"><span>상태</span><StatusBadge label={connection?.status || '-'} tone={connection?.reachable ? 'success' : 'warning'} /></div>
              <div className="list-item-row"><span>메시지</span><span>{connection?.message || '-'}</span></div>
              <div className="list-item-row"><span>실행 모델</span><span>{(connection?.runningModels || []).join(', ') || '(없음)'}</span></div>
              <div className="list-item-row"><span>Running / Installed</span><span>{connection?.runningCount ?? 0} / {connection?.installedCount ?? 0}</span></div>
            </div>
          </div>
        </div>

        <div className="sub-panel top-gap">
          <h3>2. 모델 조회 및 실행</h3>
          <div className="toolbar top-gap">
            <select value={source} onChange={(e) => setSource(e.target.value)}>
              {sourceOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
            <button className="secondary" onClick={() => loadModels(source).then(({ modelList }) => syncStatus(`모델 목록 조회 완료: ${modelList.length}건`)).catch((e) => syncStatus(e instanceof Error ? e.message : String(e)))}>모델 목록 조회</button>
          </div>

          <div className="form-grid three top-gap">
            <label className="field-label">모델명
              <input value={actionModelName} onChange={(e) => setActionModelName(e.target.value)} placeholder="qwen2.5-coder:14b" />
            </label>
            <label className="field-label">작업 유형
              <select value={actionPull} onChange={(e) => setActionPull(e.target.value)}>
                <option value="false">PS 로드만</option>
                <option value="true">Pull 설치</option>
              </select>
            </label>
            <label className="field-label">KeepAlive
              <input value={actionKeepAlive} onChange={(e) => setActionKeepAlive(e.target.value)} placeholder="24h" />
            </label>
          </div>
          <div className="button-row"><button onClick={() => runAction()}>실행</button></div>

          <DataTable
            rows={models}
            columns={[
              { key: 'name', title: '모델명', render: (row) => row.name ?? row.model ?? '-' },
              { key: 'state', title: '상태', render: (row) => row.state ?? '-' },
              { key: 'size', title: 'Size', render: (row) => typeof row.size === 'number' ? `${(row.size / 1e9).toFixed(1)} GB` : '-' },
              { key: 'updated', title: '수정일', render: (row) => row.modifiedAt ?? row.updatedAt ?? '-' },
              { key: 'actions', title: '빠른 작업', render: (row) => {
                const modelName = row.name ?? row.model ?? '';
                return (
                  <div className="button-row compact">
                    <button className="secondary" onClick={() => { setActionModelName(modelName); void runAction({ model: modelName, pull: false }); }}>Run</button>
                    <button className="secondary" onClick={() => { setActionModelName(modelName); void runAction({ model: modelName, pull: true }); }}>Pull</button>
                  </div>
                );
              } }
            ]}
          />
        </div>

        <div className="sub-panel top-gap">
          <h3>3. 카테고리별 모델 설정</h3>
          <div className="form-grid two top-gap">
            <ModelSelect label="GENERAL" value={config.generalModel || ''} models={availableModelNames} onChange={(value) => setConfig((prev) => ({ ...prev, generalModel: value }))} />
            <ModelSelect label="DEV" value={config.devModel || ''} models={availableModelNames} onChange={(value) => setConfig((prev) => ({ ...prev, devModel: value }))} />
            <ModelSelect label="MICE" value={config.miceModel || ''} models={availableModelNames} onChange={(value) => setConfig((prev) => ({ ...prev, miceModel: value }))} />
            <ModelSelect label="TRAVEL Search" value={config.travelSearchModel || ''} models={availableModelNames} onChange={(value) => setConfig((prev) => ({ ...prev, travelSearchModel: value }))} />
            <ModelSelect label="TRAVEL Plan" value={config.travelPlanModel || ''} models={availableModelNames} onChange={(value) => setConfig((prev) => ({ ...prev, travelPlanModel: value }))} />
            <label className="field-label">Resident KeepAlive
              <input value={config.residentKeepAlive || ''} onChange={(e) => setConfig((prev) => ({ ...prev, residentKeepAlive: e.target.value }))} placeholder="24h / 10m / -1s" />
            </label>
          </div>
          <label className="field-label top-gap">Resident Model List
            <textarea rows={4} value={config.residentModelList || ''} onChange={(e) => setConfig((prev) => ({ ...prev, residentModelList: e.target.value }))} placeholder={'gemma2:9b\nqwen2.5-coder:14b'} />
          </label>
          <div className="button-row"><button onClick={saveConfig}>설정 저장</button><button className="secondary" onClick={resetConfig}>설정 초기화</button></div>
        </div>

        <div className="sub-panel top-gap">
          <h3>4. 런타임 모델 우선순위 정책</h3>
          <p className="helper-text top-gap">이 항목은 모델명이 아니라 provider 라우팅 정책입니다. 카테고리별로 Ollama/OpenAI 우선 사용 순서를 지정합니다.</p>
          <div className="form-grid two top-gap">
            {priorityTargets.map((target) => (
              <PriorityPolicySelect
                key={target}
                label={priorityLabelMap[target]}
                value={modelPriority[target]?.priority ?? 'OLLAMA_FIRST'}
                description={modelPriority[target]?.description}
                onChange={(value) => setModelPriority((prev) => ({
                  ...prev,
                  [target]: {
                    priority: value,
                    description: priorityPolicyOptions.find((item) => item.value === value)?.description || ''
                  }
                }))}
              />
            ))}
          </div>
          <div className="button-row"><button onClick={savePriority}>우선순위 정책 저장</button><button className="secondary" onClick={resetPriority}>기본값 복원</button></div>
        </div>

        <div className="status-line">{status}</div>
      </AppCard>

      <AppCard title="운영 이벤트 로그" description="모델 관리 화면에서 수행한 연결 확인, 조회, 저장, 실행 이력이 하단에 계속 남습니다." actions={<button className="secondary" onClick={logs.clear}>로그 비우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="연결 JSON"><JsonBlock value={connection} /></AppCard>
        <AppCard title="모델 설정 JSON"><JsonBlock value={{ config, modelPriority }} /></AppCard>
      </div>
    </div>
  );
}

function ModelSelect({ label, value, models, onChange }: { label: string; value: string; models: string[]; onChange: (value: string) => void }) {
  return (
    <label className="field-label">{label}
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">-- 선택 --</option>
        {models.map((model) => <option key={model} value={model}>{model}</option>)}
      </select>
    </label>
  );
}

function PriorityPolicySelect({
  label,
  value,
  description,
  onChange
}: {
  label: string;
  value: string;
  description?: string;
  onChange: (value: string) => void;
}) {
  const current = priorityPolicyOptions.find((item) => item.value === value);

  return (
    <label className="field-label">{label}
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        {priorityPolicyOptions.map((item) => (
          <option key={item.value} value={item.value}>
            {item.label}
          </option>
        ))}
      </select>
      <span className="helper-text">{description || current?.description}</span>
    </label>
  );
}
