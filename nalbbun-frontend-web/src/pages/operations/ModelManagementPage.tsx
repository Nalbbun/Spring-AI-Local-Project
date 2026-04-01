import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { settingsApi } from '../../services/settingsApi';
import { keyApi } from '../../services/keyApi';
import type { DebugApiLlmConnectionInfo, DebugApiLlmProviderConfig, DebugOllamaConfig, DebugOllamaConnectionInfo, ModelPriorityItem, OllamaModelInfo } from '../../types/api';

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
  { value: 'OLLAMA_FIRST', label: 'OLLAMA_FIRST' },
  { value: 'VLLM_FIRST', label: 'VLLM_FIRST' },
  { value: 'OPENAI_FIRST', label: 'OPENAI_FIRST' },
  { value: 'OLLAMA_ONLY', label: 'OLLAMA_ONLY' },
  { value: 'VLLM_ONLY', label: 'VLLM_ONLY' },
  { value: 'OPENAI_ONLY', label: 'OPENAI_ONLY' }
] as const;

const priorityLabelMap: Record<(typeof priorityTargets)[number], string> = {
  GENERAL: 'GENERAL',
  DEV: 'DEV',
  MICE: 'MICE',
  TRAVEL_SEARCH: 'TRAVEL Search',
  TRAVEL_PLAN: 'TRAVEL Plan'
};

const providerDefaults: Record<'vllm' | 'openai', DebugApiLlmProviderConfig> = {
  vllm: {
    baseUrl: 'http://127.0.0.1:8000',
    defaultModel: '',
    keyProvider: 'VLLM',
    healthCheckPath: '/api/info',
    healthCheckMethod: 'POST',
    modelsPath: '/v1/models',
    modelsMethod: 'GET'
  },
  openai: {
    baseUrl: 'https://api.openai.com',
    defaultModel: 'gpt-4.1-mini',
    keyProvider: 'OPENAI',
    healthCheckPath: '/v1/models',
    healthCheckMethod: 'GET',
    modelsPath: '/v1/models',
    modelsMethod: 'GET'
  }
};

const MAX_PROVIDER_MODEL_PREVIEW = 120;

function formatModelListPreview(models?: string[]) {
  if (!models || models.length === 0) return '-';
  const joined = models.join(', ');
  if (joined.length <= MAX_PROVIDER_MODEL_PREVIEW) return joined;
  return `${joined.slice(0, MAX_PROVIDER_MODEL_PREVIEW)}...`;
}


type OllamaConfigFieldKey = 'generalModel' | 'devModel' | 'miceModel' | 'travelSearchModel' | 'travelPlanModel';

function buildModelSelectOptions(availableModels: string[], currentValue?: string) {
  const normalized = Array.from(new Set((availableModels || []).map((item) => item?.trim()).filter(Boolean))) as string[];
  const options = [...normalized];
  if (currentValue && !options.includes(currentValue)) {
    options.unshift(currentValue);
  }
  return options;
}

function CategoryModelSelect({
  label,
  value,
  field,
  config,
  availableModels,
  onChange
}: {
  label: string;
  value?: string;
  field: OllamaConfigFieldKey;
  config: DebugOllamaConfig;
  availableModels: string[];
  onChange: (next: DebugOllamaConfig) => void;
}) {
  const options = buildModelSelectOptions(availableModels, value || '');
  const hasOptions = options.length > 0;

  return (
    <label className="field-label">{label}
      <select
        value={value || ''}
        onChange={(e) => onChange({ ...config, [field]: e.target.value })}
        disabled={!hasOptions}
      >
        {hasOptions ? (
          <>
            <option value="">없음</option>
            {options.map((name) => (
              <option key={name} value={name}>{name}</option>
            ))}
          </>
        ) : (
          <option value="">없음</option>
        )}
      </select>
    </label>
  );
}



