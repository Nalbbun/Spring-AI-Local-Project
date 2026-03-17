(() => {
  const { qs, val, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendResult } = window.ChatCommon;
  let agentEs = null;
  let startTime = null;
  const steps = {};

  const logEl    = () => qs('agentLog');
  const resultEl = () => qs('agentResult');
  const stepsEl  = () => qs('agentSteps');

  // ── 에이전트 단계 표시 ─────────────────────────────────
  function upsertStep(agent, status, msg) {
    const container = stepsEl();
    if (!container) return;
    const idle = container.querySelector('.step-idle');
    if (idle) idle.remove();

    const icon = { running: '⏳', complete: '✅', error: '❌' }[status] || '🔵';
    const cls  = `agent-step ${status}`;

    if (steps[agent]) {
      steps[agent].className = cls;
      steps[agent].querySelector('.step-icon').textContent = icon;
      steps[agent].querySelector('.step-msg').textContent  = msg;
    } else {
      const el = document.createElement('div');
      el.className = cls;
      el.innerHTML = `<span class="step-icon">${icon}</span>
        <div class="step-body">
          <div class="step-name">${htmlEscape(agent)}</div>
          <div class="step-msg">${htmlEscape(msg)}</div>
        </div>`;
      container.appendChild(el);
      steps[agent] = el;
    }
  }

  // ── 현황 로드 ──────────────────────────────────────────
  async function loadAgentInfo() {
    try {
      const cfg = await fetchJson('/debug/api/ollama/config');
      setText('searchModelChip', `search: ${cfg?.travelSearchModel || '-'}`);
      setText('planModelChip',   `plan: ${cfg?.travelPlanModel || '-'}`);
      setText('agentModelInfo',
        `TRAVEL Search: ${cfg?.travelSearchModel || '-'}\nTRAVEL Plan  : ${cfg?.travelPlanModel || '-'}`);

      // select 동기화
      const select = (id, val) => {
        const el = qs(id);
        if (!el || !val) return;
        [...el.options].forEach(o => { o.selected = o.value === val; });
      };
      select('travelSearchModel', cfg?.travelSearchModel);
      select('travelPlanModel',   cfg?.travelPlanModel);
    } catch { /* 무시 */ }

    try {
      const cfg = await fetchJson('/debug/api/config');
      setText('searchProviderChip', `fallback: ${cfg?.fallbackPolicy || '-'}`);
    } catch { /* 무시 */ }
  }

  // ── 모델 목록 로드 ─────────────────────────────────────
  async function loadModels() {
    try {
      const list = await fetchJson('/debug/api/ollama/models?source=RUNNING');
      const models = (list || []).map(m => m.name || m.model || String(m));
      ['travelSearchModel', 'travelPlanModel'].forEach(id => {
        const sel = qs(id);
        if (!sel) return;
        const cur = sel.value;
        sel.innerHTML = '<option value="">-- 선택 --</option>' +
          models.map(m => `<option value="${m}">${m}</option>`).join('');
        if (cur) [...sel.options].forEach(o => { o.selected = o.value === cur; });
      });
      setText('agentModelSaveStatus', `모델 ${models.length}개 조회: ${models.join(', ')}`);
    } catch (e) {
      setText('agentModelSaveStatus', '모델 조회 실패: ' + e.message);
    }
  }

  async function saveAgentModels() {
    const body = {
      travelSearchModel: val('travelSearchModel'),
      travelPlanModel:   val('travelPlanModel'),
    };
    try {
      const d = await fetchJson('/debug/api/ollama/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      setText('agentModelSaveStatus', '저장 완료\n' + pretty(d));
      await loadAgentInfo();
    } catch (e) {
      setText('agentModelSaveStatus', '저장 실패: ' + e.message);
    }
  }

  async function resetAgentModels() {
    try {
      const d = await fetchJson('/debug/api/ollama/config/reset', { method: 'POST' });
      setText('agentModelSaveStatus', '초기화 완료\n' + pretty(d));
      await loadModels();
      await loadAgentInfo();
    } catch (e) {
      setText('agentModelSaveStatus', '초기화 실패: ' + e.message);
    }
  }

  // ── 웹 검색 테스트 ─────────────────────────────────────
  async function webSearch() {
    const query = val('searchQuery');
    if (!query) return;
    try {
      setText('webSearchResult', `검색 중: ${query}`);
      const d = await fetchJson(`/debug/api/search?query=${encodeURIComponent(query)}`);
      const results = d?.result?.results || d?.result || [];
      if (Array.isArray(results) && results.length) {
        qs('webSearchResult').innerHTML = results.map((r, i) => `
          <div style="border-bottom:1px solid #1e293b;padding:8px 0">
            <div style="font-weight:700;color:#93c5fd">#${i+1} ${htmlEscape(r.title || '-')}</div>
            <div style="font-size:12px;color:#64748b">${htmlEscape(r.url || '')}</div>
            <div style="font-size:13px;margin-top:4px">${htmlEscape(r.content || r.snippet || '')}</div>
          </div>`).join('');
      } else {
        setText('webSearchResult', '결과 없음\n' + pretty(d));
      }
    } catch (e) {
      setText('webSearchResult', '검색 실패: ' + e.message);
    }
  }

  // ── 에이전트 실행 ──────────────────────────────────────
  function runAgent() {
    const message  = val('agentMessage');
    const category = val('agentCategory');
    if (!message) { logLine(logEl(), '[error] 질문을 입력하세요.'); return; }
    if (agentEs) { agentEs.close(); agentEs = null; }
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML    = '';
    Object.keys(steps).forEach(k => delete steps[k]);
    startTime = Date.now();
    setText('elapsedTime', '실행 중...');

    const url = `/api/chat/stream?message=${encodeURIComponent(message)}&category=${encodeURIComponent(category)}`;
    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category}`);

    agentEs = startStream({
      url,
      onToken: data => appendResult(resultEl(), data),
      onAgent: ({ agent, status, message: msg }) => {
        logLine(logEl(), `[${agent}] ${status} — ${msg}`);
        upsertStep(agent, status, msg);
      },
      onComplete: () => {
        logLine(logEl(), '[complete] 에이전트 실행 완료');
        if (startTime) setText('elapsedTime', ((Date.now() - startTime) / 1000).toFixed(1) + 's');
      },
      onError: () => {
        logLine(logEl(), '[error] 스트림 오류');
        if (startTime) setText('elapsedTime', ((Date.now() - startTime) / 1000).toFixed(1) + 's (오류)');
      },
    });
  }

  function clearAgent() {
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML    = '<div class="step-idle">에이전트를 실행하면 단계별 진행 상황이 표시됩니다.</div>';
    setText('elapsedTime', '-');
    Object.keys(steps).forEach(k => delete steps[k]);
    if (agentEs) { agentEs.close(); agentEs = null; }
  }

  async function memoryClear() {
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      logLine(logEl(), '[memory] 대화 초기화 완료');
    } catch (e) {
      logLine(logEl(), '[memory] 초기화 실패: ' + e.message);
    }
  }

  // ── 초기화 ─────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnRefreshAgentInfo')?.addEventListener('click', loadAgentInfo);
    qs('btnLoadAgentModels')?.addEventListener('click', loadModels);
    qs('btnSaveAgentModels')?.addEventListener('click', saveAgentModels);
    qs('btnResetAgentModels')?.addEventListener('click', resetAgentModels);
    qs('btnWebSearch')?.addEventListener('click', webSearch);
    qs('searchQuery')?.addEventListener('keydown', e => {
      if (e.key === 'Enter') webSearch();
    });
    qs('btnRunAgent')?.addEventListener('click', runAgent);
    qs('btnClearAgent')?.addEventListener('click', clearAgent);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('agentMessage')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) runAgent();
    });
    loadAgentInfo();
  });
})();
