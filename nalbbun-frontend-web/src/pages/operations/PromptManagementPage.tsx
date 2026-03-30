import { useEffect, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { promptApi } from '../../services/promptApi';
import type { PromptEntry, PromptSummary } from '../../types/api';

export function PromptManagementPage() {
  const [summary, setSummary] = useState<PromptSummary | null>(null);
  const [items, setItems] = useState<PromptEntry[]>([]);
  const [filter, setFilter] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [status, setStatus] = useState('대기 중');
  const [form, setForm] = useState({ name: '', category: '', description: '', systemPrompt: '', isDefault: false, active: true });

  const load = async () => {
    const [summaryData, listData] = await Promise.all([promptApi.summary(), promptApi.listEntries(filter || undefined)]);
    setSummary(summaryData);
    setItems(listData);
  };

  useEffect(() => { load().catch(() => undefined); }, [filter]);

  const save = async () => {
    setStatus('저장 중');
    try {
      const payload = { ...form, category: form.category || null };
      const result = editingId ? await promptApi.update(editingId, payload) : await promptApi.create(payload);
      setEditingId(result.id);
      setStatus(`저장 완료: ${result.name}`);
      await load();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : String(error));
    }
  };

  return (
    <div className="page-stack">
      <AppCard title="프롬프트 요약" description="legacy prompts 화면의 summary/seed/default 기능을 React로 분리한 관리 화면입니다." actions={<div className="button-row compact"><button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button><button className="secondary" onClick={() => promptApi.seed().then(() => load()).catch(() => undefined)}>기본 시드</button></div>}>
        <div className="stats-grid">
          <div className="stat-box"><span>Store</span><strong>{summary?.store ?? '-'}</strong></div>
          <div className="stat-box"><span>Total</span><strong>{summary?.total ?? 0}</strong></div>
          <div className="stat-box"><span>Active</span><strong>{summary?.activeCount ?? 0}</strong></div>
        </div>
      </AppCard>

      <div className="two-column-grid wider-left">
        <AppCard title="프롬프트 목록" actions={<div className="toolbar"><select value={filter} onChange={e => setFilter(e.target.value)}><option value="">전체</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select><button className="secondary" onClick={() => { setEditingId(null); setForm({ name: '', category: '', description: '', systemPrompt: '', isDefault: false, active: true }); }}>신규</button></div>}>
          <DataTable
            rows={items}
            columns={[
              { key: 'name', title: '이름', render: row => <button className="link-button" onClick={() => { setEditingId(row.id); setForm({ name: row.name, category: row.category || '', description: row.description || '', systemPrompt: row.systemPrompt || '', isDefault: !!row.isDefault, active: row.active !== false }); }}>{row.name}</button> },
              { key: 'category', title: '카테고리', render: row => row.category || '공통' },
              { key: 'default', title: '기본', render: row => row.isDefault ? <StatusBadge label="기본" tone="info" /> : '-' },
              { key: 'status', title: '상태', render: row => <StatusBadge label={row.active ? '활성' : '비활성'} tone={row.active ? 'success' : 'default'} /> },
              { key: 'actions', title: '작업', render: row => <div className="button-row compact"><button className="secondary" onClick={() => promptApi.setDefault(row.id).then(() => load()).catch(() => undefined)}>기본</button><button className="danger" onClick={() => promptApi.remove(row.id).then(() => load()).catch(() => undefined)}>삭제</button></div> }
            ]}
          />
        </AppCard>

        <AppCard title={editingId ? '프롬프트 편집' : '새 프롬프트 작성'}>
          <div className="form-grid two">
            <label className="field-label">이름<input value={form.name} onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))} /></label>
            <label className="field-label">카테고리<select value={form.category} onChange={e => setForm(prev => ({ ...prev, category: e.target.value }))}><option value="">공통</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select></label>
          </div>
          <label className="field-label">설명<input value={form.description} onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))} /></label>
          <label className="field-label">시스템 프롬프트<textarea rows={14} value={form.systemPrompt} onChange={e => setForm(prev => ({ ...prev, systemPrompt: e.target.value }))} /></label>
          <div className="checkbox-row"><label className="checkbox-label"><input type="checkbox" checked={form.isDefault} onChange={e => setForm(prev => ({ ...prev, isDefault: e.target.checked }))} /> 기본 프롬프트</label><label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={e => setForm(prev => ({ ...prev, active: e.target.checked }))} /> 활성</label></div>
          <div className="button-row"><button onClick={save}>저장</button><button className="secondary" onClick={() => { setEditingId(null); setForm({ name: '', category: '', description: '', systemPrompt: '', isDefault: false, active: true }); }}>초기화</button></div>
          <div className="status-line">{status}</div>
        </AppCard>
      </div>
    </div>
  );
}
