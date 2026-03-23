(() => {
  const { qs, val, setText, fetchJson } = window.UiCommon;
  const { startStream, logLine, appendToken, showResultSkeleton, hideResultSkeleton } = window.ChatCommon;
  let es = null;

  const resultEl   = () => qs('result');
  const logEl      = () => qs('eventLog');
  const overlayEl  = () => qs('loadingOverlay');
  const loadingTxt = () => qs('loadingText');
  const tokenState = { text: '' };

  // 오버레이 + 버튼 제어
  function setLoading(active, text = '응답 생성 중...') {
    if (loadingTxt()) loadingTxt().textContent = text;
    overlayEl()?.classList.toggle('active', active);
    const btn = qs('btnSend');
    if (btn) btn.classList.toggle('btn-loading', active);
  }

  // 첫 토큰 수신 → 오버레이 숨기고 스켈레톤 제거
  function onFirstToken() {
    setLoading(false);
    hideResultSkeleton(resultEl());
  }

  function clearView() {
    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    if (es) { es.close(); es = null; }
    setLoading(false);
  }

  function send() {
    const message  = val('message');
    const category = val('category');
    const promptId = window.PromptSelector?.selected('promptSelect') || '';
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

    // 로딩 표시 + 스켈레톤 삽입
    setLoading(true, '응답 생성 중...');
    showResultSkeleton(resultEl());

    es = startStream({
      url,
      onFirstToken,  // 첫 토큰 → 오버레이 & 스켈레톤 숨김
      onToken:    token => appendToken(resultEl(), token, tokenState),
      onAgent:    ({ agent, status, message: msg }) =>
                    logLine(logEl(), `[agent:${agent}] ${status} — ${msg}`),
      onComplete: () => {
        logLine(logEl(), '[complete] 스트림 종료');
        setLoading(false);
        hideResultSkeleton(resultEl());
      },
      onError: () => {
        logLine(logEl(), '[error] 스트림 오류');
        setLoading(false);
        hideResultSkeleton(resultEl());
      },
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

  function onCategoryChange() {
    window.PromptSelector?.init('promptSelect', val('category') || null);
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    qs('category')?.addEventListener('change', onCategoryChange);
    window.ChatMemoryPanel?.init();
    window.PromptSelector?.init('promptSelect', null);
    loadStatus();
  });
})();
