/**
 * chat-common.js
 * SSE 채팅 공통 유틸. 모든 채팅 페이지에서 공유합니다.
 *
 * 로딩 UX:
 *   - 질문 전송 시: 오버레이 표시 + 답변 패널에 스켈레톤 바 표시
 *   - 첫 TOKEN 수신 시: 오버레이 즉시 숨김 + 스켈레톤 숨김 + 답변 표시 시작
 *   - complete / error 시: 오버레이 완전 숨김
 */
window.ChatCommon = (() => {

  /**
   * SSE 채팅 스트림 시작
   * @param {Object} opts
   *   url          - EventSource URL
   *   onToken      - 'token' 이벤트 콜백 (data: string)
   *   onFirstToken - 첫 번째 토큰 수신 시 1회 콜백 (로딩 숨김용)
   *   onAgent      - 'agent' 이벤트 콜백
   *   onComplete   - 'complete' 콜백
   *   onError      - 'error' 콜백
   */
  function startStream({ url, onToken, onFirstToken, onAgent, onComplete, onError }) {
    const es = new EventSource(url);
    let firstToken = true;

    es.addEventListener('token', e => {
      // 첫 토큰 수신 → onFirstToken 1회 호출
      if (firstToken) {
        firstToken = false;
        onFirstToken?.();
      }
      onToken?.(e.data);
    });

    es.addEventListener('agent', e => {
      try { onAgent?.(JSON.parse(e.data)); }
      catch { onAgent?.({ agent: '?', status: 'raw', message: e.data }); }
    });

    es.addEventListener('complete', () => { onComplete?.(); es.close(); });
    es.addEventListener('error',    e  => { onError?.(e);    es.close(); });

    return es;
  }

  /** 이벤트 로그 패널에 한 줄 추가 */
  function logLine(el, text) {
    if (!el) return;
    el.textContent += text + '\n';
    el.scrollTop = el.scrollHeight;
  }

  /** agent 이벤트를 로그 패널에 출력 */
  function logAgent(el, { agent, status, message }) {
    logLine(el, `[${agent}] ${status} — ${message}`);
  }

  /**
   * 응답 패널에 토큰 누적 표시
   * 스켈레톤이 있으면 첫 토큰에 자동 제거
   */
  function appendToken(el, token, state) {
    if (!el) return;
    // 스켈레톤 제거 (첫 토큰 시)
    const sk = el.querySelector?.('.result-skeleton');
    if (sk) sk.remove();
    if (state) state.text = (state.text || '') + token;
    el.textContent = state ? state.text : (el.textContent + token);
    el.scrollTop = el.scrollHeight;
  }

  /** 응답 패널 텍스트 교체 */
  function appendResult(el, text) {
    if (!el) return;
    const sk = el.querySelector?.('.result-skeleton');
    if (sk) sk.remove();
    el.textContent = text;
    el.scrollTop = el.scrollHeight;
  }

  /** 로딩 오버레이 토글 */
  function setLoading(overlayEl, textEl, active, text = '처리 중입니다...') {
    if (textEl) textEl.textContent = text;
    if (overlayEl) overlayEl.classList.toggle('active', active);
  }

  /**
   * 답변 패널 안에 스켈레톤 로딩 바 삽입
   * 첫 토큰이 오면 appendToken에서 자동 제거됨
   */
  function showResultSkeleton(resultEl) {
    if (!resultEl) return;
    // 기존 스켈레톤 제거
    resultEl.querySelectorAll?.('.result-skeleton').forEach(el => el.remove());
    const sk = document.createElement('div');
    sk.className = 'result-skeleton';
    sk.innerHTML = `
      <div class="sk-bar" style="width:80%"></div>
      <div class="sk-bar" style="width:65%"></div>
      <div class="sk-bar" style="width:72%"></div>
      <div class="sk-bar" style="width:55%;margin-top:12px"></div>
      <div class="sk-bar" style="width:68%"></div>
    `;
    resultEl.appendChild(sk);
  }

  /** 답변 패널 스켈레톤 제거 */
  function hideResultSkeleton(resultEl) {
    resultEl?.querySelectorAll?.('.result-skeleton').forEach(el => el.remove());
  }

  return {
    startStream,
    logLine, logAgent,
    appendToken, appendResult,
    setLoading,
    showResultSkeleton, hideResultSkeleton,
  };
})();
