(() => {
  const { qs, val, setText, fetchJson } = window.UiCommon;
  const { startStream, logLine, appendResult } = window.ChatCommon;
  let es = null;

  const resultEl   = () => qs('result');
  const logEl      = () => qs('eventLog');
  const overlayEl  = () => qs('loadingOverlay');
  const loadingTxt = () => qs('loadingText');

  function setLoading(active, text = '처리 중...') {
    if (loadingTxt()) loadingTxt().textContent = text;
    overlayEl()?.classList.toggle('active', active);
  }

  function clearView() {
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (es) { es.close(); es = null; }
  }

  function send() {
    const message  = val('message');
    const category = val('category');
    if (!message) { logLine(logEl(), '[error] 메시지를 입력하세요.'); return; }
    if (es) { es.close(); es = null; }
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;

    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category || 'AUTO'}`);
    setLoading(true, '응답 생성 중...');

    es = startStream({
      url,
      onToken:    data => appendResult(resultEl(), data),
      onAgent:    ({ agent, status, message: msg }) => logLine(logEl(), `[agent:${agent}] ${status} — ${msg}`),
      onComplete: ()   => { logLine(logEl(), '[complete] 스트림 종료'); setLoading(false); },
      onError:    ()   => { logLine(logEl(), '[error] 스트림 오류'); setLoading(false); },
    });
  }

  async function memoryClear() {
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      logLine(logEl(), '[memory] 대화 초기화 완료');
      setText('statusMemory', 'memory: cleared');
    } catch (e) {
      logLine(logEl(), '[memory] 초기화 실패: ' + e.message);
    }
  }

  async function loadStatus() {
    try {
      const data = await fetchJson('/api/runtime/ollama');
      setText('statusModel', `model: ${data?.reachable ? 'Ollama' : 'OpenAI'}`);
    } catch { setText('statusModel', 'model: -'); }
    try {
      const cfg = await fetchJson('/debug/api/config');
      setText('statusMemory', `memory: ${cfg?.memoryStore || '-'}`);
    } catch { /* 로컬 프로파일 아닌 경우 무시 */ }
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    loadStatus();
  });
})();
