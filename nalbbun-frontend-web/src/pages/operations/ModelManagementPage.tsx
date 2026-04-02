import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { notifyGlobal } from '../../lib/uiFeedback';
import { keyApi } from '../../services/keyApi';
import { settingsApi } from '../../services/settingsApi';
import type {
  DebugApiLlmConnectionInfo,
  DebugApiLlmProviderConfig,
  DebugOllamaConfig,
  DebugOllamaConnectionInfo,
  ModelPriorityItem,
  OllamaModelInfo
} from '../../types/api';

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
    healthCheckMethod: 'GET',
    modelsPath: '/v1/models',
    modelsMethod: 'GET',
    sllmPath: '/sllm',
    llmPath: '/llm',
    embeddingPath: '/embedding/api',
    rerankPath: '/rerank',
    searchModel: 'exaone-3.5-2.4b-it',
    answerModel: 'exaone-3.5-32b-it',
    embeddingModel: 'BAAI/bge-m3',
    rerankModel: 'BAAI/bge-reranker-v2-m3'
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
type OllamaConfigFieldKey = 'generalModel' | 'devModel' | 'miceModel' | 'travelSearchModel' | 'travelPlanModel';

function formatModelListPreview(models?: string[]) {
  if (!models || models.length === 0) return '-';
  const joined = models.join(', ');
  return joined.length <= MAX_PROVIDER_MODEL_PREVIEW ? joined : `${joined.slice(0, MAX_PROVIDER_MODEL_PREVIEW)}...`;
}

function formatSelectableModel(value?: string) {
  const raw = String(value || '').trim();
  if (!raw) return '-';
  if (raw.startsWith('OLLAMA::')) return `[OLLAMA] ${raw.slice('OLLAMA::'.length)}`;
  if (raw.startsWith('VLLM::')) return `[VLLM] ${raw.slice('VLLM::'.length)}`;
  if (raw.startsWith('OPENAI::')) return `[OPENAI] ${raw.slice('OPENAI::'.length)}`;
  return `[OLLAMA] ${raw}`;
}

function buildModelSelectOptions(availableModels: string[], currentValue?: string) {
  const options = Array.from(new Set((availableModels || []).map((item) => item?.trim()).filter(Boolean))) as string[];
  if (currentValue && !options.includes(currentValue)) {
    options.unshift(currentValue);
  }
  return options;
}

function LoadingBar({ active, label }: { active: boolean; label?: string }) {
  if (!active) return null;
  return (
    <div className="loading-block top-gap" role="status" aria-live="polite">
      <div className="loading-bar-track">
        <div className="loading-bar-fill" />
      </div>
      <div className="loading-bar-label">{label || '처리 중입니다...'}</div>
    </div>
  );
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

  return (
    <label className="field-label">
      {label}
      <select value={value || ''} onChange={(e) => onChange({ ...config, [field]: e.target.value })}>
        <option value="">없음</option>
        {options.map((name) => (
          <option key={name} value={name}>{formatSelectableModel(name)}</option>
        ))}
      </select>
    </label>
  );
}

