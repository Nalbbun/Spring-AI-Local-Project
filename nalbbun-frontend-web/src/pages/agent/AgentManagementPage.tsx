import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { agentApi, settingsApi } from '../../services/settingsApi';
import type { DebugOllamaConfig, DebugRuntimeConfig, OllamaModelInfo } from '../../types/api';

const categories = [
  { key: 'travelSearchModel', label: 'TRAVEL Search' },
  { key: 'travelPlanModel', label: 'TRAVEL Plan' },
  { key: 'generalModel', label: 'GENERAL' },
  { key: 'devModel', label: 'DEV' },
  { key: 'miceModel', label: 'MICE' }
] as const;

function parseAgentPayload(result: any) {
  const payload = result?.result?.payload ?? result?.payload ?? null;
  const summary = result?.result?.summary ?? result?.summary ?? '-';
  return { summary, payload };
}

export function AgentManagementPage() {
  const [config, setConfig] = useState<DebugRuntimeConfig | null>(null);
  const [ollamaConfig, setOllamaConfig] = useState<DebugOllamaConfig | null>(null);
  const [models, setModels] = useState<OllamaModelInfo[]>([]);
  const [searchQuery, setSearchQuery] = useState('부산 맛집 추천');
  const [searchResult, setSearchResult] = useState<any>(null);
  const [agentQuestion, setAgentQuestion] = useState('제주도 3박4일 100만원 여행 일정 짜줘');
  const [agentResult, setAgentResult] = useState<any>(null);
  const [editableConfig, setEditableConfig] = useState<DebugOllamaConfig | null>(null);
  const logs = useEventLog('agent-management-log', ['에이전트 관리 로그가 누적됩니다.']);

  const availableModels = useMemo(
    () => models.map((item) => item.name || item.model || '').filter(Boolean),
    [models]
  );

  const load = async () => {
    logs.append('에이전트 현황과 실행 모델을 조회합니다.');
    const [cfg, runningModels, modelConfig] = await Promise.all([
      agentApi.getAgentConfig(),
      settingsApi.browseModels('RUNNING'),
      agentApi.getOllamaConfig()
    ]);
    setConfig(cfg);
    setModels(runningModels);
    setOllamaConfig(modelConfig);
    setEditableConfig(modelConfig);
    logs.append(`조회 완료: running model ${runningModels.length}건`);
  };

  useEffect(() => {
    load().catch((error) => logs.append('초기 조회 실패', error instanceof Error ? error.message : String(error)));
  }, []);

  const saveModels = async () => {
    if (!editableConfig) return;
    logs.append('에이전트용 모델 매핑 저장을 시작합니다.', editableConfig);
    try {
      const saved = await settingsApi.saveOllamaConfig(editableConfig);
      setOllamaConfig(saved);
      setEditableConfig(saved);
      logs.append('에이전트용 모델 매핑 저장 완료', saved);
    } catch (error) {
      logs.append('에이전트용 모델 매핑 저장 실패', error instanceof Error ? error.message : String(error));
    }
  };

  const runSearch = async () => {
    logs.append('웹 검색 테스트 실행', { query: searchQuery });
    try {
      const result = await agentApi.webSearch({ query: searchQuery });
      setSearchResult(result);
      logs.append('웹 검색 테스트 완료');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setSearchResult({ error: message });
      logs.append('웹 검색 테스트 실패', message);
    }
  };

  const runAgent = async () => {
    logs.append('에이전트 실행 테스트 시작', { userMessage: agentQuestion, categoryType: 'TRAVEL', agentType: 'TRAVEL' });
    try {
      const response = await agentApi.run({ categoryType: 'TRAVEL', userMessage: agentQuestion, agentType: 'TRAVEL' });
      setAgentResult(response);
      const parsed = parseAgentPayload(response);
      logs.append(`에이전트 실행 완료: ${parsed.summary}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setAgentResult({ error: message });
      logs.append('에이전트 실행 실패', message);
    }
  };

  const clearMemory = async () => {
    try {
      await agentApi.clearMemory();
      logs.append('에이전트 대화 메모리 초기화 완료');
    } catch (error) {
      logs.append('에이전트 대화 메모리 초기화 실패', error instanceof Error ? error.message : String(error));
    }
  };

  const parsedResult = parseAgentPayload(agentResult);

  return (
    <div className="page-stack">
      <AppCard
        title="에이전트 현황"
        description="레거시 agent 화면처럼 현황, 모델 배정, 검색 테스트, 실행 테스트, 작업 로그를 한 화면으로 복원했습니다."
        actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Resolver</span><strong>{config?.resolverMode || '-'}</strong></div>
          <div className="stat-box"><span>Memory Store</span><strong>{config?.memoryStore || config?.memoryServiceType || '-'}</strong></div>
          <div className="stat-box"><span>Fallback</span><strong>{config?.fallbackPolicy || '-'}</strong></div>
          <div className="stat-box"><span>실행 모델 수</span><strong>{models.length}</strong></div>
        </div>
      </AppCard>

      <div className="two-column-grid wider-left">
        <AppCard title="에이전트 모델 배정" description="TRAVEL Search/Plan을 포함한 카테고리별 모델 배정을 여기서 바로 조정합니다.">
          <div className="form-grid two">
            {categories.map((item) => (
              <label className="field-label" key={item.key}>
                {item.label}
                <select
                  value={String(editableConfig?.[item.key] || '')}
                  onChange={(e) => setEditableConfig((prev) => ({ ...(prev || {}), [item.key]: e.target.value }))}
                >
                  <option value="">선택 안 함</option>
                  {availableModels.map((modelName) => <option key={modelName} value={modelName}>{modelName}</option>)}
                </select>
              </label>
            ))}
          </div>
          <div className="button-row">
            <button onClick={saveModels}>모델 배정 저장</button>
            <button className="secondary" onClick={() => setEditableConfig(ollamaConfig)}>원복</button>
          </div>
          <div className="status-line">실행 가능한 RUNNING 모델을 기반으로 콤보를 구성했습니다.</div>
        </AppCard>

        <AppCard title="현재 적용 상태">
          <div className="list-stack">
            <div className="list-item-row"><span>Conversation ID</span><span className="inline-mini-code">{config?.conversationId || '(없음)'}</span></div>
            <div className="list-item-row"><span>Travel Search Model</span><StatusBadge label={ollamaConfig?.travelSearchModel || '-'} tone="info" /></div>
            <div className="list-item-row"><span>Travel Plan Model</span><StatusBadge label={ollamaConfig?.travelPlanModel || '-'} tone="info" /></div>
            <div className="list-item-row"><span>일반/개발/MICE</span><span>{[ollamaConfig?.generalModel, ollamaConfig?.devModel, ollamaConfig?.miceModel].filter(Boolean).join(' / ') || '-'}</span></div>
          </div>
        </AppCard>
      </div>

      <div className="two-column-grid">
        <AppCard title="웹 검색 테스트" description="Tavily 연결 여부와 결과 형식을 빠르게 점검합니다.">
          <label className="field-label">검색 쿼리<input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} /></label>
          <div className="button-row"><button onClick={runSearch}>검색</button></div>
          <JsonBlock value={searchResult} />
        </AppCard>

        <AppCard title="에이전트 실행 테스트" description="운영자가 실제 질문을 넣고 summary/payload 결과를 바로 확인할 수 있게 구성했습니다.">
          <label className="field-label">질문<textarea rows={6} value={agentQuestion} onChange={(e) => setAgentQuestion(e.target.value)} /></label>
          <div className="button-row">
            <button onClick={runAgent}>실행</button>
            <button className="secondary" onClick={() => setAgentResult(null)}>화면 지우기</button>
            <button className="danger" onClick={clearMemory}>대화 초기화</button>
          </div>
          <div className="sub-panel top-gap">
            <h3>실행 요약</h3>
            <div className="list-stack top-gap">
              <div className="list-item-row"><span>Summary</span><span>{parsedResult.summary || '-'}</span></div>
              <div className="list-item-row"><span>Payload Type</span><span>{parsedResult.payload ? typeof parsedResult.payload : '-'}</span></div>
            </div>
          </div>
          <JsonBlock value={agentResult} />
        </AppCard>
      </div>

      <AppCard title="원본 설정 JSON" description="문제 확인이 빠르도록 런타임 설정과 모델 설정 원본도 함께 노출합니다.">
        <div className="two-column-grid">
          <JsonBlock value={config} />
          <JsonBlock value={ollamaConfig} />
        </div>
      </AppCard>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
