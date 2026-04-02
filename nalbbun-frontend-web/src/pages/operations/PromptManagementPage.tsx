import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { promptApi } from '../../services/promptApi';
import type { PromptEntry, PromptSummary } from '../../types/api';

const emptyForm = { name: '', category: '', description: '', systemPrompt: '', isDefault: false, active: true };

export function PromptManagementPage() {
  const [summary, setSummary] = useState<PromptSummary | null>(null);
  const [items, setItems] = useState<PromptEntry[]>([]);
  const [filter, setFilter] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [status, setStatus] = useState('대기 중');
  const [form, setForm] = useState(emptyForm);
  const logs = useEventLog('prompt-management-log', ['프롬프트 관리 로그가 누적됩니다.']);

  const load = async () => {
    const [summaryData, listData] = await Promise.all([promptApi.summary(), promptApi.listEntries(filter || undefined)]);
    setSummary(summaryData);
    setItems(listData);
  };

  useEffect(() => {
    load().catch((error) => logs.append('목록 조회 실패', error instanceof Error ? error.message : String(error)));
  }, [filter]);

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const edit = (row: PromptEntry) => {
    setEditingId(row.id);
    setForm({
      name: row.name,
      category: row.category || '',
      description: row.description || '',
      systemPrompt: row.systemPrompt || '',
      isDefault: !!row.isDefault,
      active: row.active !== false
    });
    logs.append(`프롬프트 편집 시작: ${row.name}`);
  };

  const save = async () => {
    setStatus('저장 중');
    try {
      const payload = { ...form, category: form.category || null };
      const result = editingId ? await promptApi.update(editingId, payload) : await promptApi.create(payload);
      setEditingId(result.id);
      setStatus(`저장 완료: ${result.name}`);
      logs.append(`프롬프트 저장 완료: ${result.name}`);
      await load();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(message);
      logs.append('프롬프트 저장 실패', message);
    }
  };

  const filteredCount = useMemo(() => items.length, [items]);

  return (
    <div className="page-stack">
      <AppCard
        title="프롬프트 운영 현황"
        description=" prompts 화면의 저장소 요약, 카테고리 필터, 편집, 기본 지정, 이벤트 로그 흐름을 React로 복원했습니다."
        actions={<div className="button-row compact"><button className="secondary" onClick={() => load().catch(() => undefined)}>새로고침</button><button className="secondary" onClick={() => promptApi.seed().then((v) => { logs.append(`기본 시드 완료: ${v.seeded}/${v.total}`); return load(); }).catch((e) => logs.append('기본 시드 실패', e instanceof Error ? e.message : String(e)))}>기본 시드</button></div>}
      >
        <div className="stats-grid compact-four">
          <div className="stat-box"><span>Store</span><strong>{summary?.store ?? '-'}</strong></div>
          <div className="stat-box"><span>Total</span><strong>{summary?.total ?? 0}</strong></div>
          <div className="stat-box"><span>Active</span><strong>{summary?.activeCount ?? 0}</strong></div>
          <div className="stat-box"><span>현재 필터 수</span><strong>{filteredCount}</strong></div>
        </div>
      </AppCard>

      <div className="two-column-grid wider-left">
        <AppCard
          title="프롬프트 목록"
          description="카테고리별로 필터링하면서 기본/활성 상태를 바로 조정할 수 있습니다."
          actions={<div className="toolbar"><select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="">전체</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select><button className="secondary" onClick={resetForm}>신규</button></div>}
        >
          <DataTable
            rows={items}
            columns={[
              { key: 'name', title: '이름', render: (row) => <button className="link-button" onClick={() => edit(row)}>{row.name}</button> },
              { key: 'category', title: '카테고리', render: (row) => row.category || '공통' },
              { key: 'default', title: '기본', render: (row) => row.isDefault ? <StatusBadge label="기본" tone="info" /> : '-' },
              { key: 'status', title: '상태', render: (row) => <StatusBadge label={row.active ? '활성' : '비활성'} tone={row.active ? 'success' : 'default'} /> },
              {
                key: 'actions',
                title: '작업',
                render: (row) => (
                  <div className="button-row compact">
                    <button className="secondary" onClick={() => promptApi.setDefault(row.id).then(() => { logs.append(`기본 프롬프트 지정: ${row.name}`); return load(); }).catch((e) => logs.append('기본 프롬프트 지정 실패', e instanceof Error ? e.message : String(e)))}>기본</button>
                    <button className="danger" onClick={() => promptApi.remove(row.id).then(() => { logs.append(`프롬프트 삭제: ${row.name}`); resetForm(); return load(); }).catch((e) => logs.append('프롬프트 삭제 실패', e instanceof Error ? e.message : String(e)))}>삭제</button>
                  </div>
                )
              }
            ]}
          />
        </AppCard>

        <AppCard title={editingId ? '프롬프트 편집' : '새 프롬프트 작성'} description="작성 → 저장 → 기본 지정 → 채팅 적용 흐름을 고려해 편집 폼을 구성했습니다.">
          <div className="form-grid two">
            <label className="field-label">이름<input value={form.name} onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))} /></label>
            <label className="field-label">카테고리<select value={form.category} onChange={(e) => setForm((prev) => ({ ...prev, category: e.target.value }))}><option value="">공통</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select></label>
          </div>
          <label className="field-label">설명<input value={form.description} onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))} /></label>
          <label className="field-label">시스템 프롬프트<textarea rows={16} value={form.systemPrompt} onChange={(e) => setForm((prev) => ({ ...prev, systemPrompt: e.target.value }))} /></label>
          <div className="checkbox-row wrap-row">
            <label className="checkbox-label"><input type="checkbox" checked={form.isDefault} onChange={(e) => setForm((prev) => ({ ...prev, isDefault: e.target.checked }))} /> 기본 프롬프트</label>
            <label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))} /> 활성</label>
          </div>
          <div className="button-row">
            <button onClick={save}>저장</button>
            <button className="secondary" onClick={resetForm}>초기화</button>
            {form.category && <a className="nav-link-like" href={`/chat/${form.category === 'TRAVEL' ? 'agent' : form.category === 'DEV' ? 'rag' : 'general'}`}>연결된 채팅 열기</a>}
          </div>
          <div className="notice-box top-gap">
            저장 후 채팅 화면에서 해당 카테고리 프롬프트를 선택해 즉시 테스트할 수 있습니다.
          </div>
          <div className="status-line">{status}</div>
        </AppCard>
      </div>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
