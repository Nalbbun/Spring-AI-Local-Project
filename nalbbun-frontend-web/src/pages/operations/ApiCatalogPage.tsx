import { useAsyncData } from '../../hooks/useAsyncData';
import { fetchApiCatalog } from '../../services/catalogApi';
import { AppCard } from '../../components/ui/AppCard';

export function ApiCatalogPage() {
  const { data, refresh } = useAsyncData(fetchApiCatalog);

  return (
    <AppCard title="API 목록" description="백엔드가 제공하는 JSON/SSE 엔드포인트를 프론트에서 참조할 수 있도록 정리한 화면입니다." actions={<button className="secondary" onClick={() => refresh().catch(() => undefined)}>새로고침</button>}>
      {!data && <div className="empty-box">API catalog를 불러오지 못했습니다.</div>}
      {data?.groups.map(group => (
        <div key={group.name} className="sub-panel">
          <h3>{group.name}</h3>
          {group.endpoints.map(endpoint => (
            <div key={`${endpoint.method}-${endpoint.path}`} className="endpoint-row">
              <span className="method-pill">{endpoint.method}</span>
              <code>{endpoint.path}</code>
              <strong>{endpoint.title}</strong>
              <span className="muted">{endpoint.notes}</span>
            </div>
          ))}
        </div>
      ))}
    </AppCard>
  );
}
