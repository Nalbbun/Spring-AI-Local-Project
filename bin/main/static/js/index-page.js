(() => {
  const { qs, val, setVal, setText, fetchJson, htmlEscape } = window.UiCommon;
  let eventSource = null;

  function setLoading(active, text = '처리 중입니다...') {
    setText('loadingText', text);
    qs('loadingOverlay').classList.toggle('active', active);
  }
  function logLine(text) {
    const log = qs('log');
    log.textContent += text + '\n';
    log.scrollTop = log.scrollHeight;
  }
  function clearView() {
    setText('result', '');
    setText('log', '');
    if (eventSource) { eventSource.close(); eventSource = null; }
  }
  function currentPayload() {
    return { category: val('ragCategory') || 'DEV', source: val('ragSource'), version: val('ragVersion') || 'v1', title: val('ragTitle') };
  }
  function applyResultIdentity(result) {
    if (!result) return;
    if (result.source) setVal('ragSource', result.source);
    if (result.version) setVal('ragVersion', result.version);
    if (result.title) setVal('ragTitle', result.title);
  }
  function renderUploadRows(items) {
    const body = qs('uploadResultTable');
    if (!items || items.length === 0) {
      body.innerHTML = '<tr><td colspan="7">아직 결과가 없습니다.</td></tr>';
      return;
    }
    body.innerHTML = items.map(item => `
      <tr>
        <td>${htmlEscape(item.fileName || '-')}</td>
        <td>${htmlEscape(item.source || '-')}</td>
        <td>${htmlEscape(item.version || '-')}</td>
        <td>${htmlEscape(item.title || '-')}</td>
        <td>${item.chunkCount ?? 0}</td>
        <td>${item.stored ? 'SUCCESS' : 'FAIL'}</td>
        <td>${htmlEscape(item.message || '-')}</td>
      </tr>`).join('');
  }
  async function loadQuickSources() {
    try {
      const items = await fetchJson('/debug/api/rag/sources?category=' + encodeURIComponent(val('ragCategory') || 'DEV'));
      const select = qs('quickSourcePick');
      select.innerHTML = '<option value="">선택 안 함</option>' + items.map(item => `<option value="${htmlEscape(item.source)}|${htmlEscape(item.version)}|${htmlEscape(item.title || '')}">${htmlEscape(item.source)} / ${htmlEscape(item.version)}</option>`).join('');
      setText('indexRagSummary', `rag: ${items.length} source`);
    } catch (e) {
      setText('indexRagSummary', 'rag: load error');
    }
  }
  async function loadOllamaInfo() {
    try {
      const data = await fetchJson('/api/runtime/ollama');
      setText('indexOllamaInfo', `baseUrl=${data?.baseUrl || '-'}\nstatus=${data?.status || '-'}\nreachable=${data?.reachable}\nrunning=${data?.runningCount ?? 0}\ninstalled=${data?.installedCount ?? 0}\nmessage=${data?.message || '-'}`);
    } catch (e) {
      setText('indexOllamaInfo', '조회 실패\n' + e.message);
    }
  }

  async function loadDbSummary() {
    try {
      const data = await fetchJson('/debug/api/rag/db-info');
      setText('indexDbSummary', `db: ${data?.jdbc?.connected ? 'connected' : 'offline'} / vector ${data?.vectorDb?.rowCount ?? 0}`);
    } catch (e) {
      setText('indexDbSummary', 'db: error');
    }
  }
  function applyQuickSource() {
    const raw = val('quickSourcePick');
    if (!raw) return;
    const [source, version, title] = raw.split('|');
    setVal('ragSource', source || '');
    setVal('ragVersion', version || 'v1');
    setVal('ragTitle', title || '');
  }
  function startStream() {
    if (eventSource) { eventSource.close(); eventSource = null; }
    setText('result', '');
    setText('log', '');
    const message = val('message');
    const category = val('category');
    if (!message) { logLine('[error] 메시지를 입력하세요.'); return; }
    let url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
    if (category) url += `&category=${encodeURIComponent(category)}`;
    logLine(`[request] ${message}`);
    logLine(`[category] ${category || 'AUTO'}`);
    eventSource = new EventSource(url);
    eventSource.addEventListener('agent', (event) => logLine(`[agent] ${event.data}`));
    eventSource.addEventListener('message', (event) => { setText('result', event.data); logLine('[message] 응답 수신 완료'); });
    eventSource.addEventListener('complete', () => { logLine('[complete] 스트림 종료'); if (eventSource) { eventSource.close(); eventSource = null; } });
    eventSource.addEventListener('error', () => { logLine('[error] 스트림 오류 또는 종료'); if (eventSource) { eventSource.close(); eventSource = null; } });
  }
  async function uploadSingleFile() {
    const file = qs('ragFile').files[0];
    if (!file) { setText('uploadStatus', '업로드 실패\n파일을 먼저 선택하세요.'); return; }
    const meta = currentPayload();
    const form = new FormData();
    form.append('category', meta.category); form.append('source', meta.source); form.append('version', meta.version); form.append('title', meta.title); form.append('file', file);
    try {
      setLoading(true, '단일 파일 업로드 중...'); setText('uploadStatus', '단일 파일 업로드를 처리 중입니다...');
      const data = await fetchJson('/debug/api/rag/ingest-file', { method: 'POST', body: form });
      applyResultIdentity(data.result);
      renderUploadRows([{ fileName: file.name, source: data.result.source, version: data.result.version, title: data.result.title, chunkCount: data.result.chunkCount, stored: data.result.stored, message: 'stored' }]);
      setText('uploadStatus', `업로드 완료\nsource=${data.result.source}\nversion=${data.result.version}\nchunks=${data.result.chunkCount}`);
      await loadQuickSources(); await loadDbSummary();
    } catch (e) {
      renderUploadRows([{ fileName: file.name, source: '-', version: '-', title: '-', chunkCount: 0, stored: false, message: e.message }]);
      setText('uploadStatus', '업로드 실패\n' + e.message);
    } finally { setLoading(false); }
  }
  async function uploadMultiFiles() {
    const files = Array.from(qs('ragFiles').files || []);
    if (files.length === 0) { setText('uploadStatus', '멀티파일 업로드 실패\n파일을 먼저 선택하세요.'); return; }
    const meta = currentPayload();
    const form = new FormData();
    form.append('category', meta.category); form.append('source', meta.source); form.append('version', meta.version); form.append('title', meta.title); files.forEach(file => form.append('files', file));
    try {
      setLoading(true, `멀티파일 업로드 중... (${files.length}개)`); setText('uploadStatus', `멀티파일 업로드를 처리 중입니다...\n선택 파일 수=${files.length}`);
      const data = await fetchJson('/debug/api/rag/ingest-files', { method: 'POST', body: form });
      applyResultIdentity(data.result); renderUploadRows(data.result.files || []);
      setText('uploadStatus', `멀티파일 업로드 완료\nsource=${data.result.source}\nversion=${data.result.version}\n성공=${data.result.successCount}, 실패=${data.result.failCount}, chunks=${data.result.totalChunkCount}`);
      await loadQuickSources(); await loadDbSummary();
    } catch (e) {
      setText('uploadStatus', '멀티파일 업로드 실패\n' + e.message);
    } finally { setLoading(false); }
  }
  async function ingestText() {
    const payload = { ...currentPayload(), text: val('ragText') };
    if (!payload.text) { setText('uploadStatus', '텍스트 적재 실패\n텍스트를 입력하세요.'); return; }
    try {
      setLoading(true, '텍스트 적재 중...');
      const data = await fetchJson('/debug/api/rag/ingest-text', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      applyResultIdentity(data.result); renderUploadRows([{ fileName: 'manual-text', source: data.result.source, version: data.result.version, title: data.result.title, chunkCount: data.result.chunkCount, stored: data.result.stored, message: 'stored' }]);
      setText('uploadStatus', `텍스트 적재 완료\nsource=${data.result.source}\nversion=${data.result.version}\nchunks=${data.result.chunkCount}`);
      await loadQuickSources(); await loadDbSummary();
    } catch (e) {
      setText('uploadStatus', '텍스트 적재 실패\n' + e.message);
    } finally { setLoading(false); }
  }
  document.addEventListener('DOMContentLoaded', async () => {
    qs('btnStream').addEventListener('click', startStream);
    qs('btnClearView').addEventListener('click', clearView);
    qs('btnUploadSingle').addEventListener('click', uploadSingleFile);
    qs('btnUploadMulti').addEventListener('click', uploadMultiFiles);
    qs('btnIngestText').addEventListener('click', ingestText);
    qs('quickSourcePick').addEventListener('change', applyQuickSource);
    qs('ragCategory').addEventListener('change', loadQuickSources);
    await loadQuickSources();
    await loadDbSummary();
    await loadOllamaInfo();
  });
})();
