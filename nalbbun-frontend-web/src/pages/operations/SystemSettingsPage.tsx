import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { settingsApi } from '../../services/settingsApi';
import type { DebugRuntimeConfig, RagStatusResponse } from '../../types/api';

const runtimeModeOptions = ['RULE', 'LLM', 'HYBRID'];

const defaultForm: DebugRuntimeConfig = {
  resolverMode: 'HYBRID',
  generalParserMode: 'HYBRID',
  travelParserMode: 'HYBRID',
  devParserMode: 'HYBRID',
  miceParserMode: 'HYBRID',
  memoryStore: 'in-memory',
  memoryServiceType: '',
  fallbackPolicy: 'ALLOW_OPENAI',
  conversationId: '',
  ollamaBaseUrl: ''
};

const defaultRagForm = {
  enabled: false,
  topK: 4,
  similarityThreshold: 0.72,
  includeCitations: true,
  generalEnabled: false,
  devEnabled: true,
  miceEnabled: true,
  travelEnabled: false,
  chunkSize: 350,
  maxNumChunks: 128
};

const memoryStoreHints: Record<string, string> = {
  'in-memory': '서버 메모리 저장소입니다. 재시작 시 대화가 사라집니다.',
  jdbc: 'PostgreSQL 기반 영구 저장소입니다.',
  redis: 'Redis 기반 저장소입니다. TTL 정책과 함께 쓰기 좋습니다.'
};

