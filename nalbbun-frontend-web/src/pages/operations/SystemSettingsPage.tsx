import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { settingsApi } from '../../services/settingsApi';
import type { DebugRuntimeConfig, ExecutionMode, RagStatusResponse } from '../../types/api';

const runtimeModeOptions = ['RULE', 'LLM', 'HYBRID'];
const executionModeOptions: ExecutionMode[] = ['CHAT', 'RAG', 'AGENT', 'AUTO'];

const defaultForm: DebugRuntimeConfig = {
  resolverMode: 'HYBRID',
  generalParserMode: 'HYBRID',
  travelParserMode: 'HYBRID',
  devParserMode: 'HYBRID',
  miceParserMode: 'HYBRID',
  generalExecutionMode: 'CHAT',
  devExecutionMode: 'RAG',
  miceExecutionMode: 'CHAT',
  travelExecutionMode: 'AGENT',
  memoryStore: 'in-memory',
  activeMemoryStore: 'in-memory',
  requestedMemoryStore: 'in-memory',
  memoryServiceType: '',
  restartRequired: false,
  restartSupported: true,
  memoryStoreNotice: '',
  restartRequestedAt: '',
  lastAppliedAt: '',
  redisSessionTtlMinutes: 180,
  restartAction: '',
  availableMemoryStores: ['in-memory', 'jdbc', 'redis'],
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
  minChunkSizeChars: 120,
  minChunkLengthToEmbed: 10,
  maxNumChunks: 128,
  maxUploadFileCount: 20
};

const memoryStoreHints: Record<string, string> = {
  'in-memory': '서버 메모리 저장소입니다. 재시작 시 대화가 사라집니다.',
  jdbc: 'PostgreSQL 기반 영구 저장소입니다.',
  redis: 'Redis 기반 저장소입니다. TTL 정책과 함께 쓰기 좋습니다.'
};

const apiCrudNotice = 'API 키·프롬프트·운영 CRUD 테이블은 메모리 타입과 무관하게 API DB에서 동작합니다. 메모리 타입은 대화 메모리 저장소에만 적용됩니다.';

