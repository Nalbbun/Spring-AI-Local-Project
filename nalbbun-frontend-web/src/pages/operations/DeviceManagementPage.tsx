import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { JsonBlock } from '../../components/ui/JsonBlock';
import { settingsApi } from '../../services/settingsApi';

export function DeviceManagementPage() {
  const [config, setConfig] = useState<any>(null);
  const [connection, setConnection] = useState<any>(null);
  const [baseUrl, setBaseUrl] = useState('');
  const [status, setStatus] = useState('대기 중');

  const load = async () => {
    const [cfg, conn] = await Promise.all([settingsApi.getOllamaConfig(), settingsApi.checkConnection()]);
    setConfig(cfg);
    setConnection(conn);
    setBaseUrl(cfg?.baseUrl ?? cfg?.ollamaBaseUrl ?? '');
  };

  useEffect(() => { load().catch(() => undefined); }, []);

  const save = async () => {
    setStatus('저장 중');
    try {
      await settingsApi.saveOllamaConfig({ baseUrl });
      setStatus('저장 완료');
      await load();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <AppCard title="외부장치 관리" description="legacy settings 화면의 Ollama 연결 정보를 별도 장치 관리 영역으로 분리했습니다.">
        <div className="form-grid two">
          <label className="field-label">Base URL<input value={baseUrl} onChange={e => setBaseUrl(e.target.value)} placeholder="http://127.0.0.1:11434" /></label>
          <div className="field-label"><span>연결 상태</span><div className="inline-code">{JSON.stringify(connection)}</div></div>
        </div>
        <div className="button-row"><button onClick={save}>URL 저장</button><button className="secondary" onClick={() => settingsApi.checkConnection().then(setConnection).catch(() => undefined)}>연결 확인</button><button className="secondary" onClick={() => settingsApi.resetOllamaConfig().then(() => load()).catch(() => undefined)}>초기화</button></div>
        <div className="status-line">{status}</div>
      </AppCard>
      <div className="two-column-grid">
        <AppCard title="장치 설정 원본"><JsonBlock value={config} /></AppCard>
        <AppCard title="연결 진단"><JsonBlock value={connection} /></AppCard>
      </div>
    </div>
  );
}
