/**
 * prompts-page.js — 프롬프트 CRUD 관리 페이지
 *
 * 주의: PromptEntry.isDefault → Jackson @JsonProperty("isDefault") 로 직렬화
 *   - 서버 응답: { "isDefault": true } → JS에서 p.isDefault 로 접근
 *   - p.default 는 JS 예약어라 항상 undefined → 버그 원인
 */
(() => {
  const { qs, val, setVal, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;

  let allPrompts   = [];   // 전체 목록 캐시
  let currentId    = null; // 편집 중인 ID (null = 신규)
  let currentCat   = '';   // 현재 필터 카테고리

  const logEl = () => qs('eventLog');

  function log(msg) {
    const el = logEl();
    if (!el) return;
    el.textContent += msg + '\n';
    el.scrollTop = el.scrollHeight;
  }

  // ── 요약 조회 ──────────────────────────────────────
  async function loadSummary() {
    try {
      const d = await fetchJson('/api/prompt-entries/summary');
      setText('storeChip',     `store: ${d.store}`);
      setText('totalChip',     `total: ${d.total}`);
      setText('activeChip',    `active: ${d.activeCount}`);
      setText('storeTypeLabel', d.store === 'redis' ? '🔴 Redis' : '🐘 PostgreSQL (JDBC)');
    } catch (e) { log('[요약] 실패: ' + e.message); }
  }

  // ── 목록 조회 ──────────────────────────────────────
  async function loadList(cat) {
    currentCat = cat ?? currentCat;
    try {
      const url = currentCat
          ? `/api/prompt-entries?category=${encodeURIComponent(currentCat)}`
          : '/api/prompt-entries';
      allPrompts = await fetchJson(url);
      renderTable(allPrompts);
      setText('listStatus', `${allPrompts.length}개 조회됨`);
    } catch (e) { log('[목록] 실패: ' + e.message); }
  }

  function renderTable(list) {
    const tbody = qs('promptTable');
    if (!list || !list.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:#64748b">등록된 프롬프트가 없습니다.</td></tr>';
      return;
    }
    tbody.innerHTML = list.map(p => {
      const cat = p.category
          ? `<span class="badge-cat">${htmlEscape(p.category)}</span>`
          : '<span class="badge-cat" style="color:#475569">공통</span>';

      // ★ 핵심 수정: p.default(JS 예약어·항상 undefined) → p.isDefault 로 변경
      const defBg = p.isDefault
          ? `<span class="badge-default">기본</span>`
          : '-';
      const actBg = p.active
          ? `<span class="badge-active">활성</span>`
          : `<span class="badge-inactive">비활성</span>`;
      const selected = p.id === currentId ? 'style="background:#1e3a5f"' : '';

      return `<tr id="row-${htmlEscape(p.id)}" ${selected}>
        <td>
          <a href="#" style="color:#e2e8f0;text-decoration:none;font-size:13px"
             onclick="window._selectPrompt('${htmlEscape(p.id)}');return false;">
            ${htmlEscape(p.name)}
          </a>
          ${p.description
              ? `<div style="font-size:11px;color:#64748b;margin-top:2px">${htmlEscape(p.description)}</div>`
              : ''}
        </td>
        <td>${cat}</td>
        <td>${defBg}</td>
        <td>${actBg}</td>
        <td>
          <div style="display:flex;gap:4px">
            <button class="primary" style="width:auto;padding:3px 8px;font-size:12px"
              onclick="window._selectPrompt('${htmlEscape(p.id)}')">✏️</button>
            <button class="green" style="width:auto;padding:3px 8px;font-size:12px"
              onclick="window._setDefault('${htmlEscape(p.id)}')">⭐</button>
            <button class="red" style="width:auto;padding:3px 8px;font-size:12px"
              onclick="window._deletePrompt('${htmlEscape(p.id)}')">🗑</button>
          </div>
        </td>
      </tr>`;
    }).join('');
  }

  // ── 카테고리 필터 탭 ──────────────────────────────
  window._filterCat = (btn, cat) => {
    document.querySelectorAll('.mem-tab[data-cat]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadList(cat);
  };

  // ── 폼 조작 ───────────────────────────────────────
  function clearForm() {
    currentId = null;
    setText('formTitle', '✏️ 새 프롬프트 작성');
    setVal('fName', '');
    setVal('fCategory', '');
    setVal('fDesc', '');
    if (qs('fSystemPrompt')) qs('fSystemPrompt').value = '';
    if (qs('fIsDefault'))    qs('fIsDefault').checked  = false;
    if (qs('fActive'))       qs('fActive').checked     = true;
    setText('formStatus', '새 프롬프트를 작성하세요.');
    const del = qs('btnDelete');
    if (del) del.style.display = 'none';
    document.querySelectorAll('#promptTable tr').forEach(r => r.removeAttribute('style'));
  }

  window._selectPrompt = (id) => {
    const p = allPrompts.find(x => x.id === id);
    if (!p) return;
    currentId = id;
    setText('formTitle', `✏️ 편집: ${p.name}`);
    setVal('fName', p.name || '');
    setVal('fCategory', p.category || '');
    setVal('fDesc', p.description || '');
    if (qs('fSystemPrompt')) qs('fSystemPrompt').value = p.systemPrompt || '';
    // ★ 핵심 수정: p.default → p.isDefault
    if (qs('fIsDefault')) qs('fIsDefault').checked = !!p.isDefault;
    if (qs('fActive'))    qs('fActive').checked    = p.active !== false;
    setText('formStatus',
        `ID: ${p.id}  |  기본: ${p.isDefault ? '✅' : '-'}  |  생성: ${
            (p.createdAt || '').toString().substring(0, 19).replace('T', ' ')
        }`);
    const del = qs('btnDelete');
    if (del) del.style.display = '';
    document.querySelectorAll('#promptTable tr').forEach(r => r.removeAttribute('style'));
    const row = qs('row-' + id);
    if (row) row.setAttribute('style', 'background:#1e3a5f');
  };

  function buildPayload() {
    const cat = val('fCategory');
    const isDefaultChecked = qs('fIsDefault')?.checked ?? false;
    return {
      name:         val('fName').trim(),
      category:     cat || null,
      systemPrompt: qs('fSystemPrompt')?.value?.trim() || '',
      description:  val('fDesc').trim(),
      isDefault:    isDefaultChecked,  // Jackson @JsonProperty("isDefault") 로 수신
      active:       qs('fActive')?.checked ?? true,
    };
  }

  async function savePrompt() {
    const payload = buildPayload();
    if (!payload.name)         { setText('formStatus', '⚠ 이름을 입력하세요.'); return; }
    if (!payload.systemPrompt) { setText('formStatus', '⚠ 시스템 프롬프트를 입력하세요.'); return; }

    setText('formStatus', '저장 중...');
    try {
      let result;
      if (currentId) {
        result = await fetchJson(`/api/prompt-entries/${encodeURIComponent(currentId)}`, {
          method:  'PUT',
          headers: { 'Content-Type': 'application/json' },
          body:    JSON.stringify(payload),
        });
        log(`[수정] ${result.name} (id=${result.id}, isDefault=${result.isDefault})`);
      } else {
        result = await fetchJson('/api/prompt-entries', {
          method:  'POST',
          headers: { 'Content-Type': 'application/json' },
          body:    JSON.stringify(payload),
        });
        log(`[생성] ${result.name} (id=${result.id}, isDefault=${result.isDefault})`);
      }
      setText('formStatus', `저장 완료 — ID: ${result.id} | 기본: ${result.isDefault ? '✅' : '-'}`);
      currentId = result.id;
      await loadList();
      await loadSummary();
    } catch (e) {
      setText('formStatus', '저장 실패: ' + e.message);
      log('[저장] 실패: ' + e.message);
    }
  }

  window._deletePrompt = async (id) => {
    const p = allPrompts.find(x => x.id === id);
    if (!confirm(`"${p?.name || id}" 프롬프트를 삭제할까요?`)) return;
    try {
      await fetchJson(`/api/prompt-entries/${encodeURIComponent(id)}`, { method: 'DELETE' });
      log(`[삭제] ${id}`);
      if (currentId === id) clearForm();
      await loadList();
      await loadSummary();
    } catch (e) { log('[삭제] 실패: ' + e.message); }
  };

  window._setDefault = async (id) => {
    const p = allPrompts.find(x => x.id === id);
    setText('formStatus', `"${p?.name || id}" 기본 설정 중...`);
    try {
      const r = await fetchJson(`/api/prompt-entries/${encodeURIComponent(id)}/default`, {
        method: 'POST',
      });
      log(`[기본설정] ${r.name} → isDefault=${r.isDefault}`);
      // 현재 편집 중인 항목이면 체크박스 즉시 반영
      if (currentId === id && qs('fIsDefault')) {
        qs('fIsDefault').checked = true;
        setText('formStatus', `기본 프롬프트 지정 완료 — ${r.name}`);
      }
      await loadList();
    } catch (e) { log('[기본설정] 실패: ' + e.message); }
  };

  async function seedDefaults() {
    try {
      const r = await fetchJson('/api/prompt-entries/seed', { method: 'POST' });
      setText('seedStatus', `시드 완료 — ${r.seeded}개 추가 (전체 ${r.total}개)`);
      log(`[시드] ${r.seeded}개 기본 프롬프트 추가`);
      await loadList();
      await loadSummary();
    } catch (e) { setText('seedStatus', '시드 실패: ' + e.message); }
  }

  // ── 초기화 ────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnRefresh')?.addEventListener('click', async () => {
      await loadList();
      await loadSummary();
    });
    qs('btnSeedDefaults')?.addEventListener('click', seedDefaults);
    qs('btnNewPrompt')?.addEventListener('click', clearForm);
    qs('btnSave')?.addEventListener('click', savePrompt);
    qs('btnCancel')?.addEventListener('click', clearForm);
    qs('btnDelete')?.addEventListener('click', () => currentId && window._deletePrompt(currentId));

    loadSummary();
    loadList();
    clearForm();
  });
})();
