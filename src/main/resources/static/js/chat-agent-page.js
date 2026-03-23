(() => {
  const { qs, val, setText, fetchJson, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendToken, showResultSkeleton, hideResultSkeleton } = window.ChatCommon;
  let es = null;
  let startTime = null;
  const steps = {};

  const resultEl = () => qs('result');
  const logEl    = () => qs('eventLog');
  const stepsEl  = () => qs('agentSteps');
  const tokenState = { text: '' };

  // ── 로딩 제어 ───────────────────────────────────────────
  function setLoading(active, text = '에이전트 실행 중...') {
    const ov = qs('loadingOverlay');
    const tx = qs('loadingText');
    if (tx) tx.textContent = text;
    ov?.classList.toggle('active', active);
    const btn = qs('btnSend');
    if (btn) btn.classList.toggle('btn-loading', active);
  }

  // 첫 토큰 수신 → 오버레이 숨김 + 스켈레톤 제거
  function onFirstToken() {
    setLoading(false);
    hideResultSkeleton(resultEl());
  }

  // ── 뷰 초기화 ───────────────────────────────────────────
  function clearView() {
    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML =
      '<div class="step-idle">에이전트를 실행하면 단계별 진행 상황이 표시됩니다.</div>';
    setText('elapsedTime', '-');
    Object.keys(steps).forEach(k => delete steps[k]);
    if (es) { es.close(); es = null; }
    setLoading(false);
  }

  // ── 에이전트 단계 카드 렌더링 ───────────────────────────
  function upsertStep(agent, status, msg) {
    const container = stepsEl();
    if (!container) return;
    container.querySelector('.step-idle')?.remove();

    const icon = { running: '⏳', complete: '✅', error: '❌', warning: '⚠️' }[status] || '🔵';
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

  // ── 전송 ────────────────────────────────────────────────
  function send() {
    const message  = val('message');
    const category = val('category');
    const promptId = window.PromptSelector?.selected('promptSelect') || '';
    if (!message) { logLine(logEl(), '[error] 메시지를 입력하세요.'); return; }
    if (es) { es.close(); es = null; }

    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML    = '';
    Object.keys(steps).forEach(k => delete steps[k]);
    startTime = Date.now();

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}&category=${encodeURIComponent(category)}`;
    if (promptId) url += `&promptId=${encodeURIComponent(promptId)}`;

    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category} | prompt: ${promptId || '기본'}`);

    // 오버레이 표시 + 스켈레톤 삽입
    setLoading(true, '에이전트 실행 중...');
    showResultSkeleton(resultEl());

    es = startStream({
      url,
      onFirstToken,   // 첫 토큰 → 오버레이 & 스켈레톤 숨김
      onToken: token => appendToken(resultEl(), token, tokenState),
      onAgent: ({ agent, status, message: msg }) => {
        logLine(logEl(), `[${agent}] ${status} — ${msg}`);
        upsertStep(agent, status, msg);
        // 단계 진행에 따라 로딩 텍스트 업데이트
        const tx = qs('loadingText');
        if (tx && status === 'running') tx.textContent = `${agent} 처리 중...`;
      },
      onComplete: () => {
        logLine(logEl(), '[complete] 에이전트 실행 완료');
        setLoading(false);
        hideResultSkeleton(resultEl());
        if (startTime) setText('elapsedTime', ((Date.now() - startTime) / 1000).toFixed(1) + 's');
      },
      onError: () => {
        logLine(logEl(), '[error] 스트림 오류');
        setLoading(false);
        hideResultSkeleton(resultEl());
        if (startTime) setText('elapsedTime', ((Date.now() - startTime) / 1000).toFixed(1) + 's (오류)');
      },
    });
  }

  async function memoryClear() {
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      logLine(logEl(), '[memory] 대화 초기화 완료');
    } catch (e) {
      logLine(logEl(), '[memory] 초기화 실패: ' + e.message);
    }
  }

  async function loadModelInfo() {
    try {
      const cfg = await fetchJson('/debug/api/ollama/config');
      setText('agentModelChip', `search: ${cfg?.travelSearchModel || '-'}`);
      setText('planModelChip',  `plan: ${cfg?.travelPlanModel || '-'}`);
    } catch { /* local profile 외에서는 무시 */ }
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    window.ChatMemoryPanel?.init();
    window.PromptSelector?.init('promptSelect', 'TRAVEL');
    loadModelInfo();
  });
})();
