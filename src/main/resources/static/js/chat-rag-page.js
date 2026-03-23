(() => {
  const { qs, val, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;
  const { startStream, logLine, appendToken, showResultSkeleton, hideResultSkeleton } = window.ChatCommon;
  let es = null;

  const resultEl = () => qs('result');
  const logEl    = () => qs('eventLog');

  // 토큰 누적 상태
  const tokenState = { text: '' };

  function clearView() {
    tokenState.text = '';
    if (resultEl()) resultEl().textContent = '';
    if (logEl())    logEl().textContent    = '';
    setText('ragHitInfo', '');
    if (es) { es.close(); es = null; }
  }

  function setLoading(active, text = 'RAG 검색 및 응답 생성 중...') {
    const ov = qs('loadingOverlay');
    const tx = qs('loadingText');
    if (tx) tx.textContent = text;
    ov?.classList.toggle('active', active);
    const btn = qs('btnSend');
    if (btn) btn.classList.toggle('btn-loading', active);
  }

  function onFirstToken() {
    setLoading(false);
    hideResultSkeleton(resultEl());
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
    setText('ragHitInfo', '');

    let url = `/api/chat/stream?message=${encodeURIComponent(message)}&category=${encodeURIComponent(category)}`;
    const src = val('sourceFilter');
    const ver = val('versionFilter');
    if (src)      url += `&source=${encodeURIComponent(src)}`;
    if (ver)      url += `&version=${encodeURIComponent(ver)}`;
    if (promptId) url += `&promptId=${encodeURIComponent(promptId)}`;

    logLine(logEl(), `[request] ${message}`);
    logLine(logEl(), `[category] ${category} | source=${src||'all'} | version=${ver||'all'} | prompt:${promptId||'기본'}`);

    setLoading(true);
    showResultSkeleton(resultEl());

    es = startStream({
      url,
      onFirstToken,
      onToken: token => appendToken(resultEl(), token, tokenState),
      onAgent: ({ agent, status, message: msg }) => {
        logLine(logEl(), `[agent:${agent}] ${status} — ${msg}`);
        if (agent.startsWith('RAG') || (msg && msg.includes('rag='))) {
          setText('ragHitInfo', `[${agent}] ${msg}`);
        }
      },
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

  async function ragPreview() {
    const category = val('category');
    const query    = val('message');
    const source   = val('sourceFilter');
    const version  = val('versionFilter');
    if (!query) { logLine(logEl(), '[error] 질문을 먼저 입력하세요.'); return; }

    try {
      let url = `/debug/api/rag/search?category=${encodeURIComponent(category)}&query=${encodeURIComponent(query)}`;
      if (source)  url += `&source=${encodeURIComponent(source)}`;
      if (version) url += `&version=${encodeURIComponent(version)}`;
      const data = await fetchJson(url);

      const card  = qs('ragPreviewCard');
      const panel = qs('ragPreviewPanel');
      if (card) card.style.display = '';

      if (!data?.documents?.length) {
        panel.textContent = `RAG 검색 결과 없음 (applied=${data?.applied}, reason=${data?.reason})`;
        return;
      }
      panel.innerHTML = data.documents.map((doc, i) => `
        <div style="border:1px solid #334155;border-radius:8px;padding:10px;margin-bottom:8px;background:#0b1220">
          <div style="font-size:12px;color:#94a3b8;margin-bottom:4px">
            #${i+1} | ${htmlEscape(doc.source)} / ${htmlEscape(doc.version)}
            ${doc.score != null ? ` | score: ${doc.score.toFixed(4)}` : ''}
          </div>
          <div style="white-space:pre-wrap;font-size:13px">${htmlEscape(doc.text || '')}</div>
        </div>`).join('');
    } catch (e) {
      logLine(logEl(), '[rag-preview] 오류: ' + e.message);
    }
  }

  async function memoryClear() {
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      logLine(logEl(), '[memory] 대화 초기화 완료');
      // 패널 갱신
      const panel = qs('memoryPanel');
      if (panel) panel.innerHTML = '<div class="mem-inline-empty">대화 메모리가 초기화되었습니다.</div>';
    } catch (e) {
      logLine(logEl(), '[memory] 초기화 실패: ' + e.message);
    }
  }

  async function loadStatus() {
    try {
      const s = await fetchJson('/debug/api/rag/status');
      setText('ragStatusChip', `rag: ${s?.enabled ? 'on' : 'off'}`);
    } catch { setText('ragStatusChip', 'rag: -'); }
    try {
      const d = await fetchJson('/debug/api/rag/db-info');
      setText('dbStatusChip', `db: ${d?.jdbc?.connected ? 'ok' : 'offline'} / ${d?.vectorDb?.rowCount ?? 0} chunks`);
    } catch { setText('dbStatusChip', 'db: -'); }
  }

  document.addEventListener('DOMContentLoaded', () => {
    qs('btnSend')?.addEventListener('click', send);
    qs('btnPreview')?.addEventListener('click', ragPreview);
    qs('btnClear')?.addEventListener('click', clearView);
    qs('btnMemoryClear')?.addEventListener('click', memoryClear);
    qs('message')?.addEventListener('keydown', e => {
      if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) send();
    });
    qs('category')?.addEventListener('change', () => {
      window.PromptSelector?.init('promptSelect', val('category') || null);
    });
    // 메모리 인라인 패널
    window.ChatMemoryPanel?.init();
    // 프롬프트 초기 로드 (DEV가 기본 선택)
    window.PromptSelector?.init('promptSelect', val('category') || 'DEV');
    loadStatus();
  });
})();
