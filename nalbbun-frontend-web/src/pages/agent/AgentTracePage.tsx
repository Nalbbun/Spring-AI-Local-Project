import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { useEffect, useState } from 'react';
import { settingsApi } from '../../services/settingsApi';

export function AgentTracePage() {
  const [config, setConfig] = useState<any>(null);
  const [ollama, setOllama] = useState<any>(null);

  useEffect(() => {
    Promise.all([settingsApi.getConfig(), settingsApi.getOllamaConfig()])
      .then(([cfg, ol]) => { setConfig(cfg); setOllama(ol); })
      .catch(() => undefined);
  }, []);

  return (
    <div className="page-stack">
      <AppCard title="에이전트 실행 추적" description="현재 백엔드에 별도 trace 조회 API가 없는 구간은 원본 설정/상태 JSON을 기준으로 운영자가 점검할 수 있도록 구성했습니다.">
        <div className="two-column-grid">
          <JsonBlock value={config} />
          <JsonBlock value={ollama} />
        </div>
      </AppCard>
    </div>
  );
}
