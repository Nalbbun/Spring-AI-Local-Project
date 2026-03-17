/**
 * chat-common.js
 * SSE 채팅 공통 유틸. 모든 채팅 페이지에서 공유합니다.
 *
 * 서버 이벤트:
 *   token    — LLM 토큰 단위 스트리밍 (실시간 표시용)
 *   agent    — 에이전트 단계 이벤트 { agent, status, message }
 *   complete — 스트림 종료
 *   error    — 오류
 */
window.ChatCommon = (() => {

  /**
   * SSE 채팅 스트림 시작
   * @param {Object} opts
   *   url        - EventSource URL
   *   onToken    - 'token' 이벤트 콜백 (data: string) — 토큰 단위 스트리밍
   *   onAgent    - 'agent' 이벤트 콜백 (data: {agent,status,message})
   *   onComplete - 'complete' 콜백
   *   onError    - 'error'   콜백 (event)
   * @returns EventSource
   */
  function startStream({ url, onToken, onAgent, onComplete, onError }) {
    const es = new EventSource(url);

    // 토큰 단위 스트리밍 (실시간 표시)
    es.addEventListener('token', e => onToken?.(e.data));

    // 에이전트 단계 이벤트
    es.addEventListener('agent', e => {
      try { onAgent?.(JSON.parse(e.data)); }
      catch { onAgent?.({ agent: '?', status: 'raw', message: e.data }); }
    });

    // 스트림 종료
    es.addEventListener('complete', () => { onComplete?.(); es.close(); });

    // 오류
    es.addEventListener('error', e => { onError?.(e); es.close(); });

    return es;
  }

  /**
   * 이벤트 로그 패널에 한 줄 추가
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
   * 응답 패널에 토큰을 누적해서 표시 (스트리밍 방식)
   * @param {HTMLElement} el  - 응답 패널 DOM
   * @param {string}      token - 새로 받은 토큰
   * @param {Object}      state - { text: '' } 형태의 외부 상태 객체
   */
  function appendToken(el, token, state) {
    if (!el) return;
    if (state) state.text = (state.text || '') + token;
    el.textContent = state ? state.text : (el.textContent + token);
    el.scrollTop = el.scrollHeight;
  }

  /**
   * 응답 패널 텍스트를 한 번에 교체 (비스트리밍 폴백용)
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

  return { startStream, logLine, logAgent, appendToken, appendResult, setLoading };
})();