function ProviderCard({
  providerKey,
  title,
  description,
  form,
  status,
  onChange,
  onCheck,
  onSave,
  onReset,
  providerOptions
}: {
  providerKey: 'openai' | 'vllm';
  title: string;
  description: string;
  form: DebugApiLlmProviderConfig;
  status: DebugApiLlmConnectionInfo | null;
  onChange: (next: DebugApiLlmProviderConfig) => void;
  onCheck: () => void;
  onSave: () => void;
  onReset: () => void;
  providerOptions: string[];
}) {
  const isVllm = providerKey === 'vllm';

  return (
    <AppCard title={title} description={description}>
      <div className="provider-grid top-gap">
        <label className="field-label">
          Base URL
          <input
            value={form.baseUrl || ''}
            onChange={(e) => onChange({ ...form, baseUrl: e.target.value })}
            placeholder={isVllm ? 'http://192.168.0.208:9000' : 'https://api.openai.com'}
          />
        </label>
        <label className="field-label">
          Default Model
          <input value={form.defaultModel || ''} onChange={(e) => onChange({ ...form, defaultModel: e.target.value })} />
        </label>
        <label className="field-label">
          키 제공자
          <select value={form.keyProvider || ''} onChange={(e) => onChange({ ...form, keyProvider: e.target.value })}>
            <option value="">선택</option>
            {providerOptions.map((item) => <option key={item} value={item}>{item}</option>)}
          </select>
        </label>
        <label className="field-label">
          Health Path
          <input value={form.healthCheckPath || ''} onChange={(e) => onChange({ ...form, healthCheckPath: e.target.value })} />
        </label>
        <label className="field-label">
          Health Method
          <select value={form.healthCheckMethod || 'GET'} onChange={(e) => onChange({ ...form, healthCheckMethod: e.target.value })}>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
          </select>
        </label>
        <label className="field-label">
          Models Path
          <input value={form.modelsPath || ''} onChange={(e) => onChange({ ...form, modelsPath: e.target.value })} />
        </label>
        <label className="field-label">
          Models Method
          <select value={form.modelsMethod || 'GET'} onChange={(e) => onChange({ ...form, modelsMethod: e.target.value })}>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
          </select>
        </label>
        {isVllm && (
          <>
            <label className="field-label">sLLM Path<input value={form.sllmPath || ''} onChange={(e) => onChange({ ...form, sllmPath: e.target.value })} /></label>
            <label className="field-label">LLM Path<input value={form.llmPath || ''} onChange={(e) => onChange({ ...form, llmPath: e.target.value })} /></label>
            <label className="field-label">Embedding Path<input value={form.embeddingPath || ''} onChange={(e) => onChange({ ...form, embeddingPath: e.target.value })} /></label>
            <label className="field-label">Rerank Path<input value={form.rerankPath || ''} onChange={(e) => onChange({ ...form, rerankPath: e.target.value })} /></label>
            <label className="field-label">Search Model<input value={form.searchModel || ''} onChange={(e) => onChange({ ...form, searchModel: e.target.value })} /></label>
            <label className="field-label">Answer Model<input value={form.answerModel || ''} onChange={(e) => onChange({ ...form, answerModel: e.target.value })} /></label>
            <label className="field-label">Embedding Model<input value={form.embeddingModel || ''} onChange={(e) => onChange({ ...form, embeddingModel: e.target.value })} /></label>
            <label className="field-label">Rerank Model<input value={form.rerankModel || ''} onChange={(e) => onChange({ ...form, rerankModel: e.target.value })} /></label>
          </>
        )}
      </div>

      <div className="button-row top-gap">
        <button onClick={onCheck}>연결 확인</button>
        <button onClick={onSave}>저장</button>
        <button className="secondary" onClick={onReset}>초기화</button>
      </div>

      <div className="list-stack top-gap">
        <div className="list-item-row"><span>상태</span><StatusBadge label={status?.status || '-'} tone={status?.reachable ? 'success' : 'warning'} /></div>
        <div className="list-item-row"><span>메시지</span><span>{status?.message || '-'}</span></div>
        <div className="list-item-row"><span>키 조회</span><span>{String(status?.keyResolved ?? false)} / {status?.keyProvider || '-'}</span></div>
        <div className="list-item-row"><span>Health 체크</span><span>{status?.healthCheckMethod || '-'} {status?.healthCheckPath || '-'} / {String(status?.healthCheckOk ?? false)}</span></div>
        <div className="list-item-row"><span>Models 체크</span><span>{status?.modelsMethod || '-'} {status?.modelsPath || '-'} / {String(status?.modelsCheckOk ?? false)}</span></div>
        <div className="list-item-row"><span>Health URL</span><span title={status?.resolvedHealthUrl || '-'}>{status?.resolvedHealthUrl || '-'}</span></div>
        <div className="list-item-row"><span>Models URL</span><span title={status?.resolvedModelsUrl || '-'}>{status?.resolvedModelsUrl || '-'}</span></div>
        <div className="list-item-row"><span>모델 수</span><span>{status?.modelCount ?? 0}</span></div>
        <div className="list-item-row"><span>모델 목록</span><span title={(status?.availableModels || []).join(', ') || '-'}>{formatModelListPreview(status?.availableModels)}</span></div>
        {isVllm && (
          <>
            <div className="list-item-row"><span>sLLM URL</span><span title={status?.resolvedSllmUrl || '-'}>{status?.resolvedSllmUrl || '-'}</span></div>
            <div className="list-item-row"><span>LLM URL</span><span title={status?.resolvedLlmUrl || '-'}>{status?.resolvedLlmUrl || '-'}</span></div>
            <div className="list-item-row"><span>Embedding URL</span><span title={status?.resolvedEmbeddingUrl || '-'}>{status?.resolvedEmbeddingUrl || '-'}</span></div>
            <div className="list-item-row"><span>Rerank URL</span><span title={status?.resolvedRerankUrl || '-'}>{status?.resolvedRerankUrl || '-'}</span></div>
          </>
        )}
      </div>
    </AppCard>
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
  const [busyMessage, setBusyMessage] = useState('');
  const [busyCount, setBusyCount] = useState(0);
  const logs = useEventLog('operations-model-management-log', ['모델 관리 작업 로그가 여기에 누적됩니다.']);

  const beginBusy = (message: string) => {
    setBusyMessage(message);
    setBusyCount((prev) => prev + 1);
  };

  const endBusy = () => {
    setBusyCount((prev) => Math.max(0, prev - 1));
  };


  const availableModelNames = useMemo(() => {
    const ollamaNames = models
      .map((item) => item.name || item.model || '')
      .map((name) => String(name || '').trim())
      .filter(Boolean)
      .map((name) => `OLLAMA::${name}`);
    const vllmNames = (providerStatus.vllm?.availableModels || [])
      .map((name) => String(name || '').trim())
      .filter(Boolean)
      .map((name) => `VLLM::${name}`);
    const openAiNames = (providerStatus.openai?.availableModels || [])
      .map((name) => String(name || '').trim())
      .filter(Boolean)
      .map((name) => `OPENAI::${name}`);
    return Array.from(new Set([...ollamaNames, ...vllmNames, ...openAiNames]));
  }, [models, providerStatus]);

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
      modelsMethod: providers?.vllm?.modelsMethod || providerDefaults.vllm.modelsMethod,
      sllmPath: providers?.vllm?.sllmPath || providerDefaults.vllm.sllmPath,
      llmPath: providers?.vllm?.llmPath || providerDefaults.vllm.llmPath,
      embeddingPath: providers?.vllm?.embeddingPath || providerDefaults.vllm.embeddingPath,
      rerankPath: providers?.vllm?.rerankPath || providerDefaults.vllm.rerankPath,
      searchModel: providers?.vllm?.searchModel || providerDefaults.vllm.searchModel,
      answerModel: providers?.vllm?.answerModel || providerDefaults.vllm.answerModel,
      embeddingModel: providers?.vllm?.embeddingModel || providerDefaults.vllm.embeddingModel,
      rerankModel: providers?.vllm?.rerankModel || providerDefaults.vllm.rerankModel
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
    const nextSource = String(cfg?.modelSource || source || 'RUNNING');
    setConfig({ ...configDefaults, ...cfg, modelSource: nextSource });
    setSource(nextSource);
    setConnection(conn);
    setBaseUrl(conn?.baseUrl ?? '');
    return { cfg, conn };
  };

  const loadModels = async (targetSource = source) => {
    const [priority, modelList] = await Promise.all([
      settingsApi.getModelPriority(),
      settingsApi.browseModels(targetSource),
      refreshProviders()
    ]);
    setModelPriority(priority);
    setModels(modelList);
    return { priority, modelList };
  };

  const loadAll = async (targetSource = source) => {
    beginBusy('전체 정보를 다시 조회하는 중입니다.');
    syncStatus('연결, 모델, 우선순위 정보를 다시 조회합니다.');
    try {
      const [{ conn }, { modelList }] = await Promise.all([loadConnection(), loadModels(targetSource)]);
      const message = `조회 완료: ${targetSource} 기준 ${modelList.length}개 모델 / reachable=${String(conn?.reachable ?? false)}`;
      syncStatus(message);
      notifyGlobal(message, 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`조회 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  useEffect(() => {
    loadAll(source).catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [source]);

  const saveConnection = async () => {
    if (!baseUrl.trim()) {
      notifyGlobal('Base URL을 입력한 뒤 저장하세요.', 'error');
      return;
    }
    beginBusy('Ollama 연결 URL을 저장하는 중입니다.');
    try {
      const result = await settingsApi.saveConnection({ baseUrl: baseUrl.trim() });
      setConnection(result);
      setBaseUrl(result.baseUrl ?? baseUrl.trim());
      syncStatus('연결 URL 저장 완료', result);
      notifyGlobal('Ollama 연결 URL이 저장되었습니다.', 'success');
      await loadAll(source);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`저장 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const checkConnection = async () => {
    beginBusy('Ollama 연결 상태를 확인하는 중입니다.');
    try {
      const result = await settingsApi.checkConnection();
      setConnection(result);
      syncStatus('연결 확인 완료', result);
      notifyGlobal('Ollama 연결 상태 확인이 완료되었습니다.', result?.reachable ? 'success' : 'info');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`연결 확인 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const resetConnection = async () => {
    beginBusy('Ollama 연결 정보를 초기화하는 중입니다.');
    try {
      await settingsApi.resetConnection();
      await loadAll(source);
      syncStatus('연결 정보 초기화 완료');
      notifyGlobal('Ollama 연결 정보가 초기화되었습니다.', 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`초기화 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const saveProvider = async (type: 'vllm' | 'openai') => {
    const form = type === 'vllm' ? vllmForm : openAiForm;
    beginBusy(`${type.toUpperCase()} 연결 정보를 저장하는 중입니다.`);
    try {
      const result = type === 'vllm' ? await settingsApi.saveVllmConfig(form) : await settingsApi.saveOpenAiConfig(form);
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      if (type === 'vllm') {
        setVllmForm({ ...providerDefaults.vllm, ...form, ...result });
      } else {
        setOpenAiForm({ ...providerDefaults.openai, ...form, ...result });
      }
      syncStatus(`${type.toUpperCase()} 연결 정보 저장 완료`, result);
      notifyGlobal(`${type.toUpperCase()} 설정이 저장되었습니다.`, 'success');
      await loadModels(source);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`${type.toUpperCase()} 저장 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const resetProvider = async (type: 'vllm' | 'openai') => {
    beginBusy(`${type.toUpperCase()} 설정을 초기화하는 중입니다.`);
    try {
      const result = type === 'vllm' ? await settingsApi.resetVllmConfig() : await settingsApi.resetOpenAiConfig();
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      if (type === 'vllm') {
        setVllmForm({ ...providerDefaults.vllm, ...result });
      } else {
        setOpenAiForm({ ...providerDefaults.openai, ...result });
      }
      syncStatus(`${type.toUpperCase()} 초기화 완료`, result);
      notifyGlobal(`${type.toUpperCase()} 설정이 초기화되었습니다.`, 'success');
      await loadModels(source);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`${type.toUpperCase()} 초기화 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const checkProvider = async (type: 'vllm' | 'openai') => {
    beginBusy(`${type.toUpperCase()} 연결 상태를 확인하는 중입니다.`);
    try {
      const result = type === 'vllm' ? await settingsApi.getVllmStatus() : await settingsApi.getOpenAiStatus();
      setProviderStatus((prev) => ({ ...prev, [type]: result }));
      syncStatus(`${type.toUpperCase()} 연결 확인 완료`, result);
      notifyGlobal(`${type.toUpperCase()} 연결 상태 확인이 완료되었습니다.`, result?.reachable ? 'success' : 'info');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`${type.toUpperCase()} 연결 확인 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const saveConfig = async () => {
    beginBusy('카테고리별 모델 구성을 저장하는 중입니다.');
    try {
      const payload = { ...config, modelSource: config.modelSource || source || 'RUNNING' };
      const saved = await settingsApi.saveOllamaConfig(payload);
      setConfig({ ...configDefaults, ...saved, modelSource: payload.modelSource });
      syncStatus('카테고리별 모델 저장 완료', saved);
      notifyGlobal('카테고리별 모델 설정이 저장되었습니다.', 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`카테고리 설정 저장 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const resetConfig = async () => {
    beginBusy('카테고리별 모델 구성을 초기화하는 중입니다.');
    try {
      const resetResult = await settingsApi.resetOllamaConfig();
      const nextSource = String(resetResult?.modelSource || 'RUNNING');
      setConfig({ ...configDefaults, ...resetResult, modelSource: nextSource });
      setSource(nextSource);
      syncStatus('카테고리별 모델 초기화 완료', resetResult);
      notifyGlobal('카테고리별 모델 설정이 초기화되었습니다.', 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`카테고리 설정 초기화 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const savePriority = async () => {
    const payload = Object.fromEntries(priorityTargets.map((target) => [target, modelPriority[target]?.priority || 'OLLAMA_FIRST']));
    beginBusy('런타임 모델 우선순위를 저장하는 중입니다.');
    try {
      const saved = await settingsApi.saveModelPriority(payload);
      setModelPriority(saved);
      syncStatus('런타임 모델 우선순위 정책 저장 완료', saved);
      notifyGlobal('런타임 모델 우선순위가 저장되었습니다.', 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`우선순위 저장 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const resetPriority = async () => {
    beginBusy('런타임 모델 우선순위를 초기화하는 중입니다.');
    try {
      const resetResult = await settingsApi.resetModelPriority();
      setModelPriority(resetResult);
      syncStatus('런타임 모델 우선순위 초기화 완료', resetResult);
      notifyGlobal('런타임 모델 우선순위가 초기화되었습니다.', 'success');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`우선순위 초기화 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  const runAction = async (override?: { model?: string; pull?: boolean }) => {
    const model = (override?.model ?? actionModelName).trim();
    const pull = override?.pull ?? actionPull === 'true';
    if (!model) {
      notifyGlobal('작업할 모델명을 입력하세요.', 'error');
      return;
    }
    beginBusy(`모델 ${pull ? 'Pull' : 'Run'} 작업을 실행하는 중입니다.`);
    try {
      const result = await settingsApi.modelAction({ model, pull, keepAlive: actionKeepAlive || undefined });
      syncStatus(`실행 완료: ${result.message || result.action || 'ok'}`, result);
      notifyGlobal(`모델 작업이 완료되었습니다: ${model}`, 'success');
      await loadAll(source);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      syncStatus(message);
      notifyGlobal(`모델 작업 실패: ${message}`, 'error');
    } finally {
      endBusy();
    }
  };

  return (
    <div className="page-stack">

      <AppCard
        title="모델 관리"
        description="화면을 세로 순차 구조로 정리했습니다. Ollama, OpenAI, vLLM, 카테고리별 모델, 우선순위, 로그를 한 흐름에서 관리합니다."
        actions={<div className="button-row compact"><button className="secondary" onClick={() => loadAll(source).catch(() => undefined)}>전체 새로고침</button></div>}
      >
        <LoadingBar active={busyCount > 0} label={busyMessage} />
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Ollama Base URL</span><strong>{connection?.baseUrl || '-'}</strong></div>
          <div className="stat-box"><span>Ollama Reachable</span><strong>{String(connection?.reachable ?? false)}</strong></div>
          <div className="stat-box"><span>조회 모델 수</span><strong>{models.length}</strong></div>
          <div className="stat-box"><span>조회 대상</span><strong>{source}</strong></div>
        </div>
      </AppCard>

      <AppCard title="1. Ollama" description="연결, 모델 조회, 실행을 한 영역에서 관리합니다.">
        <div className="provider-grid top-gap">
          <label className="field-label">Ollama Base URL<input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} placeholder="http://127.0.0.1:11434" /></label>
          <label className="field-label">작업 모델명<input list="model-name-options" value={actionModelName} onChange={(e) => setActionModelName(e.target.value)} placeholder="예: qwen2.5-coder:14b" /></label>
          <label className="field-label">작업 유형<select value={actionPull} onChange={(e) => setActionPull(e.target.value)}><option value="false">Run</option><option value="true">Pull</option></select></label>
          <label className="field-label">Keep Alive<input value={actionKeepAlive} onChange={(e) => setActionKeepAlive(e.target.value)} placeholder="24h" /></label>
        </div>
        <datalist id="model-name-options">
          {availableModelNames.map((name) => <option key={name} value={name} />)}
        </datalist>
        <div className="button-row top-gap">
          <button onClick={checkConnection}>연결 확인</button>
          <button onClick={saveConnection}>URL 저장</button>
          <button className="secondary" onClick={resetConnection}>초기화</button>
          <button className="secondary" onClick={() => loadModels(source).catch(() => undefined)}>모델 목록 새로고침</button>
          <button onClick={() => runAction()}>실행</button>
        </div>
        <div className="list-stack top-gap">
          <div className="list-item-row"><span>상태</span><StatusBadge label={connection?.status || '-'} tone={connection?.reachable ? 'success' : 'warning'} /></div>
          <div className="list-item-row"><span>메시지</span><span>{connection?.message || '-'}</span></div>
          <div className="list-item-row"><span>실행 모델</span><span>{(connection?.runningModels || []).join(', ') || '(없음)'}</span></div>
          <div className="list-item-row"><span>Running / Installed</span><span>{connection?.runningCount ?? 0} / {connection?.installedCount ?? 0}</span></div>
        </div>
        <div className="toolbar top-gap">
          <select value={source} onChange={(e) => { const nextSource = e.target.value; setSource(nextSource); setConfig((prev) => ({ ...prev, modelSource: nextSource })); }}>
            {sourceOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
        </div>
        <div className="top-gap">
          <DataTable<OllamaModelInfo>
            rows={models}
            columns={[
              { key: 'name', title: '모델명', render: (row) => row.name || row.model || '-' },
              { key: 'state', title: '상태', render: (row) => row.state || '-' },
              { key: 'size', title: '크기', render: (row) => row.size || '-' },
              { key: 'actions', title: '작업', render: (row) => <div className="button-row compact"><button className="secondary" onClick={() => runAction({ model: row.name || row.model || '', pull: false })}>실행</button><button className="secondary" onClick={() => runAction({ model: row.name || row.model || '', pull: true })}>Pull</button></div> }
            ]}
          />
        </div>
      </AppCard>

      <ProviderCard
        providerKey="openai"
        title="2. OpenAI / OpenAPI"
        description="OpenAI 호환 API 연결 정보를 저장하고 상태를 확인합니다."
        form={openAiForm}
        status={providerStatus.openai || null}
        onChange={setOpenAiForm}
        onCheck={() => checkProvider('openai')}
        onSave={() => saveProvider('openai')}
        onReset={() => resetProvider('openai')}
        providerOptions={keyProviderOptions}
      />

      <ProviderCard
        providerKey="vllm"
        title="3. vLLM"
        description="vLLM 연결, 모델 조회, sLLM/LLM/Embedding/Rerank 경로를 관리합니다."
        form={vllmForm}
        status={providerStatus.vllm || null}
        onChange={setVllmForm}
        onCheck={() => checkProvider('vllm')}
        onSave={() => saveProvider('vllm')}
        onReset={() => resetProvider('vllm')}
        providerOptions={keyProviderOptions}
      />

      <AppCard title="4. 카테고리별 모델 설정" description="Model Source를 바꾸면 Ollama 모델 목록도 함께 다시 조회됩니다. Resident Keep Alive / Resident Model List 항목은 화면에서 제거했습니다.">
        <div className="provider-grid top-gap">
          <label className="field-label">Model Source<select value={config.modelSource || 'RUNNING'} onChange={(e) => { const nextSource = e.target.value; setConfig((prev) => ({ ...prev, modelSource: nextSource })); setSource(nextSource); }}>{sourceOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>
          <div className="notice-box">선택 가능한 모델은 현재 Model Source 기준 Ollama 모델 + OpenAI + vLLM 모델을 합쳐서 표시합니다.</div>
          <CategoryModelSelect label="GENERAL" field="generalModel" value={config.generalModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
          <CategoryModelSelect label="DEV" field="devModel" value={config.devModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
          <CategoryModelSelect label="MICE" field="miceModel" value={config.miceModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
          <CategoryModelSelect label="TRAVEL_SEARCH" field="travelSearchModel" value={config.travelSearchModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
          <CategoryModelSelect label="TRAVEL_PLAN" field="travelPlanModel" value={config.travelPlanModel || ''} config={config} availableModels={availableModelNames} onChange={setConfig} />
        </div>
        <div className="button-row top-gap"><button onClick={saveConfig}>저장</button><button className="secondary" onClick={resetConfig}>초기화</button></div>
      </AppCard>

      <AppCard title="5. 런타임 모델 우선순위" description="OLLAMA / VLLM / OPENAI 정책을 카테고리별로 설정합니다.">
        <div className="provider-grid top-gap">
          {priorityTargets.map((target) => (
            <label className="field-label" key={target}>
              {priorityLabelMap[target]}
              <select value={modelPriority[target]?.priority || 'OLLAMA_FIRST'} onChange={(e) => setModelPriority((prev) => ({ ...prev, [target]: { ...(prev[target] || {}), priority: e.target.value } }))}>
                {priorityPolicyOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
            </label>
          ))}
        </div>
        <div className="button-row top-gap"><button onClick={savePriority}>저장</button><button className="secondary" onClick={resetPriority}>초기화</button></div>
      </AppCard>

      <AppCard title="6. 작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <div className="status-line">{status}</div>
        <LogPanel lines={logs.lines} />
      </AppCard>

      <AppCard title="7. 상태 로그" description="연결 상태 원본과 현재 저장된 설정 값을 확인합니다.">
        <div className="three-column-grid top-gap">
          <JsonBlock value={connection} />
          <JsonBlock value={providerStatus.openai} />
          <JsonBlock value={providerStatus.vllm} />
        </div>
      </AppCard>
    </div>
  );
}
