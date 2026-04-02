import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { agentApi, settingsApi } from '../../services/settingsApi';
import type { DebugOllamaConfig, DebugRuntimeConfig, OllamaModelInfo, WebSearchStatus } from '../../types/api';


function parseAgentPayload(result: any) {
  const payload = result?.result?.payload ?? result?.payload ?? null;
  const summary = result?.result?.summary ?? result?.summary ?? '-';
  return { summary, payload };
}

export function AgentManagementPage() {
  const [config, setConfig] = useState<DebugRuntimeConfig | null>(null);
  const [ollamaConfig, setOllamaConfig] = useState<DebugOllamaConfig | null>(null);
  const [models, setModels] = useState<OllamaModelInfo[]>([]);
  const [webSearchStatus, setWebSearchStatus] = useState<WebSearchStatus | null>(null);
  const [searchQuery, setSearchQuery] = useState('부산 맛집 추천');
  const [searchResult, setSearchResult] = useState<any>(null);
  const [agentQuestion, setAgentQuestion] = useState('제주도 3박4일 100만원 여행 일정 짜줘');
  const [agentResult, setAgentResult] = useState<any>(null);
  const logs = useEventLog('agent-management-log', ['에이전트 관리 로그가 누적됩니다.']);


  const load = async () => {
    logs.append('에이전트 현황과 실행 모델을 조회합니다.');
    const [cfg, runningModels, modelConfig, searchStatus] = await Promise.all([
      agentApi.getAgentConfig(),
      settingsApi.browseModels('RUNNING'),
      agentApi.getOllamaConfig(),
      agentApi.getWebSearchStatus().catch(() => null)
    ]);
    setConfig(cfg);
    setModels(runningModels);
    setOllamaConfig(modelConfig);
    setWebSearchStatus(searchStatus);
    logs.append(`조회 완료: running model ${runningModels.length}건`);
  };

  useEffect(() => {
    load().catch((error) => logs.append('초기 조회 실패', error instanceof Error ? error.message : String(error)));
  }, []);


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
        description=" agent 화면처럼 현황, 검색 테스트, 실행 테스트, 작업 로그를 한 화면으로 복원했습니다. 모델 배정은 운영 > 모델 관리에서 설정합니다."
        actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Resolver</span><strong>{config?.resolverMode || '-'}</strong></div>
          <div className="stat-box"><span>Memory Store</span><strong>{config?.memoryStore || config?.memoryServiceType || '-'}</strong></div>
          <div className="stat-box"><span>Fallback</span><strong>{config?.fallbackPolicy || '-'}</strong></div>
          <div className="stat-box"><span>실행 모델 수</span><strong>{models.length}</strong></div>
        </div>
        <div className="stats-grid compact-four top-gap">
          <div className="stat-box"><span>Web Search Provider</span><strong>{webSearchStatus?.provider || '-'}</strong></div>
          <div className="stat-box"><span>Primary Endpoint</span><strong>{webSearchStatus?.primaryEndpointAvailable ? '정상' : '확인 필요'}</strong></div>
          <div className="stat-box"><span>Tavily Key</span><strong>{webSearchStatus?.hasTavilyActiveKey ? '활성' : '미확인'}</strong></div>
          <div className="stat-box"><span>Debug Endpoint</span><strong>{webSearchStatus?.legacyDebugEndpointAvailable ? '활성' : '비활성'}</strong></div>
        </div>
        <div className="list-stack top-gap">
          <div className="list-item-row"><span>Primary Endpoint 경로</span><span className="inline-mini-code">{webSearchStatus?.primaryEndpoint || '/api/agent/web-search-test'}</span></div>
          <div className="list-item-row"><span>Debug Fallback 경로</span><span className="inline-mini-code">{webSearchStatus?.legacyDebugEndpoint || '/debug/api/search'}</span></div>
          <div className="list-item-row"><span>Active Profiles</span><span>{webSearchStatus?.activeProfiles?.join(', ') || '-'}</span></div>
          <div className="list-item-row"><span>상태 메시지</span><span>{webSearchStatus?.message || '상태 조회 전'}</span></div>
        </div>
      </AppCard>

      <AppCard title="현재 적용 상태" description="에이전트 모델 배정 화면은 제거하고, 현재 적용 중인 런타임/모델 상태만 표시합니다. 실제 모델 배정은 운영 > 모델 관리에서 설정합니다.">
        <div className="list-stack">
          <div className="list-item-row"><span>Conversation ID</span><span className="inline-mini-code">{config?.conversationId || '(없음)'}</span></div>
          <div className="list-item-row"><span>Travel Search Model</span><StatusBadge label={ollamaConfig?.travelSearchModel || '-'} tone="info" /></div>
          <div className="list-item-row"><span>Travel Plan Model</span><StatusBadge label={ollamaConfig?.travelPlanModel || '-'} tone="info" /></div>
          <div className="list-item-row"><span>일반/개발/MICE</span><span>{[ollamaConfig?.generalModel, ollamaConfig?.devModel, ollamaConfig?.miceModel].filter(Boolean).join(' / ') || '-'}</span></div>
          <div className="list-item-row"><span>실행 가능한 모델 수</span><span>{models.length}</span></div>
        </div>
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="웹 검색 테스트" description="현재 provider, endpoint 상태를 확인한 뒤 실제 웹 검색을 바로 테스트합니다.">
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