function ModeSelect({ label, value, onChange }: { label: string; value?: string; onChange: (value: string) => void }) {
  return (
    <label className="field-label">{label}
      <select value={value ?? 'HYBRID'} onChange={(e) => onChange(e.target.value)}>
        {runtimeModeOptions.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
    </label>
  );
}

function ExecutionModeSelect({ label, value, onChange }: { label: string; value?: string; onChange: (value: ExecutionMode) => void }) {
  return (
    <label className="field-label">{label}
      <select value={value ?? 'AUTO'} onChange={(e) => onChange(e.target.value as ExecutionMode)}>
        {executionModeOptions.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
    </label>
  );
}

const readCategoryFlag = (categories: Record<string, boolean> | undefined, key: 'GENERAL' | 'DEV' | 'MICE' | 'TRAVEL') => {
  if (!categories) return false;
  return Boolean(categories[key] ?? categories[key.toLowerCase()]);
};

export function SystemSettingsPage() {
  const [config, setConfig] = useState<DebugRuntimeConfig>(defaultForm);
  const [ragStatus, setRagStatus] = useState<RagStatusResponse | null>(null);
  const [ragForm, setRagForm] = useState(defaultRagForm);
  const [status, setStatus] = useState('설정 조회 전');

  const requestedMemoryStore = config.requestedMemoryStore ?? config.memoryStore ?? 'in-memory';
  const activeMemoryStore = config.activeMemoryStore ?? config.memoryStore ?? 'in-memory';
  const availableMemoryStores = config.availableMemoryStores?.length ? config.availableMemoryStores : ['in-memory', 'jdbc', 'redis'];

  const load = async () => {
    setStatus('설정 조회 중');
    try {
      const [runtimeConfig, currentRagStatus] = await Promise.all([
        settingsApi.getConfig(),
        settingsApi.getRagStatus()
      ]);

      setConfig({ ...defaultForm, ...runtimeConfig, requestedMemoryStore: runtimeConfig.requestedMemoryStore ?? runtimeConfig.memoryStore ?? 'in-memory', activeMemoryStore: runtimeConfig.activeMemoryStore ?? runtimeConfig.memoryStore ?? 'in-memory' });
      setRagStatus(currentRagStatus);
      setRagForm((prev) => ({
        ...prev,
        enabled: Boolean(currentRagStatus?.enabled),
        topK: Number(currentRagStatus?.topK ?? prev.topK),
        similarityThreshold: Number(currentRagStatus?.similarityThreshold ?? prev.similarityThreshold),
        includeCitations: Boolean(currentRagStatus?.includeCitations),
        generalEnabled: readCategoryFlag(currentRagStatus?.categories, 'GENERAL'),
        devEnabled: readCategoryFlag(currentRagStatus?.categories, 'DEV'),
        miceEnabled: readCategoryFlag(currentRagStatus?.categories, 'MICE'),
        travelEnabled: readCategoryFlag(currentRagStatus?.categories, 'TRAVEL'),
        chunkSize: Number(currentRagStatus?.ingest?.chunkSize ?? prev.chunkSize),
        minChunkSizeChars: Number(currentRagStatus?.ingest?.minChunkSizeChars ?? prev.minChunkSizeChars),
        minChunkLengthToEmbed: Number(currentRagStatus?.ingest?.minChunkLengthToEmbed ?? prev.minChunkLengthToEmbed),
        maxNumChunks: Number(currentRagStatus?.ingest?.maxNumChunks ?? prev.maxNumChunks),
        maxUploadFileCount: Number(currentRagStatus?.ingest?.maxUploadFileCount ?? prev.maxUploadFileCount)
      }));
      setStatus('설정 조회 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  useEffect(() => { load().catch(() => undefined); }, []);

  const saveConfig = async () => {
    setStatus('시스템 설정 저장 중');
    try {
      const next = await settingsApi.saveConfig({ ...config, memoryStore: requestedMemoryStore, requestedMemoryStore });
      setConfig({ ...defaultForm, ...next, requestedMemoryStore: next.requestedMemoryStore ?? next.memoryStore ?? 'in-memory', activeMemoryStore: next.activeMemoryStore ?? next.memoryStore ?? 'in-memory' });
      setStatus(next.restartRequired ? '시스템 설정 저장 완료 · 메모리 저장소 적용 대기' : '시스템 설정 저장 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const resetConfig = async () => {
    setStatus('시스템 설정 초기화 중');
    try {
      const next = await settingsApi.resetConfig();
      setConfig({ ...defaultForm, ...next, requestedMemoryStore: next.requestedMemoryStore ?? next.memoryStore ?? 'in-memory', activeMemoryStore: next.activeMemoryStore ?? next.memoryStore ?? 'in-memory' });
      setStatus('시스템 설정 초기화 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const applyMemoryStore = async () => {
    const ok = window.confirm('메모리 저장소 전환 시 기존 대화내용은 서로 마이그레이션되지 않습니다.\n현재 저장소의 대화 이력은 새 저장소로 자동 이전되지 않으며, 저장소별로 분리됩니다.\n계속 진행하시겠습니까?');
    if (!ok) return;
    setStatus('메모리 저장소 적용 중');
    try {
      const next = await settingsApi.applyMemoryStore();
      setConfig({ ...defaultForm, ...next, requestedMemoryStore: next.requestedMemoryStore ?? next.memoryStore ?? 'in-memory', activeMemoryStore: next.activeMemoryStore ?? next.memoryStore ?? 'in-memory' });
      setStatus('메모리 저장소 적용 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const saveRag = async () => {
    setStatus('RAG 설정 저장 중');
    try {
      const payload = {
        enabled: ragForm.enabled,
        topK: ragForm.topK,
        similarityThreshold: ragForm.similarityThreshold,
        includeCitations: ragForm.includeCitations,
        categories: {
          GENERAL: ragForm.generalEnabled,
          DEV: ragForm.devEnabled,
          MICE: ragForm.miceEnabled,
          TRAVEL: ragForm.travelEnabled
        },
        ingest: {
          chunkSize: ragForm.chunkSize,
          minChunkSizeChars: ragForm.minChunkSizeChars,
          minChunkLengthToEmbed: ragForm.minChunkLengthToEmbed,
          maxNumChunks: ragForm.maxNumChunks,
          maxUploadFileCount: ragForm.maxUploadFileCount
        }
      };
      const saved = await settingsApi.saveRagConfig(payload);
      setRagStatus(saved);
      setStatus('RAG 설정 저장 완료');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const ragCategoryText = useMemo(() => {
    const categories = ragStatus?.categories || {};
    return ['GENERAL', 'DEV', 'MICE', 'TRAVEL']
      .filter((key) => Boolean(categories[key] ?? categories[key.toLowerCase()]))
      .join(', ') || '(없음)';
  }, [ragStatus]);

  return (
    <div className="page-stack">
      <AppCard
        title="시스템 설정"
        description="카테고리 해석 방식, 기본 실행 모드, 메모리 저장소, RAG 연결 상태를 한 화면에서 확인합니다."
        actions={
          <div className="button-row">
            <button onClick={saveConfig}>시스템 설정 저장</button>
            <button className="secondary" onClick={resetConfig}>초기화</button>
            <button className="secondary" onClick={() => load().catch(() => undefined)}>다시 조회</button>
          </div>
        }
      >
        <div className="two-column-grid align-start">
          <div className="page-stack">
            <div className="sub-panel">
              <h3>해석 정책</h3>
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
              <h3>카테고리 기본 실행 모드</h3>
              <div className="form-grid two top-gap">
                <ExecutionModeSelect label="GENERAL 기본 모드" value={config.generalExecutionMode} onChange={(value) => setConfig((prev) => ({ ...prev, generalExecutionMode: value }))} />
                <ExecutionModeSelect label="DEV 기본 모드" value={config.devExecutionMode} onChange={(value) => setConfig((prev) => ({ ...prev, devExecutionMode: value }))} />
                <ExecutionModeSelect label="MICE 기본 모드" value={config.miceExecutionMode} onChange={(value) => setConfig((prev) => ({ ...prev, miceExecutionMode: value }))} />
                <ExecutionModeSelect label="TRAVEL 기본 모드" value={config.travelExecutionMode} onChange={(value) => setConfig((prev) => ({ ...prev, travelExecutionMode: value }))} />
              </div>
              <div className="notice-box top-gap">
                채팅창에서 실행 모드를 AUTO로 두면, 여기서 저장한 카테고리별 기본 실행 모드가 실제 적용됩니다.
              </div>
            </div>

            <div className="sub-panel">
              <h3>메모리 저장소</h3>
              <div className="form-grid two top-gap">
                <label className="field-label">Memory Store
                  <select value={requestedMemoryStore} onChange={(e) => setConfig((prev) => ({ ...prev, memoryStore: e.target.value, requestedMemoryStore: e.target.value }))}>
                    {availableMemoryStores.map((store) => <option key={store} value={store}>{store}</option>)}
                  </select>
                </label>
                <label className="field-label">Memory Service Type
                  <input value={config.memoryServiceType ?? ''} readOnly placeholder="현재 활성 메모리 서비스 타입" />
                </label>
              </div>
              <div className="list-stack top-gap">
                <div className="list-item-row"><span>현재 적용 값</span><StatusBadge label={activeMemoryStore} tone="success" /></div>
                <div className="list-item-row"><span>수정 요청 값</span><StatusBadge label={requestedMemoryStore} tone={config.restartRequired ? 'warning' : 'info'} /></div>
                <div className="list-item-row"><span>적용 상태</span><StatusBadge label={config.restartRequired ? '적용 대기' : '적용 완료'} tone={config.restartRequired ? 'warning' : 'success'} /></div>
                <div className="list-item-row"><span>Redis Session TTL</span><span>{config.redisSessionTtlMinutes ?? 180}분</span></div>
                <div className="list-item-row"><span>마지막 적용</span><span>{config.lastAppliedAt || '-'}</span></div>
                <div className="list-item-row"><span>마지막 요청</span><span>{config.restartRequestedAt || '-'}</span></div>
              </div>
              <div className="notice-box top-gap">{memoryStoreHints[requestedMemoryStore] || '저장소 설명이 없습니다.'}</div>
              <div className="notice-box top-gap">{config.memoryStoreNotice || '저장소 타입이 바뀔 때마다 기존 대화내용은 서로 마이그레이션되지 않습니다.'}</div>
              <div className="notice-box top-gap">{apiCrudNotice}</div>
              <div className="button-row">
                <button className="secondary" onClick={saveConfig}>수정 요청 저장</button>
                <button onClick={applyMemoryStore} disabled={!config.restartRequired}>재시작 적용</button>
              </div>
            </div>
          </div>

          <div className="page-stack">
            <div className="sub-panel">
              <h3>현재 상태</h3>
              <div className="list-stack top-gap">
                <div className="list-item-row"><span>Resolver</span><StatusBadge label={config.resolverMode || '-'} tone="info" /></div>
                <div className="list-item-row"><span>GENERAL</span><StatusBadge label={String(config.generalExecutionMode || '-')} tone="success" /></div>
                <div className="list-item-row"><span>DEV</span><StatusBadge label={String(config.devExecutionMode || '-')} tone="success" /></div>
                <div className="list-item-row"><span>MICE</span><StatusBadge label={String(config.miceExecutionMode || '-')} tone="success" /></div>
                <div className="list-item-row"><span>TRAVEL</span><StatusBadge label={String(config.travelExecutionMode || '-')} tone="success" /></div>
                <div className="list-item-row"><span>Memory Active</span><StatusBadge label={activeMemoryStore || '-'} tone="success" /></div>
                <div className="list-item-row"><span>Memory Requested</span><StatusBadge label={requestedMemoryStore || '-'} tone={config.restartRequired ? 'warning' : 'info'} /></div>
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

      <AppCard title="RAG 런타임 설정" description="RAG 검색/청킹 설정을 한 화면에서 수정하고 저장합니다." actions={<button onClick={saveRag}>RAG 설정 저장</button>}>
        <div className="page-stack">
          <div className="sub-panel">
            <h3>검색 기본값</h3>
            <div className="form-grid three top-gap">
              <label className="field-label">Enabled
                <select value={String(ragForm.enabled)} onChange={(e) => setRagForm((prev) => ({ ...prev, enabled: e.target.value === 'true' }))}>
                  <option value="true">true</option>
                  <option value="false">false</option>
                </select>
              </label>
              <label className="field-label">Top K
                <input type="number" min={1} max={20} value={ragForm.topK} onChange={(e) => setRagForm((prev) => ({ ...prev, topK: Number(e.target.value) || 1 }))} />
              </label>
              <label className="field-label">Similarity Threshold
                <input type="number" min={0} max={1} step={0.01} value={ragForm.similarityThreshold} onChange={(e) => setRagForm((prev) => ({ ...prev, similarityThreshold: Number(e.target.value) || 0 }))} />
              </label>
            </div>
          </div>

          <div className="sub-panel">
            <h3>청킹 / 적재 설정</h3>
            <div className="form-grid rag-ingest-grid top-gap">
              <label className="field-label">Chunk Size
                <input type="number" min={50} max={5000} value={ragForm.chunkSize} onChange={(e) => setRagForm((prev) => ({ ...prev, chunkSize: Number(e.target.value) || 50 }))} />
              </label>
              <label className="field-label">Min Chunk Size Chars
                <input type="number" min={1} max={5000} value={ragForm.minChunkSizeChars} onChange={(e) => setRagForm((prev) => ({ ...prev, minChunkSizeChars: Number(e.target.value) || 1 }))} />
              </label>
              <label className="field-label">Min Chunk Length To Embed
                <input type="number" min={1} max={5000} value={ragForm.minChunkLengthToEmbed} onChange={(e) => setRagForm((prev) => ({ ...prev, minChunkLengthToEmbed: Number(e.target.value) || 1 }))} />
              </label>
              <label className="field-label">Max Num Chunks
                <input type="number" min={1} max={5000} value={ragForm.maxNumChunks} onChange={(e) => setRagForm((prev) => ({ ...prev, maxNumChunks: Number(e.target.value) || 1 }))} />
              </label>
              <label className="field-label">Max Upload File Count
                <input type="number" min={1} max={200} value={ragForm.maxUploadFileCount} onChange={(e) => setRagForm((prev) => ({ ...prev, maxUploadFileCount: Number(e.target.value) || 1 }))} />
              </label>
            </div>
          </div>

          <div className="sub-panel">
            <h3>카테고리 / 인용 옵션</h3>
            <div className="rag-toggle-grid top-gap">
              <label className="checkbox-label"><input type="checkbox" checked={ragForm.includeCitations} onChange={(e) => setRagForm((prev) => ({ ...prev, includeCitations: e.target.checked }))} /> Include Citations</label>
              <label className="checkbox-label"><input type="checkbox" checked={ragForm.generalEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, generalEnabled: e.target.checked }))} /> GENERAL</label>
              <label className="checkbox-label"><input type="checkbox" checked={ragForm.devEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, devEnabled: e.target.checked }))} /> DEV</label>
              <label className="checkbox-label"><input type="checkbox" checked={ragForm.miceEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, miceEnabled: e.target.checked }))} /> MICE</label>
              <label className="checkbox-label"><input type="checkbox" checked={ragForm.travelEnabled} onChange={(e) => setRagForm((prev) => ({ ...prev, travelEnabled: e.target.checked }))} /> TRAVEL</label>
            </div>
          </div>
        </div>
        <div className="status-line top-gap">{status}</div>
        <JsonBlock value={{ currentConfig: config, ragStatus, ragDraft: ragForm }} />
      </AppCard>
    </div>
  );
}
