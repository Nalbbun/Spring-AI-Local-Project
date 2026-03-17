(() => {
  const { qs, val, setText, fetchJson, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendResult } = window.ChatCommon;
  let es = null;
  let startTime = null;
  const steps = {};  // agent명 → DOM 요소

  const resultEl = () => qs('result');
  const logEl    = () => qs('eventLog');
  const stepsEl  = () => qs('agentSteps');

  function clearView() {
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML = '<div class="step-idle">에이전트를 실행하면 단계별 진행 상황이 표시됩니다.</div>';
    setText('elapsedTime', '-');
    Object.keys(steps).forEach(k => delete steps[k]);
    if (es) { es.close(); es = null; }
  }

  function upsertStep(agent, status, msg) {
    const container = stepsEl();
    if (!container) return;

    // 처음 등장 시 idle 문구 제거
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

  function send() {
    const message  = val('message');
    const category = val('category');
    if (!message) { logLine(logEl(), '[error] 메시지를 입력하세요.'); return; }
    if (es) { es.close(); es = null; }
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (stepsEl())  stepsEl().innerHTML    = '';
    Object.keys(steps).forEach(k => delete steps[k]);
    startTime = Date.now();

    const url = `/api/chat/stream?message=${encodeURIComponent(message)}&category=${encodeURIComponent(category)}`;
    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category}`);

    es = startStream({
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
      onError: () => logLine(logEl(), '[error] 스트림 오류'),
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
    } catch { /* local profile 아닌 경우 무시 */ }
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    loadModelInfo();
  });
})();
