/**
 * settings-page.js
 * ① Ollama 연결  ② 카테고리별 모델  ③ 모델 브라우저/Pull/Run
 * ④ RAG 설정    ⑤ 메모리 저장소  ⑥ Resolver/Parser 모드  ⑦ 즉시 테스트
 */
(() => {
  const { qs, val, setVal, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendToken } = window.ChatCommon;
  let testEs = null;
  const testTokenState = { text: '' };

  // ── 공통 유틸 ──────────────────────────────────────────
  function selectSet(id, value) {
    const el = qs(id);
    if (!el || value == null) return;
    const str = String(value);
    [...el.options].forEach(o => { o.selected = o.value === str; });
  }

  function fillModelSelects(models) {
    const ids = ['generalModel','devModel','miceModel','travelSearchModel','travelPlanModel'];
    ids.forEach(id => {
      const sel = qs(id);
      if (!sel) return;
      const cur = sel.value;
      sel.innerHTML = '<option value="">-- 선택 --</option>' +
        models.map(m => `<option value="${htmlEscape(m)}">${htmlEscape(m)}</option>`).join('');
      if (cur) selectSet(id, cur);
    });
  }

  // ══════════════════════════════════════════════════════
  // ① Ollama 연결
  // ══════════════════════════════════════════════════════
  async function checkConn() {
    try {
      const d = await fetchJson('/debug/api/ollama/connection');
      setText('ollamaConnStatus',
        `status: ${d.status}  reachable: ${d.reachable}\n` +
        `running: ${d.runningCount}  installed: ${d.installedCount}\n` +
        `message: ${d.message || '-'}`);
      setText('ollamaStatusChip', `ollama: ${d.reachable ? '🟢 ok' : '🔴 offline'}`);
    } catch (e) {
      setText('ollamaConnStatus', '조회 실패: ' + e.message);
      setText('ollamaStatusChip', 'ollama: ❌ error');
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

  // ══════════════════════════════════════════════════════
  // ② 카테고리별 모델
  // ══════════════════════════════════════════════════════
  async function loadModels(silent) {
    try {
      const list   = await fetchJson('/debug/api/ollama/models?source=RUNNING');
      const models = (list || []).map(m => m.name || m.model || '').filter(Boolean);
      fillModelSelects(models);
      if (!silent)
        setText('modelStatus', `RUNNING 모델 ${models.length}개: ${models.join(', ') || '(없음)'}`);
      return models;
    } catch (e) {
      if (!silent) setText('modelStatus', '모델 조회 실패: ' + e.message);
      return [];
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
        method: 'POST', headers: { 'Content-Type': 'application/json' },
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

  // ══════════════════════════════════════════════════════
  // ③ 모델 브라우저 / Pull / Run
  // ══════════════════════════════════════════════════════
  async function browseModels() {
    const source  = val('modelBrowserSource') || 'RUNNING';
    const tbody   = qs('modelBrowserTable');
    const statusEl = qs('modelBrowserStatus');

    if (statusEl) statusEl.textContent = '조회 중...';
    if (tbody)    tbody.innerHTML = '<tr><td colspan="5">조회 중...</td></tr>';

    try {
      const list = await fetchJson(
        `/debug/api/ollama/models?source=${encodeURIComponent(source)}`
      );

      if (!list || !list.length) {
        tbody.innerHTML = '<tr><td colspan="5">모델 없음 (Ollama 실행 여부 및 baseUrl 확인)</td></tr>';
        if (statusEl) statusEl.textContent = `${source} 모델 0개`;
        return;
      }

      tbody.innerHTML = list.map(m => {
        const name   = m.name || m.model || '-';
        const state  = m.state || '-';
        const sizeGb = m.size ? (m.size / 1e9).toFixed(1) + ' GB' : '-';
        const mod    = (m.modifiedAt || '').substring(0, 10) || '-';
        const color  = state.toUpperCase().includes('RUNNING') ? '#34d399' : '#94a3b8';
        const safe   = name.replace(/'/g, "\\'");
        return `<tr>
          <td><code style="font-size:13px">${htmlEscape(name)}</code></td>
          <td><span style="color:${color}">${htmlEscape(state)}</span></td>
          <td>${sizeGb}</td>
          <td>${mod}</td>
          <td>
            <div style="display:flex;gap:6px">
              <button class="green"   style="width:auto;padding:4px 8px;font-size:12px"
                onclick="window._quickRun('${safe}')">▶ Run</button>
              <button class="primary" style="width:auto;padding:4px 8px;font-size:12px"
                onclick="window._quickPull('${safe}')">⬇ Pull</button>
            </div>
          </td>
        </tr>`;
      }).join('');

      if (statusEl) statusEl.textContent = `${source} 모델 ${list.length}개 조회 완료`;
      fillModelSelects(list.map(m => m.name || m.model || '').filter(Boolean));

    } catch (e) {
      if (tbody) tbody.innerHTML =
        `<tr><td colspan="5" style="color:#f87171">조회 실패: ${htmlEscape(e.message)}<br>
         <span style="font-size:12px;color:#94a3b8">Ollama 실행 상태 및 baseUrl을 확인하세요.</span></td></tr>`;
      if (statusEl) statusEl.textContent = '조회 실패';
    }
  }

  window._quickRun  = name => { setVal('actionModelName', name); selectSet('actionPull','false'); runModelAction(); };
  window._quickPull = name => { setVal('actionModelName', name); selectSet('actionPull','true');  runModelAction(); };

  async function applyResidentModels() {
    const list      = val('residentModelList');
    const keepAlive = val('residentKeepAlive') || '24h';
    if (!list.trim()) { setText('modelActionStatus', '⚠ 상주 모델 목록을 입력하세요.'); return; }

    const models = list.split(/[\n,]+/).map(s => s.trim()).filter(Boolean);
    setText('modelActionStatus', `상주 모델 ${models.length}개 로드 중 (keepAlive=${keepAlive})...`);

    try {
      await fetchJson('/debug/api/ollama/config', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ residentModelList: list, residentKeepAlive: keepAlive }),
      });
    } catch { /* config 저장 실패 무시 */ }

    const lines = [];
    for (const model of models) {
      try {
        const r = await fetchJson('/debug/api/ollama/models/action', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ model, pull: false, keepAlive }),
        });
        lines.push(`${r.success ? '✅' : '❌'} ${model} — ${r.message || (r.success ? 'ok' : 'fail')}`);
      } catch (e) {
        lines.push(`❌ ${model} — ${e.message}`);
      }
    }
    setText('modelActionStatus',
      `상주 모델 로드 완료 (keepAlive=${keepAlive})\n` + lines.join('\n'));
    await browseModels();
  }

  async function runModelAction() {
    const model     = val('actionModelName').trim();
    const pull      = val('actionPull') === 'true';
    const keepAlive = val('actionKeepAlive') || '24h';
    if (!model) { setText('modelActionStatus', '⚠ 모델명을 입력하세요.'); return; }

    setText('modelActionStatus',
      `${pull ? 'Pull 영구 설치' : `PS 로드 (keepAlive=${keepAlive})`} 실행 중: ${model}...`);
    try {
      const r = await fetchJson('/debug/api/ollama/models/action', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ model, pull, keepAlive }),
      });
      setText('modelActionStatus',
        `작업 완료\naction: ${r.action||'-'}  model: ${r.model||'-'}\n` +
        `keepAlive: ${r.keepAlive||'-'}  success: ${r.success}\n` +
        `running: ${r.runningCount??'-'}  installed: ${r.installedCount??'-'}\n` +
        `message: ${r.message||'-'}` +
        (r.runningModels?.length ? '\n\nRUNNING 모델\n' + r.runningModels.map(m=>`  · ${m}`).join('\n') : ''));
      await browseModels();
    } catch (e) {
      setText('modelActionStatus', '작업 실패: ' + e.message);
    }
  }

  // ══════════════════════════════════════════════════════
  // ④ RAG 설정
  // ══════════════════════════════════════════════════════
  async function loadRagConfig() {
    try {
      const s    = await fetchJson('/debug/api/rag/status');
      const cats = s.categories || {};
      const ing  = s.ingest     || {};

      selectSet('ragEnabled',          s.enabled);
      selectSet('ragIncludeCitations', s.includeCitations ?? true);
      if (s.topK != null)                setVal('ragTopK',                s.topK);
      if (s.similarityThreshold != null) setVal('ragSimilarityThreshold', s.similarityThreshold);
      selectSet('ragGeneralEnabled', cats.general ?? false);
      selectSet('ragDevEnabled',     cats.dev     ?? true);
      selectSet('ragMiceEnabled',    cats.mice    ?? true);
      selectSet('ragTravelEnabled',  cats.travel  ?? false);
      if (ing.chunkSize)    setVal('ragChunkSize',    ing.chunkSize);
      if (ing.maxNumChunks) setVal('ragMaxNumChunks', ing.maxNumChunks);

      setText('ragConfigStatus',
        `RAG 설정 조회 완료\nenabled: ${s.enabled}  vectorStore: ${s.vectorStore}\n` +
        `topK: ${s.topK}  threshold: ${s.similarityThreshold}\n` +
        `카테고리 — GEN:${cats.general} DEV:${cats.dev} MICE:${cats.mice} TRAVEL:${cats.travel}`);
      setText('ragStatusChip', `rag: ${s.enabled ? '✅ on' : '❌ off'}`);
    } catch (e) {
      setText('ragConfigStatus', 'RAG 설정 조회 실패: ' + e.message);
    }
  }

  async function saveRagConfig() {
    const body = {
      enabled:             val('ragEnabled') === 'true',
      topK:                Number(val('ragTopK') || 4),
      similarityThreshold: Number(val('ragSimilarityThreshold') || 0.72),
      includeCitations:    val('ragIncludeCitations') === 'true',
      generalEnabled:      val('ragGeneralEnabled') === 'true',
      devEnabled:          val('ragDevEnabled')     === 'true',
      miceEnabled:         val('ragMiceEnabled')    === 'true',
      travelEnabled:       val('ragTravelEnabled')  === 'true',
      chunkSize:           Number(val('ragChunkSize')    || 350),
      maxNumChunks:        Number(val('ragMaxNumChunks') || 128),
    };
    try {
      const d    = await fetchJson('/debug/api/rag/config', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const cats = d.categories || {};
      setText('ragConfigStatus',
        `RAG 설정 저장 완료\nenabled: ${d.enabled}  topK: ${d.topK}  threshold: ${d.similarityThreshold}\n` +
        `카테고리 — GEN:${cats.general} DEV:${cats.dev} MICE:${cats.mice} TRAVEL:${cats.travel}`);
      setText('ragStatusChip', `rag: ${d.enabled ? '✅ on' : '❌ off'}`);
    } catch (e) {
      setText('ragConfigStatus', 'RAG 설정 저장 실패: ' + e.message);
    }
  }

  // ══════════════════════════════════════════════════════
  // ⑤ 메모리 저장소 설정
  // ══════════════════════════════════════════════════════

  const STORE_HINTS = {
    'in-memory': '서버 메모리에만 저장합니다. 재시작 시 모든 대화가 사라집니다. 개발·테스트용으로 적합합니다.',
    'jdbc':      'PostgreSQL에 영구 저장합니다. SPRING_DATASOURCE_URL 등 DB 연결 설정이 필요합니다.',
    'redis':     'Redis에 TTL 기반으로 저장합니다. SPRING_DATA_REDIS_HOST 설정이 필요합니다.',
  };

  function updateMemoryStoreUI() {
    const store = qs('memoryStoreSelect')?.value || 'in-memory';

    // 힌트 텍스트
    const hintEl = qs('memoryStoreHint');
    if (hintEl) hintEl.textContent = STORE_HINTS[store] || '';

    // 환경변수 입력창
    const envEl = qs('memoryStoreEnvValue');
    if (envEl) envEl.value = `APP_MEMORY_STORE=${store}`;

    // 섹션 표시/숨김
    const redisSection = qs('redisConfigSection');
    const jdbcSection  = qs('jdbcConfigSection');
    if (redisSection) redisSection.style.display = store === 'redis' ? '' : 'none';
    if (jdbcSection)  jdbcSection.style.display  = store === 'jdbc'  ? '' : 'none';

    // Redis 환경변수 블록 업데이트
    if (store === 'redis') updateRedisEnvBlock();
  }

  function updateRedisEnvBlock() {
    const host     = val('redisHost')     || 'localhost';
    const port     = val('redisPort')     || '6379';
    const password = val('redisPassword') || '';
    const ttl      = val('redisTtlHours') || '24';
    const block = qs('redisEnvBlock');
    if (!block) return;
    block.textContent =
      `APP_MEMORY_STORE=redis\n` +
      `SPRING_DATA_REDIS_HOST=${host}\n` +
      `SPRING_DATA_REDIS_PORT=${port}\n` +
      (password ? `SPRING_DATA_REDIS_PASSWORD=${password}\n` : '') +
      `APP_MEMORY_REDIS_TTL_HOURS=${ttl}`;
  }

  async function loadMemoryConfig() {
    try {
      const cfg = await fetchJson('/debug/api/config');
      const store       = cfg.memoryStore       || 'in-memory';
      const serviceType = cfg.memoryServiceType || '-';

      setText('memoryStoreType', storeLabel(store));
      setText('memoryServiceClass', serviceType);
      setText('memoryChip', `memory: ${store}`);

      // select 동기화
      const sel = qs('memoryStoreSelect');
      if (sel) {
        [...sel.options].forEach(o => { o.selected = o.value === store; });
        updateMemoryStoreUI();
      }

      setText('memoryConfigStatus',
        `메모리 설정 조회 완료\nstore: ${store}\nclass: ${serviceType}`);
    } catch (e) {
      setText('memoryConfigStatus', '메모리 설정 조회 실패: ' + e.message);
    }
  }

  function storeLabel(store) {
    const map = { 'in-memory': '💡 in-memory', 'jdbc': '🐘 jdbc (PostgreSQL)', 'redis': '🔴 redis' };
    return map[store] || store;
  }

  async function applyRedisTtl() {
    const ttl = Number(val('redisTtlHours') || 24);
    if (!ttl || ttl < 1) { setText('memoryConfigStatus', '⚠ TTL은 1 이상이어야 합니다.'); return; }
    try {
      // Redis TTL은 환경변수 기반이라 런타임 API가 없으므로
      // /debug/api/config POST로 전달 시도 (서버에서 처리 가능한 경우 반영됨)
      setText('memoryConfigStatus',
        `Redis TTL 설정: ${ttl}h\n` +
        `⚠ 적용하려면 APP_MEMORY_REDIS_TTL_HOURS=${ttl} 환경변수 설정 후 재시작하세요.`);
      updateRedisEnvBlock();
    } catch (e) {
      setText('memoryConfigStatus', 'TTL 적용 실패: ' + e.message);
    }
  }

  async function checkJdbc() {
    try {
      const d = await fetchJson('/debug/api/rag/db-info');
      setText('jdbcInfoPanel',
        `연결: ${d?.jdbc?.connected ? '✅ 정상' : '❌ 실패'}\n` +
        `URL: ${d?.jdbc?.url || '-'}\n` +
        `대화 메시지 rows: ${d?.conversationMessageRows ?? '-'}\n` +
        `대화 요약 rows: ${d?.conversationSummaryRows ?? '-'}`);
    } catch (e) {
      setText('jdbcInfoPanel', 'DB 연결 확인 실패: ' + e.message);
    }
  }

  function copyToClipboard(text, successMsg = '복사됨') {
    navigator.clipboard?.writeText(text)
      .then(() => setText('memoryConfigStatus', `📋 ${successMsg}`))
      .catch(() => {
        // 폴백
        const ta = document.createElement('textarea');
        ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
        document.body.appendChild(ta); ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        setText('memoryConfigStatus', `📋 ${successMsg}`);
      });
  }

  // ⑥ Resolver / Parser 모드
  // ══════════════════════════════════════════════════════
  // ══════════════════════════════════════════════════════
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
        method: 'POST', headers: { 'Content-Type': 'application/json' },
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
      setText('configStatus', '초기화 완료\n' + pretty(d));
      await loadAll();
    } catch (e) {
      setText('configStatus', '초기화 실패: ' + e.message);
    }
  }

  // ══════════════════════════════════════════════════════
  // ⑥ 즉시 테스트
  // ══════════════════════════════════════════════════════
  function runTest() {
    const message  = val('testMessage');
    const category = val('testCategory');
    const promptId = window.PromptSelector?.selected('testPromptSelect') || '';
    if (!message) { logLine(qs('testLog'), '[error] 질문을 입력하세요.'); return; }
    if (testEs) { testEs.close(); testEs = null; }
    testTokenState.text = '';
    if (qs('testResult')) qs('testResult').textContent = '';
    if (qs('testLog'))    qs('testLog').textContent    = '';

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    if (promptId) url += `&promptId=${encodeURIComponent(promptId)}`;
    logLine(qs('testLog'), `[request] ${message} | prompt: ${promptId || '기본'}`);

    testEs = startStream({
      url,
      onToken: token => appendToken(qs('testResult'), token, testTokenState),
      onAgent: ({ agent, status, message: msg }) =>
        logLine(qs('testLog'), `[${agent}] ${status} — ${msg}`),
      onComplete: () => logLine(qs('testLog'), '[complete] 완료'),
      onError:    () => logLine(qs('testLog'), '[error] 오류'),
    });
  }

  // ══════════════════════════════════════════════════════
  // 전체 초기 로드
  // ══════════════════════════════════════════════════════
  async function loadAll() {
    try {
      const conn = await fetchJson('/debug/api/ollama/connection');
      setVal('ollamaBaseUrl', conn.baseUrl || '');
      setText('ollamaConnStatus',
        `status: ${conn.status}  reachable: ${conn.reachable}\n` +
        `running: ${conn.runningCount}  installed: ${conn.installedCount}`);
      setText('ollamaStatusChip', `ollama: ${conn.reachable ? '🟢 ok' : '🔴 offline'}`);
    } catch { /* 무시 */ }

    const models = await loadModels(true);
    try {
      const cfg = await fetchJson('/debug/api/ollama/config');
      if (!models.length) fillModelSelects(
        [cfg.generalModel,cfg.devModel,cfg.miceModel,cfg.travelSearchModel,cfg.travelPlanModel]
          .filter(Boolean));
      selectSet('generalModel',      cfg.generalModel);
      selectSet('devModel',          cfg.devModel);
      selectSet('miceModel',         cfg.miceModel);
      selectSet('travelSearchModel', cfg.travelSearchModel);
      selectSet('travelPlanModel',   cfg.travelPlanModel);
      if (cfg.residentModelList && qs('residentModelList'))
        qs('residentModelList').value = cfg.residentModelList;
      if (cfg.residentKeepAlive && qs('residentKeepAlive'))
        qs('residentKeepAlive').value = cfg.residentKeepAlive;
    } catch { /* 무시 */ }

    await browseModels();
    await loadRagConfig();
    await loadMemoryConfig();

    try {
      const cfg = await fetchJson('/debug/api/config');
      selectSet('resolverMode',      cfg.resolverMode);
      selectSet('generalParserMode', cfg.generalParserMode);
      selectSet('devParserMode',     cfg.devParserMode);
      selectSet('miceParserMode',    cfg.miceParserMode);
      selectSet('travelParserMode',  cfg.travelParserMode);
      setText('fallbackChip', `fallback: ${cfg.fallbackPolicy || '-'}`);
    } catch { /* 무시 */ }
  }

  // ══════════════════════════════════════════════════════
  // ⑦ 카테고리별 모델 우선순위
  // ══════════════════════════════════════════════════════
  const PRIORITY_OPTIONS = [
    { value: 'OLLAMA_FIRST',  label: '🟢 OLLAMA_FIRST',  desc: 'Ollama 우선, 실패 시 OpenAI' },
    { value: 'OPENAI_FIRST',  label: '🔵 OPENAI_FIRST',  desc: 'OpenAI 우선 (유료)' },
    { value: 'OLLAMA_ONLY',   label: '⚫ OLLAMA_ONLY',   desc: 'Ollama 전용, OpenAI 차단' },
    { value: 'OPENAI_ONLY',   label: '🟣 OPENAI_ONLY',   desc: 'OpenAI 전용 (유료)' },
  ];

  const PRIORITY_TARGETS = [
    { key: 'GENERAL',       label: '💬 GENERAL' },
    { key: 'DEV',           label: '💻 DEV' },
    { key: 'MICE',          label: '🎪 MICE' },
    { key: 'TRAVEL_SEARCH', label: '✈️ TRAVEL Search' },
    { key: 'TRAVEL_PLAN',   label: '🗺 TRAVEL Plan' },
  ];

  function renderPriorityGrid(data) {
    const grid = qs('priorityGrid');
    if (!grid) return;
    grid.innerHTML = PRIORITY_TARGETS.map(t => {
      const current = (data[t.key] && data[t.key].priority) || 'OLLAMA_FIRST';
      const opts = PRIORITY_OPTIONS.map(o =>
        `<option value="${o.value}" ${o.value === current ? 'selected' : ''}>${o.label}</option>`
      ).join('');
      return `<div style="background:#0b1220;border:1px solid #1e293b;border-radius:10px;padding:12px">
        <div style="font-weight:700;font-size:13px;color:#e2e8f0;margin-bottom:8px">${t.label}</div>
        <select id="priority_${t.key}" style="font-size:12px">${opts}</select>
        <div style="font-size:11px;color:#64748b;margin-top:4px" id="priorityDesc_${t.key}">
          ${PRIORITY_OPTIONS.find(o => o.value === current)?.desc || ''}
        </div>
      </div>`;
    }).join('');

    // select 변경 시 설명 업데이트
    PRIORITY_TARGETS.forEach(t => {
      const sel = qs('priority_' + t.key);
      const desc = qs('priorityDesc_' + t.key);
      if (sel && desc) {
        sel.addEventListener('change', () => {
          desc.textContent = PRIORITY_OPTIONS.find(o => o.value === sel.value)?.desc || '';
        });
      }
    });
  }

  async function loadPriority() {
    try {
      const data = await fetchJson('/api/model-priority');
      renderPriorityGrid(data);
      const summary = PRIORITY_TARGETS.map(t => {
        const p = (data[t.key] && data[t.key].priority) || '?';
        return `${t.label}: ${p}`;
      }).join('  |  ');
      setText('priorityStatus', '현재 설정 — ' + summary);
    } catch (e) { setText('priorityStatus', '조회 실패: ' + e.message); }
  }

  async function savePriority() {
    const body = {};
    PRIORITY_TARGETS.forEach(t => {
      const sel = qs('priority_' + t.key);
      if (sel) body[t.key] = sel.value;
    });
    setText('priorityStatus', '저장 중...');
    try {
      const data = await fetchJson('/api/model-priority', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      renderPriorityGrid(data);
      const summary = PRIORITY_TARGETS.map(t => {
        const p = (data[t.key] && data[t.key].priority) || '?';
        return `${t.label}: ${p}`;
      }).join('  |  ');
      setText('priorityStatus', '✅ 저장 완료 — ' + summary);
    } catch (e) { setText('priorityStatus', '저장 실패: ' + e.message); }
  }

  async function resetPriority() {
    if (!confirm('모든 카테고리를 OLLAMA_FIRST로 초기화할까요?')) return;
    try {
      const data = await fetchJson('/api/model-priority/reset', { method: 'POST' });
      renderPriorityGrid(data);
      setText('priorityStatus', '↩ 초기화 완료 — 모든 카테고리 OLLAMA_FIRST');
    } catch (e) { setText('priorityStatus', '초기화 실패: ' + e.message); }
  }

  // ══════════════════════════════════════════════════════
  // 이벤트 바인딩
  // ══════════════════════════════════════════════════════
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnCheckConn')?.addEventListener('click', checkConn);
    qs('btnSaveConn')?.addEventListener('click', saveConn);
    qs('btnResetConn')?.addEventListener('click', resetConn);

    qs('btnLoadModels')?.addEventListener('click', () => loadModels(false));
    qs('btnSaveModels')?.addEventListener('click', saveModels);
    qs('btnResetModels')?.addEventListener('click', resetModels);

    qs('btnBrowseModels')?.addEventListener('click', browseModels);
    qs('btnApplyResident')?.addEventListener('click', applyResidentModels);
    qs('btnRunAction')?.addEventListener('click', runModelAction);

    qs('btnLoadRagConfig')?.addEventListener('click', loadRagConfig);
    qs('btnSaveRagConfig')?.addEventListener('click', saveRagConfig);

    // ⑤ 메모리 저장소 설정
    qs('btnLoadMemoryConfig')?.addEventListener('click', loadMemoryConfig);
    qs('btnApplyRedisTtl')?.addEventListener('click', applyRedisTtl);
    qs('btnCheckJdbc')?.addEventListener('click', checkJdbc);
    qs('btnCopyMemoryEnv')?.addEventListener('click', () =>
      copyToClipboard(val('memoryStoreEnvValue'), '환경변수 복사됨'));
    qs('btnCopyRedisEnv')?.addEventListener('click', () =>
      copyToClipboard(qs('redisEnvBlock')?.textContent || '', 'Redis 환경변수 복사됨'));
    qs('memoryStoreSelect')?.addEventListener('change', updateMemoryStoreUI);
    ['redisHost','redisPort','redisPassword','redisTtlHours'].forEach(id =>
      qs(id)?.addEventListener('input', updateRedisEnvBlock));

    qs('btnSaveConfig')?.addEventListener('click', saveConfig);
    qs('btnResetConfig')?.addEventListener('click', resetConfig);

    // ⑦ 우선순위
    qs('btnSavePriority')?.addEventListener('click',  savePriority);
    qs('btnResetPriority')?.addEventListener('click', resetPriority);
    qs('btnLoadPriority')?.addEventListener('click',  loadPriority);

    qs('btnTest')?.addEventListener('click', runTest);
    qs('btnTestClear')?.addEventListener('click', () => {
      testTokenState.text = '';
      if (qs('testResult')) qs('testResult').textContent = '';
      if (qs('testLog'))    qs('testLog').textContent    = '';
      if (testEs) { testEs.close(); testEs = null; }
    });
    qs('testMessage')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) runTest();
    });
    // 테스트 카테고리 변경 시 프롬프트 목록 갱신
    qs('testCategory')?.addEventListener('change', () => {
      window.PromptSelector?.init('testPromptSelect', val('testCategory') || null);
    });
    // 프롬프트 초기 로드
    window.PromptSelector?.init('testPromptSelect', null);

    loadAll();
    loadPriority();  // 우선순위 초기 로드
  });
})();
