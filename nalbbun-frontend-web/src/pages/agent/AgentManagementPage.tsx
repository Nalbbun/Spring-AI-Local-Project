import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { agentApi, settingsApi } from '../../services/settingsApi';

export function AgentManagementPage() {
  const [config, setConfig] = useState<any>(null);
  const [models, setModels] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('부산 맛집 추천');
  const [searchResult, setSearchResult] = useState<any>(null);
  const [agentQuestion, setAgentQuestion] = useState('제주도 3박4일 100만원 여행 일정 짜줘');
  const [agentResult, setAgentResult] = useState<any>(null);

  const load = async () => {
    const [cfg, runningModels] = await Promise.all([agentApi.getAgentConfig(), settingsApi.browseModels('RUNNING')]);
    setConfig(cfg);
    setModels(runningModels);
  };

  useEffect(() => { load().catch(() => undefined); }, []);

  return (
    <div className="page-stack">
      <AppCard title="에이전트 현황" description="legacy agent 화면의 에이전트 모델/현황 패널을 React 관리 화면으로 재구성했습니다." actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}>
        <div className="two-column-grid">
          <JsonBlock value={config} />
          <JsonBlock value={models} />
        </div>
      </AppCard>

      <div className="two-column-grid">
        <AppCard title="웹 검색 테스트 (Tavily)">
          <label className="field-label">검색 쿼리<input value={searchQuery} onChange={e => setSearchQuery(e.target.value)} /></label>
          <div className="button-row"><button onClick={() => agentApi.webSearch({ query: searchQuery }).then(setSearchResult).catch(err => setSearchResult({ error: err.message }))}>검색</button></div>
          <JsonBlock value={searchResult} />
        </AppCard>
        <AppCard title="에이전트 실행 테스트">
          <label className="field-label">질문<textarea rows={5} value={agentQuestion} onChange={e => setAgentQuestion(e.target.value)} /></label>
          <div className="button-row"><button onClick={() => agentApi.run({ category: 'TRAVEL', message: agentQuestion }).then(setAgentResult).catch(err => setAgentResult({ error: err.message }))}>실행</button><button className="secondary" onClick={() => agentApi.clearMemory().catch(() => undefined)}>대화 초기화</button></div>
          <JsonBlock value={agentResult} />
        </AppCard>
      </div>
    </div>
  );
}