function ProviderCard({
  title,
  form,
  status,
  onChange,
  onCheck,
  onSave,
  onReset,
  providerOptions
}: {
  title: string;
  form: DebugApiLlmProviderConfig;
  status: DebugApiLlmConnectionInfo | null;
  onChange: (next: DebugApiLlmProviderConfig) => void;
  onCheck: () => void;
  onSave: () => void;
  onReset: () => void;
  providerOptions: string[];
}) {
  return (
    <div className="sub-panel">
      <h3>{title}</h3>
      <div className="two-column-grid top-gap">
        <div>
          <label className="field-label">Base URL
            <input value={form.baseUrl || ''} onChange={(e) => onChange({ ...form, baseUrl: e.target.value })} placeholder="https://api.openai.com/v1 또는 http://127.0.0.1:8000/v1" />
          </label>
          <label className="field-label">Default Model
            <input value={form.defaultModel || ''} onChange={(e) => onChange({ ...form, defaultModel: e.target.value })} placeholder="gpt-4.1-mini / qwen2.5-14b 등" />
          </label>
          <label className="field-label">키 제공자
            <select value={form.keyProvider || ''} onChange={(e) => onChange({ ...form, keyProvider: e.target.value })}>
              <option value="">선택</option>
              {providerOptions.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
          <div className="two-column-grid top-gap">
            <label className="field-label">Health Path
              <input value={form.healthCheckPath || ''} onChange={(e) => onChange({ ...form, healthCheckPath: e.target.value })} placeholder="/api/info 또는 /v1/models" />
            </label>
            <label className="field-label">Health Method
              <select value={form.healthCheckMethod || 'GET'} onChange={(e) => onChange({ ...form, healthCheckMethod: e.target.value })}>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
              </select>
            </label>
            <label className="field-label">Models Path
              <input value={form.modelsPath || ''} onChange={(e) => onChange({ ...form, modelsPath: e.target.value })} placeholder="/v1/models" />
            </label>
            <label className="field-label">Models Method
              <select value={form.modelsMethod || 'GET'} onChange={(e) => onChange({ ...form, modelsMethod: e.target.value })}>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
              </select>
            </label>
          </div>
          <div className="button-row">
            <button onClick={onCheck}>연결 확인</button>
            <button onClick={onSave}>저장</button>
            <button className="secondary" onClick={onReset}>초기화</button>
          </div>
        </div>
        <div className="list-stack">
          <div className="list-item-row"><span>상태</span><StatusBadge label={status?.status || '-'} tone={status?.reachable ? 'success' : 'warning'} /></div>
          <div className="list-item-row"><span>메시지</span><span>{status?.message || '-'}</span></div>
          <div className="list-item-row"><span>키 조회</span><span>{String(status?.keyResolved ?? false)} / {status?.keyProvider || '-'}</span></div>
          <div className="list-item-row"><span>Health 체크</span><span>{status?.healthCheckMethod || '-'} {status?.healthCheckPath || '-'} / {String(status?.healthCheckOk ?? false)}</span></div>
          <div className="list-item-row"><span>Models 체크</span><span>{status?.modelsMethod || '-'} {status?.modelsPath || '-'} / {String(status?.modelsCheckOk ?? false)}</span></div>
          <div className="list-item-row"><span>Health URL</span><span title={status?.resolvedHealthUrl || '-'}>{status?.resolvedHealthUrl || '-'}</span></div>
          <div className="list-item-row"><span>Models URL</span><span title={status?.resolvedModelsUrl || '-'}>{status?.resolvedModelsUrl || '-'}</span></div>
          <div className="list-item-row"><span>모델 수</span><span>{status?.modelCount ?? 0}</span></div>
          <div className="list-item-row"><span>모델 목록</span><span title={(status?.availableModels || []).join(', ') || '-'}>{formatModelListPreview(status?.availableModels)}</span></div>
        </div>
      </div>
    </div>
  );
}

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
  const [providerStatus, setProviderStatus] = useState<Record<string, DebugApiLlmConnectionInfo>>({});
  const [vllmForm, setVllmForm] = useState<DebugApiLlmProviderConfig>(providerDefaults.vllm);
  const [openAiForm, setOpenAiForm] = useState<DebugApiLlmProviderConfig>(providerDefaults.openai);
  const [keyProviderOptions, setKeyProviderOptions] = useState<string[]>(['OPENAI', 'VLLM']);
  const logs = useEventLog('operations-model-management-log', ['모델 관리 작업 로그가 여기에 누적됩니다.']);

  const availableModelNames = useMemo(
    () => models.map((item) => item.name || item.model || '').filter(Boolean),
    [models]
  );

  const syncStatus = (message: string, detail?: unknown) => {
    setStatus(message);
    logs.append(message, detail);
  };

  const refreshProviders = async () => {
    const [providers, apiKeyProviders] = await Promise.all([
      settingsApi.getLlmProvidersStatus(),
      keyApi.providers().catch(() => [])
    ]);
    setProviderStatus(providers || {});
    setVllmForm({
      baseUrl: providers?.vllm?.baseUrl || providerDefaults.vllm.baseUrl,
      defaultModel: providers?.vllm?.defaultModel || providerDefaults.vllm.defaultModel,
      keyProvider: providers?.vllm?.keyProvider || providerDefaults.vllm.keyProvider,
      healthCheckPath: providers?.vllm?.healthCheckPath || providerDefaults.vllm.healthCheckPath,
      healthCheckMethod: providers?.vllm?.healthCheckMethod || providerDefaults.vllm.healthCheckMethod,
      modelsPath: providers?.vllm?.modelsPath || providerDefaults.vllm.modelsPath,
      modelsMethod: providers?.vllm?.modelsMethod || providerDefaults.vllm.modelsMethod
    });
    setOpenAiForm({
      baseUrl: providers?.openai?.baseUrl || providerDefaults.openai.baseUrl,
      defaultModel: providers?.openai?.defaultModel || providerDefaults.openai.defaultModel,
      keyProvider: providers?.openai?.keyProvider || providerDefaults.openai.keyProvider,
      healthCheckPath: providers?.openai?.healthCheckPath || providerDefaults.openai.healthCheckPath,
      healthCheckMethod: providers?.openai?.healthCheckMethod || providerDefaults.openai.healthCheckMethod,
      modelsPath: providers?.openai?.modelsPath || providerDefaults.openai.modelsPath,
      modelsMethod: providers?.openai?.modelsMethod || providerDefaults.openai.modelsMethod
    });

    const dynamicOptions = Array.from(new Set(
      (apiKeyProviders || [])
        .map((item) => item.provider?.trim())
        .filter((item): item is string => Boolean(item))
        .concat(['OPENAI', 'VLLM'])
    )).sort((a, b) => a.localeCompare(b));
    setKeyProviderOptions(dynamicOptions);

    return providers;
  };

  const loadConnection = async () => {
    const [cfg, conn] = await Promise.all([
      settingsApi.getOllamaConfig(),
      settingsApi.checkConnection()
    ]);
    setConfig((prev) => ({ ...prev, ...configDefaults, ...cfg, modelSource: source }));
    setConnection(conn);
    setBaseUrl(conn?.baseUrl ?? '');
    return { cfg, conn };
  };

  const loadModels = async (targetSource = source) => {
    const [priority, modelList, providers] = await Promise.all([
      settingsApi.getModelPriority(),
      settingsApi.browseModels(targetSource),
      refreshProviders()
    ]);
    setModelPriority(priority);
    setModels(modelList);
    return { priority, modelList, providers };
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
      syncStatus('연결 확인 완료', result);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetConnection = async () => {
    syncStatus('Ollama 연결 정보를 기본값으로 되돌립니다.');
    try {
      await settingsApi.resetConnection();
      await loadAll(source);
      syncStatus('연결 정보 초기화 완료');
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const saveProvider = async (type: 'vllm' | 'openai') => {
    const form = type === 'vllm' ? vllmForm : openAiForm;
    syncStatus(`${type.toUpperCase()} 연결 정보를 저장합니다.`, form);
    try {
      const result = type === 'vllm'
        ? await settingsApi.saveVllmConfig(form)
        : await settingsApi.saveOpenAiConfig(form);
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      syncStatus(`${type.toUpperCase()} 연결 저장 완료`, result);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetProvider = async (type: 'vllm' | 'openai') => {
    syncStatus(`${type.toUpperCase()} 연결 정보를 초기화합니다.`);
    try {
      const result = type === 'vllm'
        ? await settingsApi.resetVllmConfig()
        : await settingsApi.resetOpenAiConfig();
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      if (type === 'vllm') setVllmForm({
        baseUrl: result.baseUrl || '',
        defaultModel: result.defaultModel || '',
        keyProvider: result.keyProvider || 'VLLM',
        healthCheckPath: result.healthCheckPath || providerDefaults.vllm.healthCheckPath,
        healthCheckMethod: result.healthCheckMethod || providerDefaults.vllm.healthCheckMethod,
        modelsPath: result.modelsPath || providerDefaults.vllm.modelsPath,
        modelsMethod: result.modelsMethod || providerDefaults.vllm.modelsMethod
      });
      else setOpenAiForm({
        baseUrl: result.baseUrl || '',
        defaultModel: result.defaultModel || '',
        keyProvider: result.keyProvider || 'OPENAI',
        healthCheckPath: result.healthCheckPath || providerDefaults.openai.healthCheckPath,
        healthCheckMethod: result.healthCheckMethod || providerDefaults.openai.healthCheckMethod,
        modelsPath: result.modelsPath || providerDefaults.openai.modelsPath,
        modelsMethod: result.modelsMethod || providerDefaults.openai.modelsMethod
      });
      syncStatus(`${type.toUpperCase()} 연결 초기화 완료`, result);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const checkProvider = async (type: 'vllm' | 'openai') => {
    syncStatus(`${type.toUpperCase()} 연결 상태를 확인합니다.`);
    try {
      const result = type === 'vllm' ? await settingsApi.getVllmStatus() : await settingsApi.getOpenAiStatus();
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      syncStatus(`${type.toUpperCase()} 연결 확인 완료`, result);
    } catch (error) {
      syncStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const saveConfig = async () => {
    syncStatus('카테고리별 Ollama 모델 구성을 저장합니다.', config);
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
        description="기존 Ollama 기능은 유지하고, vLLM / OpenAI 호환 API 상태 체크와 키관리 연동을 추가했습니다. API 키는 별도 키관리에서 활성화된 값을 사용합니다."
        actions={<div className="button-row compact"><button className="secondary" onClick={() => loadAll(source).catch(() => undefined)}>전체 새로고침</button></div>}
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Ollama Base URL</span><strong>{connection?.baseUrl || '-'}</strong></div>
          <div className="stat-box"><span>Ollama Reachable</span><strong>{String(connection?.reachable ?? false)}</strong></div>
          <div className="stat-box"><span>조회 모델 수</span><strong>{models.length}</strong></div>
          <div className="stat-box"><span>조회 대상</span><strong>{source}</strong></div>
        </div>

        <div className="sub-panel top-gap">
          <h3>1. Ollama 연결</h3>
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

        <div className="two-column-grid top-gap wider-left">
          <ProviderCard title="2. vLLM API 연결" form={vllmForm} status={providerStatus.vllm || null} onChange={setVllmForm} onCheck={() => checkProvider('vllm')} onSave={() => saveProvider('vllm')} onReset={() => resetProvider('vllm')} providerOptions={keyProviderOptions} />
          <ProviderCard title="3. OpenAI / OpenAPI 연결" form={openAiForm} status={providerStatus.openai || null} onChange={setOpenAiForm} onCheck={() => checkProvider('openai')} onSave={() => saveProvider('openai')} onReset={() => resetProvider('openai')} providerOptions={keyProviderOptions} />
        </div>

        <div className="sub-panel top-gap">
          <h3>4. Ollama 모델 조회 및 실행</h3>
          <div className="toolbar top-gap">
            <select value={source} onChange={(e) => setSource(e.target.value)}>
              {sourceOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
            <button className="secondary" onClick={() => loadModels(source).catch(() => undefined)}>모델 목록 새로고침</button>
          </div>
          <div className="two-column-grid top-gap wider-left">
            <div>
              <DataTable
                rows={models}
                columns={[
                  { key: 'name', title: '모델명', render: (row) => row.name || row.model || '-' },
                  { key: 'state', title: '상태', render: (row) => row.state || '-' },
                  { key: 'size', title: '크기', render: (row) => row.size || '-' },
                  { key: 'actions', title: '작업', render: (row) => <div className="button-row compact"><button className="secondary" onClick={() => runAction({ model: row.name || row.model || '', pull: false })}>실행</button><button className="secondary" onClick={() => runAction({ model: row.name || row.model || '', pull: true })}>Pull</button></div> }
                ]}
              />
            </div>
            <div>
              <label className="field-label">모델명
                <input list="model-name-options" value={actionModelName} onChange={(e) => setActionModelName(e.target.value)} placeholder="예: qwen2.5-coder:14b" />
                <datalist id="model-name-options">
                  {availableModelNames.map((name) => <option key={name} value={name} />)}
                </datalist>
              </label>
              <label className="field-label">작업 유형
                <select value={actionPull} onChange={(e) => setActionPull(e.target.value)}>
                  <option value="false">Run</option>
                  <option value="true">Pull</option>
                </select>
              </label>
              <label className="field-label">Keep Alive
                <input value={actionKeepAlive} onChange={(e) => setActionKeepAlive(e.target.value)} placeholder="24h" />
              </label>
              <div className="button-row"><button onClick={() => runAction()}>실행</button></div>
            </div>
          </div>
        </div>

        <div className="two-column-grid top-gap wider-left">
          <AppCard title="5. 카테고리별 Ollama 모델 설정" description="현재 Ollama 모델 맵핑을 유지합니다.">
            <div className="form-grid two">
              <label className="field-label">Model Source<select value={config.modelSource || 'RUNNING'} onChange={(e) => setConfig((prev) => ({ ...prev, modelSource: e.target.value }))}>{sourceOptions.map((item) => <option key={item.value} value={item.value}>{item.value}</option>)}</select></label>
              <label className="field-label">Resident Keep Alive<input value={config.residentKeepAlive || ''} onChange={(e) => setConfig((prev) => ({ ...prev, residentKeepAlive: e.target.value }))} /></label>
              <CategoryModelSelect label="GENERAL" field="generalModel" value={config.generalModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
              <CategoryModelSelect label="DEV" field="devModel" value={config.devModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
              <CategoryModelSelect label="MICE" field="miceModel" value={config.miceModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
              <CategoryModelSelect label="TRAVEL_SEARCH" field="travelSearchModel" value={config.travelSearchModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
              <CategoryModelSelect label="TRAVEL_PLAN" field="travelPlanModel" value={config.travelPlanModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
              <label className="field-label">Resident Model List<textarea value={config.residentModelList || ''} onChange={(e) => setConfig((prev) => ({ ...prev, residentModelList: e.target.value }))} rows={5} /></label>
            </div>
            <div className="button-row top-gap"><button onClick={saveConfig}>저장</button><button className="secondary" onClick={resetConfig}>초기화</button></div>
          </AppCard>

          <AppCard title="6. 런타임 모델 우선순위" description="OLLAMA / VLLM / OPENAI 정책을 카테고리별로 설정합니다.">
            <div className="list-stack">
              {priorityTargets.map((target) => (
                <label className="field-label" key={target}>{priorityLabelMap[target]}
                  <select value={modelPriority[target]?.priority || 'OLLAMA_FIRST'} onChange={(e) => setModelPriority((prev) => ({ ...prev, [target]: { ...(prev[target] || {}), priority: e.target.value } }))}>
                    {priorityPolicyOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                  </select>
                </label>
              ))}
            </div>
            <div className="button-row top-gap"><button onClick={savePriority}>저장</button><button className="secondary" onClick={resetPriority}>초기화</button></div>
          </AppCard>
        </div>

        <div className="sub-panel top-gap">
          <h3>7. 상태 원본</h3>
          <div className="three-column-grid top-gap">
            <JsonBlock value={connection} />
            <JsonBlock value={providerStatus.vllm} />
            <JsonBlock value={providerStatus.openai} />
          </div>
        </div>
      </AppCard>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <div className="status-line">{status}</div>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
