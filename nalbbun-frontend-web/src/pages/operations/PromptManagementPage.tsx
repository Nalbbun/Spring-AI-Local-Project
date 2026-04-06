import { useEffect, useMemo, useState } from 'react';
import { AppCard } from '../../components/ui/AppCard';
import { DataTable } from '../../components/ui/DataTable';
import { LogPanel } from '../../components/ui/LogPanel';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { useEventLog } from '../../hooks/useEventLog';
import { promptApi } from '../../services/promptApi';
import type { PromptEntry, PromptEntryHistory, PromptSummary } from '../../types/api';

const emptyForm = { name: '', category: '', description: '', systemPrompt: '', isDefault: false, active: true };

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' }).format(date);
}

export function PromptManagementPage() {
  const [summary, setSummary] = useState<PromptSummary | null>(null);
  const [items, setItems] = useState<PromptEntry[]>([]);
  const [history, setHistory] = useState<PromptEntryHistory[]>([]);
  const [filter, setFilter] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [status, setStatus] = useState('대기 중');
  const [form, setForm] = useState(emptyForm);
  const logs = useEventLog('prompt-management-log', ['프롬프트 관리 로그가 누적됩니다.']);

  const load = async (targetId?: string | null) => {
    const [summaryData, listData] = await Promise.all([promptApi.summary(), promptApi.listEntries(filter || undefined)]);
    setSummary(summaryData);
    setItems(listData);
    const historyId = targetId ?? editingId;
    if (historyId) {
      const historyData = await promptApi.history(historyId).catch(() => []);
      setHistory(historyData);
    } else {
      setHistory([]);
    }
  };

  useEffect(() => {
    load().catch((error) => logs.append('목록 조회 실패', error instanceof Error ? error.message : String(error)));
  }, [filter]);

  const resetForm = () => {
    setEditingId(null);
    setForm(emptyForm);
    setHistory([]);
  };

  const edit = async (row: PromptEntry) => {
    setEditingId(row.id);
    setForm({
      name: row.name,
      category: row.category || '',
      description: row.description || '',
      systemPrompt: row.systemPrompt || '',
      isDefault: !!row.isDefault,
      active: row.active !== false
    });
    setHistory(await promptApi.history(row.id).catch(() => []));
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
      await load(result.id);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setStatus(message);
      logs.append('프롬프트 저장 실패', message);
    }
  };

  const rollback = async (row: PromptEntryHistory) => {
    if (!editingId) return;
    const ok = window.confirm(`선택한 이력(${row.action} / v${row.versionNo ?? 1})으로 롤백하시겠습니까?`);
    if (!ok) return;
    try {
      const result = await promptApi.rollback(editingId, row.historyId);
      logs.append(`프롬프트 롤백 완료: ${result.name}`, `historyId=${row.historyId}`);
      await edit(result);
      await load(result.id);
      setStatus(`롤백 완료: ${result.name}`);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      logs.append('프롬프트 롤백 실패', message);
      setStatus(message);
    }
  };

  const filteredCount = useMemo(() => items.length, [items]);

  return (
    <div className="page-stack">
      <AppCard
        title="프롬프트 운영 현황"
        description="프롬프트 목록, 버전 히스토리, 롤백까지 한 화면에서 운영할 수 있도록 구성했습니다."
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
          description="카테고리별 조회와 기본 프롬프트 지정, 삭제를 처리합니다."
          actions={<div className="toolbar"><select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="">전체</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select><button className="secondary" onClick={resetForm}>신규</button></div>}
        >
          <DataTable
            rows={items}
            columns={[
              { key: 'name', title: '이름', render: (row) => <button className="link-button" onClick={() => edit(row)}>{row.name}</button> },
              { key: 'category', title: '카테고리', render: (row) => row.category || '공통' },
              { key: 'default', title: '기본', render: (row) => row.isDefault ? <StatusBadge label="기본" tone="info" /> : '-' },
              { key: 'version', title: '버전', render: (row) => `v${row.versionNo ?? 1}` },
              { key: 'status', title: '상태', render: (row) => <StatusBadge label={row.active ? '활성' : '비활성'} tone={row.active ? 'success' : 'default'} /> },
              {
                key: 'actions',
                title: '작업',
                render: (row) => (
                  <div className="button-row compact">
                    <button className="secondary" onClick={() => promptApi.setDefault(row.id).then(() => { logs.append(`기본 프롬프트 지정: ${row.name}`); return load(row.id); }).catch((e) => logs.append('기본 프롬프트 지정 실패', e instanceof Error ? e.message : String(e)))}>기본</button>
                    <button className="danger" onClick={() => promptApi.remove(row.id).then(() => { logs.append(`프롬프트 삭제: ${row.name}`); resetForm(); return load(); }).catch((e) => logs.append('프롬프트 삭제 실패', e instanceof Error ? e.message : String(e)))}>삭제</button>
                  </div>
                )
              }
            ]}
          />
        </AppCard>

        <AppCard title={editingId ? '프롬프트 편집' : '새 프롬프트 작성'} description="저장 시 히스토리가 적재되고, 우측 하단에서 바로 롤백할 수 있습니다.">
          {editingId && <div className="notice-box">수정 / 기본 지정 / 롤백 이력은 별도 히스토리로 저장됩니다.</div>}
          <div className="form-grid two">
            <label className="field-label">이름<input value={form.name} onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))} /></label>
            <label className="field-label">카테고리<select value={form.category} onChange={(e) => setForm((prev) => ({ ...prev, category: e.target.value }))}><option value="">공통</option><option value="GENERAL">GENERAL</option><option value="DEV">DEV</option><option value="MICE">MICE</option><option value="TRAVEL">TRAVEL</option></select></label>
          </div>
          <label className="field-label">설명<input value={form.description} onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))} /></label>
          <label className="field-label">시스템 프롬프트<textarea rows={14} value={form.systemPrompt} onChange={(e) => setForm((prev) => ({ ...prev, systemPrompt: e.target.value }))} /></label>
          <div className="checkbox-row wrap-row">
            <label className="checkbox-label"><input type="checkbox" checked={form.isDefault} onChange={(e) => setForm((prev) => ({ ...prev, isDefault: e.target.checked }))} /> 기본 프롬프트</label>
            <label className="checkbox-label"><input type="checkbox" checked={form.active} onChange={(e) => setForm((prev) => ({ ...prev, active: e.target.checked }))} /> 활성</label>
          </div>
          <div className="button-row">
            <button onClick={save}>저장</button>
            <button className="secondary" onClick={resetForm}>초기화</button>
          </div>
          <div className="status-line">{status}</div>
        </AppCard>
      </div>

      <AppCard title="프롬프트 버전 히스토리" description={editingId ? `선택된 프롬프트: ${editingId}` : '프롬프트를 선택하면 히스토리가 표시됩니다.'}>
        <DataTable
          rows={history}
          columns={[
            { key: 'capturedAt', title: '시각', render: (row) => formatDate(row.capturedAt) },
            { key: 'action', title: '액션', render: (row) => row.action },
            { key: 'versionNo', title: '버전', render: (row) => `v${row.versionNo ?? 1}` },
            { key: 'category', title: '카테고리', render: (row) => row.category || '공통' },
            { key: 'desc', title: '설명', render: (row) => row.description || '-' },
            { key: 'actions', title: '작업', render: (row) => <button className="secondary" disabled={!editingId} onClick={() => rollback(row)}>이 버전으로 롤백</button> }
          ]}
        />
      </AppCard>

      <AppCard title="작업 로그" actions={<button className="secondary" onClick={logs.clear}>로그 지우기</button>}>
        <LogPanel lines={logs.lines} />
      </AppCard>
    </div>
  );
}