function ModeSelect({ label, value, onChange }: { label: string; value?: string; onChange: (value: string) => void }) {
  return (
    <label className="field-label">{label}
      <select value={value ?? 'HYBRID'} onChange={(e) => onChange(e.target.value)}>
        {runtimeModeOptions.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
    </label>
  );
}

export function SystemSettingsPage() {
  const [config, setConfig] = useState<DebugRuntimeConfig>(defaultForm);
  const [ragStatus, setRagStatus] = useState<RagStatusResponse | null>(null);
  const [ragForm, setRagForm] = useState(defaultRagForm);
  const [status, setStatus] = useState('런타임 설정을 불러오는 중입니다.');
  const [rawConfig, setRawConfig] = useState<DebugRuntimeConfig | null>(null);

  const load = async () => {
    const [runtimeConfig, rag] = await Promise.all([
      settingsApi.getConfig(),
      settingsApi.getRagStatus()
    ]);
    setConfig({ ...defaultForm, ...runtimeConfig });
    setRawConfig(runtimeConfig);
    setRagStatus(rag);
    setRagForm({
      enabled: Boolean(rag.enabled),
      topK: rag.topK ?? 4,
      similarityThreshold: rag.similarityThreshold ?? 0.72,
      includeCitations: rag.includeCitations ?? true,
      generalEnabled: Boolean(rag.categories?.general),
      devEnabled: Boolean(rag.categories?.dev),
      miceEnabled: Boolean(rag.categories?.mice),
      travelEnabled: Boolean(rag.categories?.travel),
      chunkSize: rag.ingest?.chunkSize ?? 350,
      maxNumChunks: rag.ingest?.maxNumChunks ?? 128
    });
    setStatus('현재 런타임 설정을 불러왔습니다.');
  };

  useEffect(() => {
    load().catch((error) => setStatus(error instanceof Error ? error.message : String(error)));
  }, []);

  const save = async () => {
    setStatus('런타임 설정 저장 중');
    try {
      const saved = await settingsApi.saveConfig(config);
      setConfig({ ...defaultForm, ...saved });
      setRawConfig(saved);
      setStatus('런타임 설정 저장 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const reset = async () => {
    setStatus('기본값으로 되돌리는 중');
    try {
      const resetResult = await settingsApi.resetConfig();
      setConfig({ ...defaultForm, ...resetResult });
      setRawConfig(resetResult);
      setStatus('런타임 설정 초기화 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const saveRag = async () => {
    setStatus('RAG 설정 저장 중');
    try {
      const saved = await settingsApi.saveRagConfig(ragForm);
      setRagStatus(saved);
      setStatus('RAG 설정 저장 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const ragCategoryText = useMemo(() => {
    const categories = ragStatus?.categories ?? {};
    return `GEN:${String(categories.general ?? false)} / DEV:${String(categories.dev ?? false)} / MICE:${String(categories.mice ?? false)} / TRAVEL:${String(categories.travel ?? false)}`;
  }, [ragStatus]);

  return (
    <div className="page-stack">
      <AppCard
        title="시스템 설정"
        description="레거시 settings 화면의 런타임 설정 영역을 분리했습니다. Resolver, Parser, Memory Store, Fallback 정책을 먼저 안정화합니다."
        actions={
          <div className="button-row compact">
            <button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>
            <button onClick={save}>저장</button>
            <button className="secondary" onClick={reset}>초기화</button>
          </div>
        }
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Resolver</span><strong>{config.resolverMode || '-'}</strong></div>
          <div className="stat-box"><span>Memory Store</span><strong>{config.memoryStore || '-'}</strong></div>
          <div className="stat-box"><span>Fallback</span><strong>{config.fallbackPolicy || '-'}</strong></div>
          <div className="stat-box"><span>Conversation</span><strong>{config.conversationId || '-'}</strong></div>
        </div>

        <div className="two-column-grid wider-left top-gap">
          <div className="page-stack">
            <div className="sub-panel">
              <h3>기본 런타임 정책</h3>
              <div className="form-grid two top-gap">
                <ModeSelect label="Resolver Mode" value={config.resolverMode} onChange={(value) => setConfig((prev) => ({ ...prev, resolverMode: value }))} />
                <label className="field-label">Fallback Policy
                  <select value={config.fallbackPolicy ?? ''} onChange={(e) => setConfig((prev) => ({ ...prev, fallbackPolicy: e.target.value }))}>
                    <option value="ALLOW_OPENAI">ALLOW_OPENAI</option>
                    <option value="BLOCK_OPENAI">BLOCK_OPENAI</option>
                  </select>
                </label>
              </div>
            </div>

            <div className="sub-panel">
              <h3>카테고리 파서 모드</h3>
              <div className="form-grid two top-gap">
                <ModeSelect label="GENERAL Parser" value={config.generalParserMode} onChange={(value) => setConfig((prev) => ({ ...prev, generalParserMode: value }))} />
                <ModeSelect label="DEV Parser" value={config.devParserMode} onChange={(value) => setConfig((prev) => ({ ...prev, devParserMode: value }))} />
                <ModeSelect label="MICE Parser" value={config.miceParserMode} onChange={(value) => setConfig((prev) => ({ ...prev, miceParserMode: value }))} />
                <ModeSelect label="TRAVEL Parser" value={config.travelParserMode} onChange={(value) => setConfig((prev) => ({ ...prev, travelParserMode: value }))} />
              </div>
            </div>

            <div className="sub-panel">
              <h3>메모리 저장소</h3>
              <div className="form-grid two top-gap">
                <label className="field-label">Memory Store
                  <select value={config.memoryStore ?? 'in-memory'} onChange={(e) => setConfig((prev) => ({ ...prev, memoryStore: e.target.value }))}>
                    <option value="in-memory">in-memory</option>
                    <option value="jdbc">jdbc</option>
                    <option value="redis">redis</option>
                  </select>
                </label>
                <label className="field-label">Memory Service Type
                  <input value={config.memoryServiceType ?? ''} onChange={(e) => setConfig((prev) => ({ ...prev, memoryServiceType: e.target.value }))} placeholder="자동 감지 결과 또는 수동 표시" />
                </label>
              </div>
              <div className="notice-box top-gap">{memoryStoreHints[config.memoryStore || 'in-memory'] || '저장소 설명이 없습니다.'}</div>
            </div>
          </div>

          <div className="page-stack">
            <div className="sub-panel">
              <h3>현재 상태</h3>
              <div className="list-stack top-gap">
                <div className="list-item-row"><span>Resolver</span><StatusBadge label={config.resolverMode || '-'} tone="info" /></div>
                <div className="list-item-row"><span>Memory</span><StatusBadge label={config.memoryStore || '-'} tone="success" /></div>
                <div className="list-item-row"><span>Fallback</span><StatusBadge label={config.fallbackPolicy || '-'} tone="warning" /></div>
                <div className="list-item-row"><span>Conversation ID</span><span className="inline-mini-code">{config.conversationId || '(없음)'}</span></div>
              </div>
            </div>

            <div className="sub-panel">
              <h3>RAG 연계 상태</h3>
              <div className="list-stack top-gap">
                <div className="list-item-row"><span>RAG Enabled</span><StatusBadge label={ragStatus?.enabled ? 'ON' : 'OFF'} tone={ragStatus?.enabled ? 'success' : 'default'} /></div>
                <div className="list-item-row"><span>Vector Store</span><span>{ragStatus?.vectorStore || '-'}</span></div>
                <div className="list-item-row"><span>TopK / Threshold</span><span>{ragStatus?.topK ?? '-'} / {ragStatus?.similarityThreshold ?? '-'}</span></div>
                <div className="list-item-row"><span>Category Flags</span><span>{ragCategoryText}</span></div>
              </div>
            </div>
          </div>
        </div>

        <div className="status-line">{status}</div>
      </AppCard>

      <AppCard title="RAG 런타임 설정" description="레거시 settings의 RAG 설정 저장 기능을 이 화면으로 옮겼습니다." actions={<button onClick={saveRag}>RAG 설정 저장</button>}>
        <div className="form-grid three">
          <label className="field-label">Enabled
            <select value={String(ragForm.enabled)} onChange={(e) => setRagForm((prev) => ({ ...prev, enabled: e.target.value === 'true' }))}>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </label>
          <label className="field-label">TopK
            <input type="number" value={ragForm.topK} onChange={(e) => setRagForm((prev) => ({ ...prev, topK: Number(e.target.value) }))} />
          </label>
          <label className="field-label">Similarity Threshold
            <input type="number" step="0.01" value={ragForm.similarityThreshold} onChange={(e) => setRagForm((prev) => ({ ...prev, similarityThreshold: Number(e.target.value) }))} />
          </label>
          <label className="field-label">Include Citations
            <select value={String(ragForm.includeCitations)} onChange={(e) => setRagForm((prev) => ({ ...prev, includeCitations: e.target.value === 'true' }))}>
              <option value="true">true</option>
              <option value="false">false</option>
            </select>
          </label>
          <label className="field-label">Chunk Size
            <input type="number" value={ragForm.chunkSize} onChange={(e) => setRagForm((prev) => ({ ...prev, chunkSize: Number(e.target.value) }))} />
          </label>
          <label className="field-label">Max Num Chunks
            <input type="number" value={ragForm.maxNumChunks} onChange={(e) => setRagForm((prev) => ({ ...prev, maxNumChunks: Number(e.target.value) }))} />
          </label>
        </div>
        <div className="checkbox-row top-gap wrap-row">
          <label className="checkbox-label"><input type="checkbox" checked={ragForm.generalEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, generalEnabled: e.target.checked }))} /> GENERAL</label>
          <label className="checkbox-label"><input type="checkbox" checked={ragForm.devEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, devEnabled: e.target.checked }))} /> DEV</label>
          <label className="checkbox-label"><input type="checkbox" checked={ragForm.miceEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, miceEnabled: e.target.checked }))} /> MICE</label>
          <label className="checkbox-label"><input type="checkbox" checked={ragForm.travelEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, travelEnabled: e.target.checked }))} /> TRAVEL</label>
        </div>
      </AppCard>

      <AppCard title="원본 설정 JSON" description="디버그 원본을 같이 보여줘야 문제를 빨리 찾을 수 있습니다.">
        <JsonBlock value={{ runtime: rawConfig, ragStatus }} />
      </AppCard>
    </div>
  );
}
