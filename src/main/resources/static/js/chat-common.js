/**
 * chat-common.js
 * SSE 채팅 공통 유틸. 모든 채팅 페이지에서 공유합니다.
 */
window.ChatCommon = (() => {

  /**
   * SSE 채팅 스트림 시작
   * @param {Object} opts
   *   url        - EventSource URL
   *   onToken    - 'message' 이벤트 콜백 (data: string)
   *   onAgent    - 'agent'   이벤트 콜백 (data: {agent,status,message})
   *   onComplete - 'complete' 콜백
   *   onError    - 'error'   콜백 (event)
   * @returns EventSource
   */
  function startStream({ url, onToken, onAgent, onComplete, onError }) {
    const es = new EventSource(url);
    es.addEventListener('message', e => onToken?.(e.data));
    es.addEventListener('agent', e => {
      try { onAgent?.(JSON.parse(e.data)); }
      catch { onAgent?.({ agent: '?', status: 'raw', message: e.data }); }
    });
    es.addEventListener('complete', () => { onComplete?.(); es.close(); });
    es.addEventListener('error', e => { onError?.(e); es.close(); });
    return es;
  }

  /**
   * 이벤트 로그 패널에 한 줄 추가
   * @param {HTMLElement} el - 로그 패널 DOM
   * @param {string} text
   */
  function logLine(el, text) {
    if (!el) return;
    el.textContent += text + '\n';
    el.scrollTop = el.scrollHeight;
  }

  /**
   * agent 이벤트를 로그 패널에 출력
   */
  function logAgent(el, { agent, status, message }) {
    logLine(el, `[${agent}] ${status} — ${message}`);
  }

  /**
   * 응답 패널 텍스트를 스트리밍으로 누적
   */
  function appendResult(el, text) {
    if (!el) return;
    el.textContent = text;
    el.scrollTop = el.scrollHeight;
  }

  /**
   * 로딩 오버레이 토글
   */
  function setLoading(overlayEl, textEl, active, text = '처리 중입니다...') {
    if (textEl) textEl.textContent = text;
    if (overlayEl) overlayEl.classList.toggle('active', active);
  }

  return { startStream, logLine, logAgent, appendResult, setLoading };
})();
