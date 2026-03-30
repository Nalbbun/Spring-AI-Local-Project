import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { useAsyncData } from '../../hooks/useAsyncData';
import { settingsApi } from '../../services/settingsApi';

export function SystemSettingsPage() {
  const config = useAsyncData(() => settingsApi.getConfig());
  const ragStatus = useAsyncData(() => settingsApi.getRagStatus());

  return (
    <div className="page-stack">
      <AppCard title="시스템 설정" description="legacy settings/debug 화면에서 공통 런타임 설정과 기본 정보를 먼저 분리한 영역입니다." actions={<div className="button-row compact"><button className="secondary" onClick={() => config.refresh().catch(() => undefined)}>설정 새로고침</button><button className="secondary" onClick={() => settingsApi.resetConfig().then(() => config.refresh()).catch(() => undefined)}>설정 초기화</button></div>}>
        <JsonBlock value={config.data} />
      </AppCard>
      <AppCard title="RAG 상태 요약" actions={<button className="secondary" onClick={() => ragStatus.refresh().catch(() => undefined)}>새로고침</button>}>
        <JsonBlock value={ragStatus.data} />
      </AppCard>
    </div>
  );
}
