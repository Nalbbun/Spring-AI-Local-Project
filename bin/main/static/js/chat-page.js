(() => {
  const { qs, val, setText, fetchJson } = window.UiCommon;
  const { startStream, logLine, appendToken } = window.ChatCommon;
  let es = null;

  const resultEl   = () => qs('result');
  const logEl      = () => qs('eventLog');
  const overlayEl  = () => qs('loadingOverlay');
  const loadingTxt = () => qs('loadingText');
  const tokenState = { text: '' };

  function setLoading(active, text = '처리 중...') {
    if (loadingTxt()) loadingTxt().textContent = text;
    overlayEl()?.classList.toggle('active', active);
  }

  function clearView() {
    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (es) { es.close(); es = null; }
  }

  function send() {
    const message   = val('message');
    const category  = val('category');
    const promptId  = window.PromptSelector?.selected('promptSelect') || '';
    if (!message) { logLine(logEl(), '[error] 메시지를 입력하세요.'); return; }
    if (es) { es.close(); es = null; }

    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    if (promptId) url += `&promptId=${encodeURIComponent(promptId)}`;

    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category || 'AUTO'} | prompt: ${promptId || '기본'}`);
    setLoading(true, '응답 생성 중...');

    es = startStream({
      url,
      onToken: token => appendToken(resultEl(), token, tokenState),
      onAgent: ({ agent, status, message: msg }) =>
        logLine(logEl(), `[agent:${agent}] ${status} — ${msg}`),
      onComplete: () => { logLine(logEl(), '[complete] 스트림 종료'); setLoading(false); },
      onError:    () => { logLine(logEl(), '[error] 스트림 오류');    setLoading(false); },
    });
  }

  async function memoryClear() {
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      logLine(logEl(), '[memory] 대화 초기화 완료');
      setText('statusMemory', 'memory: cleared');
    } catch (e) { logLine(logEl(), '[memory] 초기화 실패: ' + e.message); }
  }

  async function loadStatus() {
    try {
      const data = await fetchJson('/api/runtime/ollama');
      setText('statusModel', `model: ${data?.reachable ? 'Ollama' : 'OpenAI'}`);
    } catch { setText('statusModel', 'model: -'); }
    try {
      const cfg = await fetchJson('/debug/api/config');
      setText('statusMemory', `memory: ${cfg?.memoryStore || '-'}`);
    } catch { /* local profile 외에서는 무시 */ }
  }

  // 카테고리 변경 시 프롬프트 목록 갱신
  function onCategoryChange() {
    const cat = val('category');
    window.PromptSelector?.init('promptSelect', cat || null);
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    qs('category')?.addEventListener('change', onCategoryChange);
    // 메모리 인라인 패널
    window.ChatMemoryPanel?.init();
    // 프롬프트 목록 초기 로드
    window.PromptSelector?.init('promptSelect', null);
    loadStatus();
  });
})();
