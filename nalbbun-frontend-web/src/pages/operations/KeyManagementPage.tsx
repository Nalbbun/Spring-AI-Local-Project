import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { keyApi } from '../../services/keyApi';
import type { ApiKeyEntry, ProviderStatus } from '../../types/api';

const issueMap: Record<string, string> = {
  OPENAI: 'https://platform.openai.com/api-keys',
  TAVILY: 'https://app.tavily.com',
  ANTHROPIC: 'https://console.anthropic.com'
};

export function KeyManagementPage() {
  const [providers, setProviders] = useState<ProviderStatus[]>([]);
  const [items, setItems] = useState<ApiKeyEntry[]>([]);
  const [filter, setFilter] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [revealedKey, setRevealedKey] = useState('');
  const [status, setStatus] = useState('대기 중');
  const [form, setForm] = useState({ provider: '', label: '', description: '', keyValue: '', active: true });

  const load = async () => {
    const [providerData, listData] = await Promise.all([keyApi.providers(), keyApi.list(filter || undefined)]);
    setProviders(providerData);
    setItems(listData);
  };

  useEffect(() => { load().catch(() => undefined); }, [filter]);

  const save = async () => {
    setStatus('저장 중');
    try {
      const payload: Record<string, unknown> = { provider: form.provider, label: form.label, description: form.description, active: form.active };
      if (form.keyValue) payload.keyValue = form.keyValue;
      const result = editingId ? await keyApi.update(editingId, payload) : await keyApi.create({ ...payload, keyValue: form.keyValue });
      setEditingId(result.id);
      setStatus(`저장 완료: ${result.label}`);
      setForm(prev => ({ ...prev, keyValue: '' }));
      await load();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  const providerHint = useMemo(() => issueMap[form.provider] || '', [form.provider]);

  return (
    <div className="page-stack">
      <AppCard title="프로바이더 현황" description="legacy api-keys 화면의 상태 카드와 발급 안내를 React 카드로 분리했습니다." actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}>
        <div className="provider-grid">
          {providers.map(provider => (
            <div className="provider-card" key={provider.provider}>
              <div className="provider-title">{provider.displayName}</div>
              <div className="muted">{provider.description}</div>
              <div className="provider-footer">
                <StatusBadge label={provider.hasActiveKey ? '활성' : '미설정'} tone={provider.hasActiveKey ? 'success' : 'warning'} />
                {provider.keyIssueUrl && <a href={provider.keyIssueUrl} target="_blank" rel="noreferrer">키 발급</a>}
              </div>
            </div>
          ))}
        </div>
      </AppCard>

      <div className="two-column-grid wider-left">
        <AppCard title="등록된 API 키" actions={<div className="toolbar"><select value={filter} onChange={e => setFilter(e.target.value)}><option value="">전체</option><option value="OPENAI">OpenAI</option><option value="TAVILY">Tavily</option><option value="ANTHROPIC">Anthropic</option><option value="CUSTOM">Custom</option></select><button className="secondary" onClick={() => { setEditingId(null); setForm({ provider: '', label: '', description: '', keyValue: '', active: true }); }}>신규</button></div>}>
          <DataTable
            rows={items}
            columns={[
              { key: 'label', title: '레이블', render: row => <button className="link-button" onClick={() => { setEditingId(row.id); setForm({ provider: row.provider, label: row.label, description: row.description || '', keyValue: '', active: row.active !== false }); }}>{row.label}</button> },
              { key: 'provider', title: '프로바이더', render: row => row.provider },
              { key: 'masked', title: '마스킹', render: row => row.maskedKey ?? '-' },
              { key: 'status', title: '상태', render: row => <StatusBadge label={row.active ? '활성' : '비활성'} tone={row.active ? 'success' : 'default'} /> },
              { key: 'actions', title: '작업', render: row => <div className="button-row compact"><button className="secondary" onClick={() => keyApi.reveal(row.id).then(v => setRevealedKey(v.keyValue)).catch(() => setRevealedKey('조회 실패'))}>표시</button><button className="secondary" onClick={() => keyApi.activate(row.id).then(() => load()).catch(() => undefined)}>활성화</button><button className="danger" onClick={() => keyApi.remove(row.id).then(() => load()).catch(() => undefined)}>삭제</button></div> }
            ]}
          />
          {revealedKey && <div className="inline-code">{revealedKey}</div>}
        </AppCard>

        <AppCard title={editingId ? 'API 키 편집' : 'API 키 등록'} description="실제 키는 프론트에 저장하지 않고 저장 요청만 보냅니다.">
          <div className="form-grid two">
            <label className="field-label">프로바이더<select value={form.provider} onChange={e => setForm(prev => ({ ...prev, provider: e.target.value }))}><option value="">선택</option><option value="OPENAI">OpenAI</option><option value="TAVILY">Tavily</option><option value="ANTHROPIC">Anthropic</option><option value="CUSTOM">Custom</option></select></label>
            <label className="field-label">레이블<input value={form.label} onChange={e => setForm(prev => ({ ...prev, label: e.target.value }))} /></label>
          </div>
          <label className="field-label">설명<input value={form.description} onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))} /></label>
          <label className="field-label">API Key<input type="password" value={form.keyValue} onChange={e => setForm(prev => ({ ...prev, keyValue: e.target.value }))} placeholder={editingId ? '수정 시에만 입력' : '실제 키 입력'} /></label>
          {providerHint && <div className="notice-box">발급 페이지: <a href={providerHint} target="_blank" rel="noreferrer">{providerHint}</a></div>}
          <label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={e => setForm(prev => ({ ...prev, active: e.target.checked }))} /> 활성 상태로 저장</label>
          <div className="button-row"><button onClick={save}>저장</button><button className="secondary" onClick={() => { setEditingId(null); setForm({ provider: '', label: '', description: '', keyValue: '', active: true }); }}>초기화</button></div>
          <div className="status-line">{status}</div>
        </AppCard>
      </div>
    </div>
  );
}
