import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { keyApi } from '../../services/keyApi';
import type { ApiKeyEntry, ProviderStatus } from '../../types/api';

const issueMap: Record<string, string> = {
  OPENAI: 'https://platform.openai.com/api-keys',
  TAVILY: 'https://app.tavily.com',
  ANTHROPIC: 'https://console.anthropic.com'
};

const emptyForm = { provider: '', label: '', description: '', keyValue: '', active: true };

export function KeyManagementPage() {
  const [providers, setProviders] = useState<ProviderStatus[]>([]);
  const [items, setItems] = useState<ApiKeyEntry[]>([]);
  const [filter, setFilter] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [revealedKey, setRevealedKey] = useState('');
  const [status, setStatus] = useState('대기 중');
  const [form, setForm] = useState(emptyForm);
  const logs = useEventLog('key-management-log', ['API 키 관리 로그가 누적됩니다.']);

  const load = async () => {
    const [providerData, listData] = await Promise.all([keyApi.providers(), keyApi.list(filter || undefined)]);
    setProviders(providerData);
    setItems(listData);
  };

  useEffect(() => { load().catch((error) => logs.append('API 키 조회 실패', error instanceof Error ? error.message : String(error))); }, [filter]);

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const save = async () => {
    setStatus('저장 중');
    try {
      const normalizedProvider = form.provider.trim().toUpperCase();
      const payload: Record<string, unknown> = { provider: normalizedProvider, label: form.label, description: form.description, active: form.active };
      if (form.keyValue) payload.keyValue = form.keyValue;
      const result = editingId ? await keyApi.update(editingId, payload) : await keyApi.create({ ...payload, keyValue: form.keyValue });
      setEditingId(result.id);
      setStatus(`저장 완료: ${result.label}`);
      setForm((prev) => ({ ...prev, provider: normalizedProvider, keyValue: '' }));
      logs.append(`API 키 저장 완료: ${result.label}`);
      await load();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(message);
      logs.append('API 키 저장 실패', message);
    }
  };

  const providerHint = useMemo(() => issueMap[form.provider.trim().toUpperCase()] || '', [form.provider]);
  const activeCount = providers.filter((provider) => provider.hasActiveKey).length;
  const providerNames = providers.map((provider) => provider.provider).filter(Boolean);

  return (
    <div className="page-stack">
      <AppCard title="프로바이더 현황" description="기본 provider와 사용자 정의 provider를 함께 관리합니다." actions={<button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button>}>
        <div className="stats-grid compact-four top-gap">
          <div className="stat-box"><span>Provider 수</span><strong>{providers.length}</strong></div>
          <div className="stat-box"><span>Active Provider</span><strong>{activeCount}</strong></div>
          <div className="stat-box"><span>등록 키 수</span><strong>{items.length}</strong></div>
          <div className="stat-box"><span>현재 필터</span><strong>{filter || '전체'}</strong></div>
        </div>
        <div className="provider-grid top-gap">
          {providers.map((provider) => (
            <div className="provider-card" key={provider.provider}>
              <div className="provider-title">{provider.displayName || provider.provider}</div>
              <div className="muted">{provider.description || '사용자 정의 API 키 그룹'}</div>
              <div className="provider-footer">
                <StatusBadge label={provider.hasActiveKey ? '활성' : '미설정'} tone={provider.hasActiveKey ? 'success' : 'warning'} />
                {provider.keyIssueUrl && <a href={provider.keyIssueUrl} target="_blank" rel="noreferrer">키 발급</a>}
              </div>
            </div>
          ))}
        </div>
      </AppCard>

      <div className="two-column-grid wider-left">
        <AppCard title="등록된 API 키" actions={<div className="toolbar"><select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="">전체</option>{providerNames.map((provider) => <option key={provider} value={provider}>{provider}</option>)}</select><button className="secondary" onClick={resetForm}>신규</button></div>}>
          <DataTable
            rows={items}
            columns={[
              { key: 'label', title: '레이블', render: (row) => <button className="link-button" onClick={() => { setEditingId(row.id); setForm({ provider: row.provider, label: row.label, description: row.description || '', keyValue: '', active: row.active !== false }); logs.append(`API 키 편집 시작: ${row.label}`); }}>{row.label}</button> },
              { key: 'provider', title: '프로바이더', render: (row) => row.provider },
              { key: 'masked', title: '마스킹', render: (row) => row.maskedKey ?? '-' },
              { key: 'status', title: '상태', render: (row) => <StatusBadge label={row.active ? '활성' : '비활성'} tone={row.active ? 'success' : 'default'} /> },
              {
                key: 'actions',
                title: '작업',
                render: (row) => (
                  <div className="button-row compact">
                    <button className="secondary" onClick={() => keyApi.reveal(row.id).then((v) => { setRevealedKey(v.keyValue); logs.append(`API 키 노출 조회: ${row.label}`); }).catch((e) => logs.append('API 키 조회 실패', e instanceof Error ? e.message : String(e)))}>표시</button>
                    <button className="secondary" onClick={() => keyApi.activate(row.id).then(() => { logs.append(`API 키 활성화: ${row.label}`); return load(); }).catch((e) => logs.append('API 키 활성화 실패', e instanceof Error ? e.message : String(e)))}>활성화</button>
                    <button className="danger" onClick={() => keyApi.remove(row.id).then(() => { logs.append(`API 키 삭제: ${row.label}`); return load(); }).catch((e) => logs.append('API 키 삭제 실패', e instanceof Error ? e.message : String(e)))}>삭제</button>
                  </div>
                )
              }
            ]}
          />
          {revealedKey && <div className="inline-code">{revealedKey}</div>}
        </AppCard>

        <AppCard title={editingId ? 'API 키 편집' : 'API 키 등록'} description="정해진 provider 외에도 원하는 이름으로 여러 키 그룹을 만들 수 있습니다.">
          <div className="form-grid two">
            <label className="field-label">프로바이더
              <input list="provider-name-options" value={form.provider} onChange={(e) => setForm((prev) => ({ ...prev, provider: e.target.value }))} placeholder="예: OPENAI, VLLM, CUSTOMER_A, INTERNAL_LLM_01" />
              <datalist id="provider-name-options">
                {providerNames.map((provider) => <option key={provider} value={provider} />)}
              </datalist>
            </label>
            <label className="field-label">레이블<input value={form.label} onChange={(e) => setForm((prev) => ({ ...prev, label: e.target.value }))} /></label>
          </div>
          <label className="field-label">설명<input value={form.description} onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))} /></label>
          <label className="field-label">API Key<input type="password" value={form.keyValue} onChange={(e) => setForm((prev) => ({ ...prev, keyValue: e.target.value }))} placeholder={editingId ? '수정 시에만 입력' : '실제 키 입력'} /></label>
          {providerHint && <div className="notice-box">발급 페이지: <a href={providerHint} target="_blank" rel="noreferrer">{providerHint}</a></div>}
          <label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))} /> 활성 상태로 저장</label>
          <div className="button-row"><button onClick={save}>저장</button><button className="secondary" onClick={resetForm}>초기화</button></div>
          <div className="status-line">{status}</div>
        </AppCard>
      </div>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
