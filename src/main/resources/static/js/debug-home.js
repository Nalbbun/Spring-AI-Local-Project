(() => {
  const { qs, val, rawVal, setVal, setText, fetchJson, parseMetadata, ensure, renderJson, htmlEscape } = window.UiCommon;
  let eventSource = null;
  let loadedSources = [];
  let loadedSourceFiles = [];

  const currentCategory = () => qs('category').value || 'DEV';
  const currentSource = () => val('sourceKey') || val('requestSourceFilter');
  const currentVersion = () => val('sourceVersion') || val('requestVersionFilter');
  const uploadSource = () => val('ingestSource') || currentSource();
  const uploadVersion = () => val('ingestVersion') || currentVersion() || 'v1';
  const uploadTitle = () => val('ingestTitle') || uploadSource() || 'manual-ingest';
  const asBool = (v) => String(v) === 'true';

  function applyExample() { if (qs('example').value) qs('message').value = qs('example').value; }

  function renderRuntime(config) {
    setText('statusConversationId', `conversation: ${config.conversationId || '-'}`);
    setText('statusStoreType', `store: ${config.memoryStore || '-'} / ${config.memoryServiceType || '-'}`);
    setText('statusFallbackPolicy', `fallback: ${config.fallbackPolicy || '-'}`);
    setText('statusRagEnabled', `rag: ${config.ragEnabled ? 'on' : 'off'}`);
    setText('statusRagFilter', `default filter: ${config.ragDefaultSourceFilter || '-'} / ${config.ragDefaultVersionFilter || '-'}`);
  }

  function updateActionButtons() {
    const hasSource = !!currentSource();
    ['purgeVectorBtn','purgeRegistryBtn','reindexSameBtn','reindexCopyBtn','compareBtn','btnLoadSourceFiles']
      .forEach(id => { const el = qs(id); if (el) el.disabled = !hasSource; });
    qs('btnIngestFiles').disabled = !(uploadSource() && uploadVersion() && qs('ingestFiles').files.length > 0);
    setText('sourceHint', hasSource ? `선택 source: ${currentSource()} / version: ${currentVersion() || '-'}` : 'source 목록에서 항목을 선택하거나 Source를 직접 입력하세요.');
  }

  function renderSourcesTable(items) {
    const panel = qs('sourceTablePanel');
    if (!items || items.length === 0) {
      panel.innerHTML = '<div class="panel" style="min-height:120px;max-height:260px">조회된 source가 없습니다.</div>';
      return;
    }
    panel.innerHTML = `
      <table class="table"><thead><tr>
        <th>선택</th><th>Source</th><th>Version</th><th>Title</th><th>Files</th><th>Chunks</th><th>Storage</th><th>Indexed</th>
      </tr></thead><tbody>
      ${items.map((item, index) => `
        <tr class="${currentSource() === item.source && currentVersion() === item.version ? 'selected' : ''}">
          <td><button type="button" data-select-source="${index}" class="gray">선택</button></td>
          <td>${htmlEscape(item.source)}</td>
          <td>${htmlEscape(item.version)}</td>
          <td>${htmlEscape(item.title || '-')}</td>
          <td>${item.fileCount ?? 0}</td>
          <td>${item.chunkCount ?? 0}</td>
          <td>${htmlEscape(item.storageKind || '-')}</td>
          <td>${htmlEscape(item.lastIndexedAt || item.ingestedAt || '-')}</td>
        </tr>`).join('')}
      </tbody></table>`;
    panel.querySelectorAll('[data-select-source]').forEach(btn => btn.addEventListener('click', () => selectSourceByIndex(Number(btn.dataset.selectSource))));
  }

  function renderSourceFilesTable(items) {
    const panel = qs('sourceFilesTablePanel');
    if (!items || items.length === 0) {
      panel.innerHTML = '<div class="panel" style="min-height:120px;max-height:260px">등록된 파일이 없습니다.</div>';
      return;
    }
    panel.innerHTML = `
      <table class="table"><thead><tr>
        <th>fileId</th><th>파일명</th><th>제목</th><th>타입</th><th>Storage</th><th>Chunks</th><th>Indexed</th><th>Action</th>
      </tr></thead><tbody>
      ${items.map(item => `
        <tr>
          <td>${htmlEscape(item.fileId)}</td>
          <td>${htmlEscape(item.originalFileName || item.fileName || '-')}</td>
          <td>${htmlEscape(item.title || '-')}</td>
          <td>${htmlEscape(item.contentType || '-')}</td>
          <td>${htmlEscape(item.storageKind || '-')}</td>
          <td>${item.chunkCount ?? 0}</td>
          <td>${htmlEscape(item.lastIndexedAt || item.ingestedAt || '-')}</td>
          <td><button type="button" data-file-purge="${htmlEscape(item.fileId)}" class="red">삭제</button></td>
        </tr>`).join('')}
      </tbody></table>`;
    panel.querySelectorAll('[data-file-purge]').forEach(btn => btn.addEventListener('click', () => purgeSourceFile(btn.dataset.filePurge)));
  }

  function renderDbInfo(data) {
    setText('statusDb', `db: ${data?.jdbc?.connected ? 'connected' : 'offline'}`);
    const stats = [
      { label:'Vector Rows', value:data?.vectorDb?.rowCount ?? 0 },
      { label:'Distinct Sources', value:data?.vectorDb?.distinctSources ?? '-' },
      { label:'Registry Files', value:data?.registry?.totalFiles ?? 0 }
    ];
    qs('dbInfoStats').innerHTML = stats.map(stat => `<div class="stat-card"><span class="muted">${stat.label}</span><strong>${htmlEscape(stat.value)}</strong></div>`).join('');
    renderJson('dbInfoPanel', data);
  }

  function applyManifestSelection(manifest) {
    if (!manifest) return;
    setVal('sourceKey', manifest.source || '');
    setVal('sourceVersion', manifest.version || '');
    setVal('ingestSource', manifest.source || '');
    setVal('ingestVersion', manifest.version || '');
    setVal('ingestTitle', manifest.title || '');
    updateActionButtons();
  }

  function selectSourceByIndex(index) {
    const item = loadedSources[index];
    if (!item) return;
    applyManifestSelection(item);
    renderSourcesTable(loadedSources);
    loadSourceFiles();
  }

  async function loadDebugConfig() {
    try {
      const data = await fetchJson('/debug/api/config');
      qs('resolverMode').value = data.resolverMode;
      qs('generalParserMode').value = data.generalParserMode;
      qs('travelParserMode').value = data.travelParserMode;
      qs('devParserMode').value = data.devParserMode;
      qs('miceParserMode').value = data.miceParserMode;
      qs('ragEnabled').value = String(data.ragEnabled);
      qs('ragTopK').value = data.ragTopK;
      qs('ragSimilarityThreshold').value = data.ragSimilarityThreshold;
      qs('ragIncludeCitations').value = String(data.ragIncludeCitations);
      qs('ragGeneralEnabled').value = String(data.ragGeneralEnabled);
      qs('ragDevEnabled').value = String(data.ragDevEnabled);
      qs('ragMiceEnabled').value = String(data.ragMiceEnabled);
      qs('ragTravelEnabled').value = String(data.ragTravelEnabled);
      qs('ragDefaultSourceFilter').value = data.ragDefaultSourceFilter || '';
      qs('ragDefaultVersionFilter').value = data.ragDefaultVersionFilter || '';
      qs('ragDatasetLocation').value = data.ragDatasetLocation || '';
      renderRuntime(data);
      renderJson('configStatus', data);
    } catch (e) {
      setText('configStatus', '설정 조회 실패: ' + e.message);
    }
  }

  async function saveDebugConfig() {
    const payload = {
      resolverMode: val('resolverMode'), generalParserMode: val('generalParserMode'), travelParserMode: val('travelParserMode'),
      devParserMode: val('devParserMode'), miceParserMode: val('miceParserMode'), ragEnabled: asBool(val('ragEnabled')),
      ragTopK: Number(val('ragTopK') || 0), ragSimilarityThreshold: Number(val('ragSimilarityThreshold') || 0),
      ragIncludeCitations: asBool(val('ragIncludeCitations')), ragGeneralEnabled: asBool(val('ragGeneralEnabled')),
      ragDevEnabled: asBool(val('ragDevEnabled')), ragMiceEnabled: asBool(val('ragMiceEnabled')), ragTravelEnabled: asBool(val('ragTravelEnabled')),
      ragDefaultSourceFilter: val('ragDefaultSourceFilter'), ragDefaultVersionFilter: val('ragDefaultVersionFilter'), ragDatasetLocation: val('ragDatasetLocation')
    };
    try {
      const data = await fetchJson('/debug/api/config', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderRuntime(data); renderJson('configStatus', data); await loadDbInfo();
    } catch (e) { setText('configStatus', '설정 저장 실패: ' + e.message); }
  }

  async function resetDebugConfig() {
    try {
      const data = await fetchJson('/debug/api/config/reset', { method:'POST' });
      renderRuntime(data); renderJson('configStatus', data); await loadDebugConfig(); await loadDbInfo();
    } catch (e) { setText('configStatus', '설정 초기화 실패: ' + e.message); }
  }

  function fillModelSelect(id, models, selected) {
    const select = qs(id); select.innerHTML = '';
    const empty = document.createElement('option'); empty.value=''; empty.textContent='(선택 안 함)'; select.appendChild(empty);
    models.forEach(model => {
      const option = document.createElement('option'); option.value = model.name; option.textContent = model.name;
      if (selected && selected === model.name) option.selected = true;
      select.appendChild(option);
    });
  }

  async function loadOllamaModels() {
    const source = val('ollamaModelSource');
    const models = await fetchJson(`/debug/api/ollama/models?source=${encodeURIComponent(source)}`);
    fillModelSelect('generalModel', models, val('generalModel'));
    fillModelSelect('devModel', models, val('devModel'));
    fillModelSelect('miceModel', models, val('miceModel'));
    fillModelSelect('travelSearchModel', models, val('travelSearchModel'));
    fillModelSelect('travelPlanModel', models, val('travelPlanModel'));
    setText('ollamaModelStatus', `모델 목록 로드 완료 (${source}) / ${models.length}개`);
  }

  async function loadOllamaModelConfig() {
    try {
      const config = await fetchJson('/debug/api/ollama/config');
      qs('ollamaModelSource').value = config.modelSource || 'RUNNING';
      await loadOllamaModels();
      qs('generalModel').value = config.generalModel || '';
      qs('devModel').value = config.devModel || '';
      qs('miceModel').value = config.miceModel || '';
      qs('travelSearchModel').value = config.travelSearchModel || '';
      qs('travelPlanModel').value = config.travelPlanModel || '';
      renderJson('ollamaModelStatus', config);
    } catch (e) {
      setText('ollamaModelStatus', '모델 설정 조회 실패: ' + e.message);
    }
  }

  async function saveOllamaModelConfig() {
    const payload = {
      modelSource: val('ollamaModelSource'), generalModel: val('generalModel'), devModel: val('devModel'), miceModel: val('miceModel'),
      travelSearchModel: val('travelSearchModel'), travelPlanModel: val('travelPlanModel')
    };
    try { renderJson('ollamaModelStatus', await fetchJson('/debug/api/ollama/config', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) })); }
    catch (e) { setText('ollamaModelStatus', '모델 설정 저장 실패: ' + e.message); }
  }

  async function resetOllamaModelConfig() {
    try { await fetchJson('/debug/api/ollama/config/reset', { method:'POST' }); await loadOllamaModelConfig(); }
    catch (e) { setText('ollamaModelStatus', '모델 설정 초기화 실패: ' + e.message); }
  }

  function buildChatUrl() {
    let url = `/api/chat/stream?message=${encodeURIComponent(val('message'))}`;
    if (currentCategory()) url += `&category=${encodeURIComponent(currentCategory())}`;
    if (val('requestSourceFilter')) url += `&source=${encodeURIComponent(val('requestSourceFilter'))}`;
    if (val('requestVersionFilter')) url += `&version=${encodeURIComponent(val('requestVersionFilter'))}`;
    return url;
  }

  function startStream() {
    if (eventSource) { eventSource.close(); eventSource = null; }
    setText('streamLog', '');
    if (!val('message')) { setText('streamLog', '메시지를 입력하세요.'); return; }
    const url = buildChatUrl();
    const append = (line) => { qs('streamLog').textContent += line; qs('streamLog').scrollTop = qs('streamLog').scrollHeight; };
    append('[request] ' + url + '\n');
    eventSource = new EventSource(url);
    eventSource.addEventListener('agent', e => append('[agent] ' + e.data + '\n'));
    eventSource.addEventListener('message', e => append('[message] ' + e.data + '\n'));
    eventSource.addEventListener('complete', () => { append('[complete]\n'); if (eventSource) { eventSource.close(); eventSource = null; } });
    eventSource.addEventListener('error', () => { append('[error] 종료\n'); if (eventSource) { eventSource.close(); eventSource = null; } });
  }

  async function previewRagSearch() {
    try {
      let url = `/debug/api/rag/search?category=${encodeURIComponent(currentCategory())}&query=${encodeURIComponent(val('message'))}`;
      if (val('requestSourceFilter')) url += `&source=${encodeURIComponent(val('requestSourceFilter'))}`;
      if (val('requestVersionFilter')) url += `&version=${encodeURIComponent(val('requestVersionFilter'))}`;
      renderJson('resultPanel', await fetchJson(url));
    } catch (e) {
      setText('resultPanel', 'RAG 검색 실패: ' + e.message);
    }
  }

  async function runEval() {
    try { renderJson('memoryPanel', await fetchJson('/debug/api/rag/eval/run-default')); }
    catch (e) { setText('memoryPanel', '평가셋 실행 실패: ' + e.message); }
  }
  async function loadMemory() {
    try { renderJson('memoryPanel', await fetchJson('/debug/api/memory')); }
    catch (e) { setText('memoryPanel', '메모리 조회 실패: ' + e.message); }
  }
  async function clearMemory() {
    try {
      const res = await fetch('/debug/api/memory/clear', { method:'POST' });
      const text = await res.text();
      if (!res.ok) throw new Error(text);
      setText('memoryPanel', text);
    } catch (e) { setText('memoryPanel', '메모리 초기화 실패: ' + e.message); }
  }

  async function loadDbInfo() {
    try { renderDbInfo(await fetchJson('/debug/api/rag/db-info')); }
    catch (e) { setText('dbInfoPanel', 'DB 정보 조회 실패: ' + e.message); }
  }

  async function loadSources() {
    try {
      let url = `/debug/api/rag/sources?category=${encodeURIComponent(currentCategory())}`;
      if (currentSource()) url += `&source=${encodeURIComponent(currentSource())}`;
      if (currentVersion()) url += `&version=${encodeURIComponent(currentVersion())}`;
      loadedSources = await fetchJson(url);
      renderSourcesTable(loadedSources);
      renderJson('resultPanel', loadedSources);
      if (loadedSources.length === 1) selectSourceByIndex(0);
      updateActionButtons();
    } catch (e) { setText('resultPanel', 'source 목록 조회 실패: ' + e.message); }
  }

  async function loadSourceFiles() {
    try {
      const source = ensure(currentSource(), 'source를 먼저 선택하세요.');
      const version = ensure(currentVersion(), 'version을 입력하세요.');
      loadedSourceFiles = await fetchJson(`/debug/api/rag/source/files?category=${encodeURIComponent(currentCategory())}&source=${encodeURIComponent(source)}&version=${encodeURIComponent(version)}`);
      renderSourceFilesTable(loadedSourceFiles);
    } catch (e) { setText('sourceFilesTablePanel', '파일 목록 조회 실패: ' + e.message); }
  }

  async function purgeSource(deleteRegistry) {
    try {
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: currentVersion(), deleteRegistry };
      renderJson('resultPanel', await fetchJson('/debug/api/rag/source/purge', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) }));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', 'purge 실패: ' + e.message); }
  }

  async function purgeSourceFile(fileId) {
    try {
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: currentVersion(), fileId, deleteRegistry: true };
      renderJson('resultPanel', await fetchJson('/debug/api/rag/source/file/purge', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) }));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', '파일 삭제 실패: ' + e.message); }
  }

  async function reindexSource(copyToNewVersion) {
    try {
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: currentVersion(), targetVersion: val('targetVersion'), purgeBeforeReindex: true, copyToNewVersion };
      renderJson('resultPanel', await fetchJson('/debug/api/rag/source/reindex', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) }));
      if (copyToNewVersion && val('targetVersion')) setVal('sourceVersion', val('targetVersion'));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', 'reindex 실패: ' + e.message); }
  }

  async function compareVersions() {
    try {
      const source = ensure(currentSource(), 'source는 필수입니다.');
      const left = ensure(currentVersion(), 'version은 필수입니다.');
      const right = ensure(val('targetVersion'), 'target version은 필수입니다.');
      let url = `/debug/api/rag/source/compare?category=${encodeURIComponent(currentCategory())}&source=${encodeURIComponent(source)}&leftVersion=${encodeURIComponent(left)}&rightVersion=${encodeURIComponent(right)}`;
      if (val('compareQuery')) url += `&query=${encodeURIComponent(val('compareQuery'))}`;
      renderJson('resultPanel', await fetchJson(url));
    } catch (e) { setText('resultPanel', 'compare 실패: ' + e.message); }
  }

  async function ingestTextSource() {
    try {
      const payload = { category: currentCategory(), source: uploadSource(), version: uploadVersion(), title: uploadTitle(), text: val('ingestTextBody'), metadata: parseMetadata(rawVal('ingestMetadata')) };
      ensure(payload.text, '적재할 텍스트를 입력하세요.');
      const data = await fetchJson('/debug/api/rag/ingest-text', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderJson('ingestPanel', data); renderJson('debugOutputPanel', data);
      applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles);
      await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', 'Text 적재 실패: ' + e.message); }
  }

  async function ingestFilesSource() {
    try {
      ensure(uploadSource(), '멀티파일 업로드에서는 source가 필수입니다.');
      ensure(uploadVersion(), 'version은 필수입니다.');
      const files = qs('ingestFiles').files; ensure(files.length > 0, '업로드할 파일을 선택하세요.');
      const form = new FormData();
      form.append('category', currentCategory()); form.append('source', uploadSource()); form.append('version', uploadVersion()); form.append('title', uploadTitle());
      const metadata = parseMetadata(rawVal('ingestMetadata')); if (Object.keys(metadata).length > 0) form.append('metadataJson', JSON.stringify(metadata));
      Array.from(files).forEach(file => form.append('files', file));
      const data = await fetchJson('/debug/api/rag/ingest-files', { method:'POST', body: form });
      renderJson('ingestPanel', data); renderJson('debugOutputPanel', data);
      applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles);
      await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', '멀티파일 업로드 실패: ' + e.message); }
  }

  async function ingestUrlSource() {
    try {
      const payload = { category: currentCategory(), url: val('ingestUrl'), source: uploadSource(), version: uploadVersion(), title: uploadTitle(), metadata: parseMetadata(rawVal('ingestMetadata')) };
      ensure(payload.url, '적재할 URL을 입력하세요.');
      const data = await fetchJson('/debug/api/rag/ingest-url', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderJson('ingestPanel', data); renderJson('debugOutputPanel', data);
      applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles);
      await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', 'URL 적재 실패: ' + e.message); }
  }

  document.addEventListener('DOMContentLoaded', async () => {
    qs('example').addEventListener('change', applyExample);
    qs('ollamaModelSource').addEventListener('change', loadOllamaModels);
    ['sourceKey','sourceVersion','requestSourceFilter','requestVersionFilter','targetVersion','ingestSource','ingestVersion'].forEach(id => qs(id).addEventListener('input', updateActionButtons));
    qs('ingestFiles').addEventListener('change', updateActionButtons);
    qs('btnSaveDebugConfig').addEventListener('click', saveDebugConfig);
    qs('btnLoadDebugConfig').addEventListener('click', loadDebugConfig);
    qs('btnResetDebugConfig').addEventListener('click', resetDebugConfig);
    qs('btnSaveOllamaConfig').addEventListener('click', saveOllamaModelConfig);
    qs('btnLoadOllamaConfig').addEventListener('click', loadOllamaModelConfig);
    qs('btnResetOllamaConfig').addEventListener('click', resetOllamaModelConfig);
    qs('btnStream').addEventListener('click', startStream);
    qs('btnPreviewSearch').addEventListener('click', previewRagSearch);
    qs('btnRunEval').addEventListener('click', runEval);
    qs('btnLoadMemory').addEventListener('click', loadMemory);
    qs('btnClearMemory').addEventListener('click', clearMemory);
    qs('btnLoadSources').addEventListener('click', loadSources);
    qs('purgeVectorBtn').addEventListener('click', () => purgeSource(false));
    qs('purgeRegistryBtn').addEventListener('click', () => purgeSource(true));
    qs('reindexSameBtn').addEventListener('click', () => reindexSource(false));
    qs('reindexCopyBtn').addEventListener('click', () => reindexSource(true));
    qs('compareBtn').addEventListener('click', compareVersions);
    qs('btnIngestText').addEventListener('click', ingestTextSource);
    qs('btnIngestFiles').addEventListener('click', ingestFilesSource);
    qs('btnIngestUrl').addEventListener('click', ingestUrlSource);
    qs('btnLoadSourceFiles').addEventListener('click', loadSourceFiles);
    qs('btnLoadDbInfo').addEventListener('click', loadDbInfo);
    updateActionButtons();
    await loadDebugConfig();
    await loadOllamaModelConfig();
    await loadDbInfo();
  });
})();
