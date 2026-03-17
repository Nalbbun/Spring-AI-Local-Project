(() => {
  const { qs, val, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendToken } = window.ChatCommon;
  let agentEs = null;
  let startTime = null;
  const steps = {};

  const logEl    = () => qs('agentLog');
  const resultEl = () => qs('agentResult');
  const stepsEl  = () => qs('agentSteps');

  // 토큰 누적 상태
  const tokenState = { text: '' };

  // ── 에이전트 단계 표시 ─────────────────────────────────
  function upsertStep(agent, status, msg) {
    const container = stepsEl();
    if (!container) return;
    container.querySelector('.step-idle')?.remove();

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
      const sel = (id, v) => {
        const el = qs(id);
        if (!el || !v) return;
        [...el.options].forEach(o => { o.selected = o.value === v; });
      };
      sel('travelSearchModel', cfg?.travelSearchModel);
      sel('travelPlanModel',   cfg?.travelPlanModel);
    } catch { /* 무시 */ }

    try {
      const cfg = await fetchJson('/debug/api/config');
      setText('searchProviderChip', `fallback: ${cfg?.fallbackPolicy || '-'}`);
    } catch { /* 무시 */ }
  }

  // ── 모델 목록 로드 ─────────────────────────────────────
  async function loadModels() {
    try {
      const list   = await fetchJson('/debug/api/ollama/models?source=RUNNING');
      const models = (list || []).map(m => m.name || m.model || '').filter(Boolean);
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
        method: 'POST', headers: { 'Content-Type': 'application/json' },
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
      setText('webSearchResult', `검색 중: ${query}...`);
      const d = await fetchJson(`/debug/api/search?query=${encodeURIComponent(query)}`);
      const text = typeof d?.result === 'string' ? d.result : pretty(d);
      if (!text?.trim()) { setText('webSearchResult', '검색 결과 없음'); return; }

      const panel = qs('webSearchResult');
      const lines = text.split('\n');
      let html = '';
      let inItem = false;
      let itemBuf = [];

      const flushItem = () => {
        if (!itemBuf.length) return;
        const [title, url, ...rest] = itemBuf;
        html += `<div style="border-bottom:1px solid #1e293b;padding:8px 0">
          <div style="font-weight:700;color:#93c5fd">${htmlEscape(title || '')}</div>
          ${url ? `<div style="font-size:12px;color:#64748b">${htmlEscape(url)}</div>` : ''}
          ${rest.length ? `<div style="font-size:13px;margin-top:4px;color:#cbd5e1">${htmlEscape(rest.join(' ').trim())}</div>` : ''}
        </div>`;
        itemBuf = [];
      };

      for (const line of lines) {
        if (line.startsWith('[요약]')) {
          html += `<div style="background:#0f2a1e;border:1px solid #047857;border-radius:8px;
            padding:8px 10px;margin-bottom:8px;font-size:13px;color:#6ee7b7">${htmlEscape(line)}</div>`;
        } else if (/^\[\d+\]/.test(line)) {
          flushItem(); inItem = true;
          itemBuf.push(line.replace(/^\[\d+\]\s*/, ''));
        } else if (inItem && line.trim()) {
          itemBuf.push(line.trim());
        } else if (inItem && !line.trim()) {
          flushItem(); inItem = false;
        }
      }
      flushItem();
      panel.innerHTML = html || `<pre style="white-space:pre-wrap;font-size:13px">${htmlEscape(text)}</pre>`;
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

    tokenState.text = '';
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
      onToken: token => appendToken(resultEl(), token, tokenState),
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
    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML    =
      '<div class="step-idle">에이전트를 실행하면 단계별 진행 상황이 표시됩니다.</div>';
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
    qs('searchQuery')?.addEventListener('keydown', e => { if (e.key === 'Enter') webSearch(); });
    qs('btnRunAgent')?.addEventListener('click', runAgent);
    qs('btnClearAgent')?.addEventListener('click', clearAgent);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('agentMessage')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) runAgent();
    });
    loadAgentInfo();
  });
})();
