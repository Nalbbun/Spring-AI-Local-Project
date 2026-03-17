(() => {
  const { qs, val, setVal, setText, fetchJson, pretty } = window.UiCommon;
  const { startStream, logLine, appendResult } = window.ChatCommon;
  let testEs = null;

  // ── 유틸 ───────────────────────────────────────────────
  function selectVal(id, value) {
    const el = qs(id);
    if (!el || !value) return;
    [...el.options].forEach(o => { o.selected = o.value === value; });
  }

  function fillModelSelects(models) {
    const ids = ['generalModel', 'devModel', 'miceModel', 'travelSearchModel', 'travelPlanModel'];
    ids.forEach(id => {
      const sel = qs(id);
      if (!sel) return;
      const cur = sel.value;
      sel.innerHTML = '<option value="">-- 선택 --</option>' +
        models.map(m => `<option value="${m}">${m}</option>`).join('');
      if (cur) selectVal(id, cur);
    });
  }

  // ── Ollama 연결 ────────────────────────────────────────
  async function checkConn() {
    try {
      const d = await fetchJson('/debug/api/ollama/connection');
      setText('ollamaConnStatus',
        `status: ${d.status}\nreachable: ${d.reachable}\nrunning: ${d.runningCount}\ninstalled: ${d.installedCount}\nmessage: ${d.message || '-'}`);
      setText('ollamaStatusChip', `ollama: ${d.reachable ? '🟢 connected' : '🔴 offline'}`);
    } catch (e) {
      setText('ollamaConnStatus', '조회 실패: ' + e.message);
    }
  }

  async function saveConn() {
    const url = val('ollamaBaseUrl');
    if (!url) { setText('ollamaConnStatus', '⚠ URL을 입력하세요.'); return; }
    try {
      await fetchJson('/debug/api/ollama/connection', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ baseUrl: url }),
      });
      await checkConn();
    } catch (e) {
      setText('ollamaConnStatus', '저장 실패: ' + e.message);
    }
  }

  async function resetConn() {
    try {
      await fetchJson('/debug/api/ollama/connection/reset', { method: 'POST' });
      await loadAll();
    } catch (e) {
      setText('ollamaConnStatus', '초기화 실패: ' + e.message);
    }
  }

  // ── 모델 목록 ──────────────────────────────────────────
  async function loadModels() {
    try {
      const list = await fetchJson('/debug/api/ollama/models?source=RUNNING');
      const models = (list || []).map(m => m.name || m.model || String(m));
      fillModelSelects(models);
      setText('modelStatus', `모델 ${models.length}개 조회 완료: ${models.join(', ')}`);
    } catch (e) {
      setText('modelStatus', '모델 조회 실패: ' + e.message);
    }
  }

  async function saveModels() {
    const body = {
      generalModel:      val('generalModel'),
      devModel:          val('devModel'),
      miceModel:         val('miceModel'),
      travelSearchModel: val('travelSearchModel'),
      travelPlanModel:   val('travelPlanModel'),
    };
    try {
      const d = await fetchJson('/debug/api/ollama/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      setText('modelStatus', '모델 저장 완료\n' + pretty(d));
    } catch (e) {
      setText('modelStatus', '모델 저장 실패: ' + e.message);
    }
  }

  async function resetModels() {
    try {
      const d = await fetchJson('/debug/api/ollama/config/reset', { method: 'POST' });
      setText('modelStatus', '모델 초기화 완료\n' + pretty(d));
      await loadAll();
    } catch (e) {
      setText('modelStatus', '초기화 실패: ' + e.message);
    }
  }

  // ── Resolver / Parser 모드 ────────────────────────────
  async function saveConfig() {
    const body = {
      resolverMode:      val('resolverMode'),
      generalParserMode: val('generalParserMode'),
      devParserMode:     val('devParserMode'),
      miceParserMode:    val('miceParserMode'),
      travelParserMode:  val('travelParserMode'),
    };
    try {
      const d = await fetchJson('/debug/api/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      setText('configStatus', '설정 저장 완료\n' + pretty(d));
    } catch (e) {
      setText('configStatus', '설정 저장 실패: ' + e.message);
    }
  }

  async function resetConfig() {
    try {
      const d = await fetchJson('/debug/api/config/reset', { method: 'POST' });
      setText('configStatus', '설정 초기화 완료\n' + pretty(d));
      await loadAll();
    } catch (e) {
      setText('configStatus', '초기화 실패: ' + e.message);
    }
  }

  // ── 즉시 테스트 ────────────────────────────────────────
  function runTest() {
    const message  = val('testMessage');
    const category = val('testCategory');
    if (!message) { logLine(qs('testLog'), '[error] 질문을 입력하세요.'); return; }
    if (testEs) { testEs.close(); testEs = null; }
    if (qs('testResult')) qs('testResult').textContent = '';
    if (qs('testLog'))    qs('testLog').textContent    = '';

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;

    logLine(qs('testLog'), `[request] ${message}`);
    testEs = startStream({
      url,
      onToken: data => appendResult(qs('testResult'), data),
      onAgent: ({ agent, status, message: msg }) =>
        logLine(qs('testLog'), `[${agent}] ${status} — ${msg}`),
      onComplete: () => logLine(qs('testLog'), '[complete] 완료'),
      onError:    () => logLine(qs('testLog'), '[error] 오류'),
    });
  }

  function clearTest() {
    if (qs('testResult')) qs('testResult').textContent = '';
    if (qs('testLog'))    qs('testLog').textContent    = '';
    if (testEs) { testEs.close(); testEs = null; }
  }

  // ── 전체 로드 ──────────────────────────────────────────
  async function loadAll() {
    // Ollama 연결 정보
    try {
      const conn = await fetchJson('/debug/api/ollama/connection');
      setVal('ollamaBaseUrl', conn.baseUrl || '');
      setText('ollamaConnStatus',
        `status: ${conn.status}\nreachable: ${conn.reachable}\nrunning: ${conn.runningCount}\ninstalled: ${conn.installedCount}`);
      setText('ollamaStatusChip', `ollama: ${conn.reachable ? '🟢 connected' : '🔴 offline'}`);
    } catch { /* 무시 */ }

    // 모델 설정 로드
    try {
      const cfg = await fetchJson('/debug/api/ollama/config');
      await loadModels();   // 목록 먼저
      selectVal('generalModel',      cfg.generalModel);
      selectVal('devModel',          cfg.devModel);
      selectVal('miceModel',         cfg.miceModel);
      selectVal('travelSearchModel', cfg.travelSearchModel);
      selectVal('travelPlanModel',   cfg.travelPlanModel);
    } catch { /* 무시 */ }

    // Resolver/Parser 모드
    try {
      const cfg = await fetchJson('/debug/api/config');
      selectVal('resolverMode',      cfg.resolverMode);
      selectVal('generalParserMode', cfg.generalParserMode);
      selectVal('devParserMode',     cfg.devParserMode);
      selectVal('miceParserMode',    cfg.miceParserMode);
      selectVal('travelParserMode',  cfg.travelParserMode);
      setText('fallbackChip',  `fallback: ${cfg.fallbackPolicy || '-'}`);
      setText('memoryChip',    `memory: ${cfg.memoryStore || '-'}`);
    } catch { /* 무시 */ }
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnCheckConn')?.addEventListener('click', checkConn);
    qs('btnSaveConn')?.addEventListener('click', saveConn);
    qs('btnResetConn')?.addEventListener('click', resetConn);
    qs('btnLoadModels')?.addEventListener('click', loadModels);
    qs('btnSaveModels')?.addEventListener('click', saveModels);
    qs('btnResetModels')?.addEventListener('click', resetModels);
    qs('btnSaveConfig')?.addEventListener('click', saveConfig);
    qs('btnResetConfig')?.addEventListener('click', resetConfig);
    qs('btnTest')?.addEventListener('click', runTest);
    qs('btnTestClear')?.addEventListener('click', clearTest);
    loadAll();
  });
})();
