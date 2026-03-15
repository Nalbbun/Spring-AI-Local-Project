(() => {
  const byId = (id) => document.getElementById(id);
  const val = (id) => (byId(id)?.value || '').trim();
  const setVal = (id, value) => { const el = byId(id); if (el) el.value = value ?? ''; };
  const setText = (id, text) => { const el = byId(id); if (el) el.textContent = text == null ? '' : String(text); };
  const htmlEscape = (v) => String(v ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;');
  const pretty = (v) => JSON.stringify(v, null, 2);
  let eventSource = null;
  let allOllamaModels = [];
  let loadedSources = [];
  let loadedSourceFiles = [];
  const agentStateMap = {};

  async function fetchJson(url, options = {}) {
    const res = await fetch(url, options);
    const text = await res.text();
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch (_) {}
    if (!res.ok) {
      const message = json?.message || json?.error || text || `HTTP ${res.status}`;
      const hint = json?.hint ? `
힌트: ${json.hint}` : '';
      throw new Error(message + hint);
    }
    return json;
  }

  function ensure(value, message) { if (!value) throw new Error(message); return value; }
  function parseMetadata(text) {
    const raw = (text || '').trim();
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error('JSON 객체 형식이어야 합니다.');
    return parsed;
  }
  function setLoading(active, text = '처리 중입니다...') {
    setText('loadingText', text);
    byId('loadingOverlay')?.classList.toggle('active', active);
  }
  function setStatusBox(id, text, type) {
    const el = byId(id);
    if (!el) return;
    el.className = `status-box${type ? ' ' + type : ''}`;
    el.textContent = text;
  }
  function renderJson(id, data) { setText(id, pretty(data)); }
  function currentCategory() { return val('category') || 'DEV'; }
  function currentSource() { return val('sourceKey') || val('requestSource'); }
  function currentVersion() { return val('sourceVersion') || val('requestVersion'); }
  function uploadSource() { return val('ingestSource') || currentSource(); }
  function uploadVersion() { return val('ingestVersion') || currentVersion() || 'v1'; }
  function uploadTitle() { return val('ingestTitle') || uploadSource() || 'manual-ingest'; }
  function currentQuickUploadPayload() {
    return { category: byId('ragCategory')?.value || 'DEV', source: val('ragSource'), version: val('ragVersion') || 'v1', title: val('ragTitle') };
  }
  function applyExample() { if (val('example')) setVal('message', val('example')); }
  function renderMemoryStoreInfo(data) {
    setText('memoryStoreBadge', `store: ${data.memoryStore || '-'}`);
    setText('memoryServiceBadge', `service: ${data.memoryServiceType || '-'}`);
    setText('statusConversationId', data.conversationId || '-');
    setText('statusStoreType', data.memoryStore || '-');
    setText('statusFallbackPolicy', data.fallbackPolicy || '-');
    setText('runtimeStoreStatus', `메모리 저장소
store=${data.memoryStore || '-'}
service=${data.memoryServiceType || '-'}
fallbackPolicy=${data.fallbackPolicy || '-'}
conversationId=${data.conversationId || '-'}
* 저장소 변경은 프로필/환경변수 변경 후 재시작이 필요합니다.`);
  }
  function renderServerSettings(data) {
    const profiles = Array.isArray(data.activeProfiles) && data.activeProfiles.length ? data.activeProfiles.join(', ') : '-';
    setText('serverSettingsPanel', `application=${data.applicationName || '-'}
profiles=${profiles}
debugEnabled=${data.debugEnabled}
serverPort=${data.serverPort || '-'}
datasourceUrl=${data.datasourceUrl || '-'}
datasourceUsername=${data.datasourceUsername || '-'}
redis=${data.redisHost || '-'}:${data.redisPort || '-'}
ollamaBaseUrl=${data.ollamaBaseUrl || '-'}
multipart.maxFileSize=${data.multipartMaxFileSize || '-'}
multipart.maxRequestSize=${data.multipartMaxRequestSize || '-'}
ragVectorStore=${data.ragVectorStore || '-'}
ragRegistryBaseDir=${data.ragRegistryBaseDir || '-'}
ragMaxUploadFileCount=${data.ragMaxUploadFileCount || '-'}
llmTimeoutMs=${data.llmTimeoutMs || '-'}
llmRetryAttempts=${data.llmRetryAttempts || '-'}
llmRetryBackoffMs=${data.llmRetryBackoffMs || '-'}
* datasource/redis/port/profile 등은 조회 전용이며 실제 변경은 재시작이 필요합니다.`);
  }
  async function loadDebugConfig() {
    try {
      const data = await fetchJson('/debug/api/config');
      setVal('resolverMode', data.resolverMode || 'HYBRID');
      setVal('generalParserMode', data.generalParserMode || 'HYBRID');
      setVal('travelParserMode', data.travelParserMode || 'HYBRID');
      setVal('devParserMode', data.devParserMode || 'HYBRID');
      setVal('miceParserMode', data.miceParserMode || 'HYBRID');
      setVal('fallbackPolicy', data.fallbackPolicy || 'BLOCK_OPENAI');
      setVal('llmTimeoutMs', data.llmTimeoutMs || 45000);
      setVal('llmRetryAttempts', data.llmRetryAttempts || 2);
      setVal('llmRetryBackoffMs', data.llmRetryBackoffMs || 800);
      setVal('ragEnabledSelect', String(data.ragEnabled ?? true));
      setVal('ragTopK', data.ragTopK || 4);
      setVal('ragSimilarityThreshold', data.ragSimilarityThreshold ?? 0.72);
      setVal('ragIncludeCitations', String(data.ragIncludeCitations ?? true));
      setVal('ragGeneralEnabled', String(data.ragGeneralEnabled ?? false));
      setVal('ragDevEnabled', String(data.ragDevEnabled ?? true));
      setVal('ragMiceEnabled', String(data.ragMiceEnabled ?? true));
      setVal('ragTravelEnabled', String(data.ragTravelEnabled ?? false));
      setVal('ragDatasetLocation', data.ragDatasetLocation || '');
      setVal('ragMaxUploadFileCount', data.ragMaxUploadFileCount || 20);
      setStatusBox('configStatus', `설정 조회 완료
resolver=${data.resolverMode}
general=${data.generalParserMode}
travel=${data.travelParserMode}
dev=${data.devParserMode}
mice=${data.miceParserMode}
fallbackPolicy=${data.fallbackPolicy}
ragEnabled=${data.ragEnabled}
ragTopK=${data.ragTopK}
similarityThreshold=${data.ragSimilarityThreshold}
maxUploadFileCount=${data.ragMaxUploadFileCount}
conversationId=${data.conversationId}`, 'success');
      renderMemoryStoreInfo(data);
      renderServerSettings(data);
    } catch (e) {
      setStatusBox('configStatus', '설정 조회 실패: ' + e.message, 'error');
    }
  }
  async function saveDebugConfig() {
    try {
      const payload = {
        resolverMode: val('resolverMode'), generalParserMode: val('generalParserMode'), travelParserMode: val('travelParserMode'), devParserMode: val('devParserMode'), miceParserMode: val('miceParserMode'),
        fallbackPolicy: val('fallbackPolicy'), llmTimeoutMs: Number(val('llmTimeoutMs') || 45000), llmRetryAttempts: Number(val('llmRetryAttempts') || 2), llmRetryBackoffMs: Number(val('llmRetryBackoffMs') || 800),
        ragEnabled: val('ragEnabledSelect') === 'true', ragTopK: Number(val('ragTopK') || 4), ragSimilarityThreshold: Number(val('ragSimilarityThreshold') || 0.72), ragIncludeCitations: val('ragIncludeCitations') === 'true',
        ragGeneralEnabled: val('ragGeneralEnabled') === 'true', ragDevEnabled: val('ragDevEnabled') === 'true', ragMiceEnabled: val('ragMiceEnabled') === 'true', ragTravelEnabled: val('ragTravelEnabled') === 'true',
        ragDatasetLocation: val('ragDatasetLocation'), ragMaxUploadFileCount: Number(val('ragMaxUploadFileCount') || 20)
      };
      const data = await fetchJson('/debug/api/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      renderMemoryStoreInfo(data); renderServerSettings(data);
      setStatusBox('configStatus', `설정 저장 완료
resolver=${data.resolverMode}
fallbackPolicy=${data.fallbackPolicy}
ragEnabled=${data.ragEnabled}
ragTopK=${data.ragTopK}
maxUploadFileCount=${data.ragMaxUploadFileCount}`, 'success');
      await loadRagStatus();
    } catch (e) {
      setStatusBox('configStatus', '설정 저장 실패: ' + e.message, 'error');
    }
  }
  async function resetDebugConfig() {
    try {
      await fetchJson('/debug/api/config/reset', { method: 'POST' });
      await loadDebugConfig();
      await loadRagStatus();
      setStatusBox('configStatus', '설정 초기화 완료', 'success');
    } catch (e) {
      setStatusBox('configStatus', '설정 초기화 실패: ' + e.message, 'error');
    }
  }

  function setOllamaModelStatus(text) { setText('ollamaModelStatus', text); }
  function fillModelSelect(selectId, models, selectedValue) {
    const select = byId(selectId); if (!select) return;
    select.innerHTML = '';
    const empty = document.createElement('option'); empty.value=''; empty.textContent='(선택 안 함)'; select.appendChild(empty);
    (models || []).forEach(model => { const opt = document.createElement('option'); opt.value=model.name; opt.textContent=model.displayName || model.name; if (selectedValue && selectedValue===model.name) opt.selected=true; select.appendChild(opt); });
  }
  function renderOllamaModelTable(models) {
    const table = byId('ollamaModelTable'); if (!table) return;
    if (!models || !models.length) { table.innerHTML = '<tr><td colspan="4">조회된 모델이 없습니다.</td></tr>'; setText('ollamaModelSummary', '모델 0건'); return; }
    table.innerHTML = models.map(model => `<tr><td>${htmlEscape(model.displayName || model.name)}</td><td>${htmlEscape(model.name || '-')}</td><td>${htmlEscape(model.state || '-')}</td><td>${model.size ? Number(model.size).toLocaleString() : '-'}</td></tr>`).join('');
    const running = models.filter(m => String(m.state || '').includes('RUNNING')).length;
    const installed = models.filter(m => String(m.state || '').includes('INSTALLED')).length;
    setText('ollamaModelSummary', `전체 ${models.length}건 / running 포함 ${running} / installed 포함 ${installed}`);
  }
  function applyOllamaFilters() {
    const keyword = val('ollamaSearchKeyword').toLowerCase();
    const state = val('ollamaStateFilter') || 'ALL';
    const filtered = allOllamaModels.filter(model => {
      const target = `${model.name || ''} ${model.displayName || ''}`.toLowerCase();
      const keywordMatched = !keyword || target.includes(keyword);
      const stateMatched = state === 'ALL' || String(model.state || '').includes(state);
      return keywordMatched && stateMatched;
    });
    ['travelSearchModel','travelPlanModel','generalModel','devModel','miceModel'].forEach(id => fillModelSelect(id, filtered, byId(id)?.value));
    renderOllamaModelTable(filtered);
  }
  async function loadOllamaModels() {
    try {
      const source = val('ollamaModelSource') || 'RUNNING';
      allOllamaModels = await fetchJson(`/debug/api/ollama/models?source=${encodeURIComponent(source)}`);
      applyOllamaFilters();
      setOllamaModelStatus(`모델 목록 로드 완료 (${source}) / 개수=${allOllamaModels.length}`);
    } catch (e) {
      setOllamaModelStatus('모델 목록 조회 실패: ' + e.message);
    }
  }
  async function loadOllamaModelConfig() {
    try {
      const config = await fetchJson('/debug/api/ollama/config');
      setVal('ollamaModelSource', config.modelSource || 'RUNNING');
      await loadOllamaModels();
      setVal('travelSearchModel', config.travelSearchModel || '');
      setVal('travelPlanModel', config.travelPlanModel || '');
      setVal('generalModel', config.generalModel || '');
      setVal('devModel', config.devModel || '');
      setVal('miceModel', config.miceModel || '');
      setOllamaModelStatus(`모델 설정 조회 완료
source=${config.modelSource}
travelSearch=${config.travelSearchModel || '(none)'}
travelPlan=${config.travelPlanModel || '(none)'}
general=${config.generalModel || '(none)'}
dev=${config.devModel || '(none)'}
mice=${config.miceModel || '(none)'}`);
    } catch (e) {
      setOllamaModelStatus('모델 설정 조회 실패: ' + e.message);
    }
  }
  async function saveOllamaModelConfig() {
    try {
      const payload = { modelSource: val('ollamaModelSource'), travelSearchModel: val('travelSearchModel'), travelPlanModel: val('travelPlanModel'), generalModel: val('generalModel'), devModel: val('devModel'), miceModel: val('miceModel') };
      const config = await fetchJson('/debug/api/ollama/config', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      setOllamaModelStatus(`모델 설정 저장 완료
source=${config.modelSource}
travelSearch=${config.travelSearchModel || '(none)'}
travelPlan=${config.travelPlanModel || '(none)'}
general=${config.generalModel || '(none)'}
dev=${config.devModel || '(none)'}
mice=${config.miceModel || '(none)'}`);
    } catch (e) {
      setOllamaModelStatus('모델 설정 저장 실패: ' + e.message);
    }
  }
  async function resetOllamaModelConfig() {
    try { await fetchJson('/debug/api/ollama/config/reset', { method: 'POST' }); await loadOllamaModelConfig(); setOllamaModelStatus('모델 설정 초기화 완료'); }
    catch (e) { setOllamaModelStatus('모델 설정 초기화 실패: ' + e.message); }
  }

  function renderDbInfo(data) {
    setText('statusDb', data?.jdbc?.connected ? 'connected' : 'offline');
    setText('ragEnabled', data?.ragEnabled ? 'on' : 'off');
    setText('ragStore', data?.vectorStoreType || '-');
    setText('ragRegistry', data?.registryBaseDir || '-');
    setText('ragFilter', `${data?.registry?.manifestCount ?? 0} sources / ${data?.registry?.totalFiles ?? 0} files`);
    const stats = [
      { label:'Vector Rows', value:data?.vectorDb?.rowCount ?? 0 },
      { label:'Distinct Sources', value:data?.vectorDb?.distinctSources ?? 0 },
      { label:'Registry Sources', value:data?.registry?.manifestCount ?? 0 }
    ];
    const statsEl = byId('dbInfoStats');
    if (statsEl) statsEl.innerHTML = stats.map(stat => `<div class="stat-card"><span class="muted">${stat.label}</span><strong>${htmlEscape(stat.value)}</strong></div>`).join('');
    renderJson('dbInfoPanel', data);
  }
  async function loadDbInfo() {
    try { renderDbInfo(await fetchJson('/debug/api/rag/db-info')); }
    catch (e) { setText('statusDb', 'error'); setText('dbInfoPanel', 'DB 정보 조회 실패: ' + e.message); }
  }
  async function loadRagStatus() {
    try {
      const data = await fetchJson('/debug/api/rag/status');
      setText('ragEnabled', data.enabled ? 'on' : 'off');
      setText('ragStore', data.vectorStore || '-');
      setText('ragRegistry', data.registryBaseDir || '-');
      setText('ragFilter', `max-upload=${data.maxUploadFileCount || '-'} / topK=${data.topK || '-'}`);
    } catch (_) {}
  }

  function updateActionButtons() {
    const hasSource = !!currentSource();
    ['purgeVectorBtn','purgeRegistryBtn','reindexSameBtn','reindexCopyBtn','compareBtn','btnLoadSourceFiles'].forEach(id => { const el = byId(id); if (el) el.disabled = !hasSource; });
    const ingestFilesBtn = byId('btnIngestFiles');
    if (ingestFilesBtn) ingestFilesBtn.disabled = !(uploadSource() && uploadVersion() && (byId('ingestFiles')?.files?.length || 0) > 0);
    setStatusBox('sourceHint', hasSource ? `선택 source: ${currentSource()} / version: ${currentVersion() || '-'}` : 'source 목록에서 항목을 선택하거나 Source를 직접 입력하세요.');
  }
  function applyManifestSelection(manifest) {
    if (!manifest) return;
    setVal('sourceKey', manifest.source || ''); setVal('sourceVersion', manifest.version || '');
    setVal('requestSource', manifest.source || ''); setVal('requestVersion', manifest.version || '');
    setVal('ingestSource', manifest.source || ''); setVal('ingestVersion', manifest.version || ''); setVal('ingestTitle', manifest.title || '');
    setVal('ragSource', manifest.source || ''); setVal('ragVersion', manifest.version || ''); setVal('ragTitle', manifest.title || '');
    updateActionButtons();
  }
  function renderSourcesTable(items) {
    const panel = byId('sourceTablePanel'); if (!panel) return;
    if (!items || !items.length) { panel.innerHTML = '<div class="panel" style="min-height:120px;max-height:260px">조회된 source가 없습니다.</div>'; return; }
    panel.innerHTML = `<table><thead><tr><th>선택</th><th>Source</th><th>Version</th><th>Title</th><th>Files</th><th>Chunks</th><th>Storage</th><th>Indexed</th></tr></thead><tbody>${items.map((item, index) => `<tr><td><button type="button" class="mini-btn" data-select-source="${index}">선택</button></td><td>${htmlEscape(item.source)}</td><td>${htmlEscape(item.version)}</td><td>${htmlEscape(item.title || '-')}</td><td>${item.fileCount ?? 0}</td><td>${item.chunkCount ?? 0}</td><td>${htmlEscape(item.storageKind || '-')}</td><td>${htmlEscape(item.lastIndexedAt || item.ingestedAt || '-')}</td></tr>`).join('')}</tbody></table>`;
    panel.querySelectorAll('[data-select-source]').forEach(btn => btn.addEventListener('click', () => selectSourceByIndex(Number(btn.dataset.selectSource))));
  }
  function renderSourceFilesTable(items) {
    const panel = byId('sourceFilesTablePanel'); if (!panel) return;
    if (!items || !items.length) { panel.innerHTML = '<div class="panel" style="min-height:120px;max-height:260px">등록된 파일이 없습니다.</div>'; return; }
    panel.innerHTML = `<table><thead><tr><th>fileId</th><th>파일명</th><th>제목</th><th>타입</th><th>Storage</th><th>Chunks</th><th>Indexed</th><th>Action</th></tr></thead><tbody>${items.map(item => `<tr><td>${htmlEscape(item.fileId)}</td><td>${htmlEscape(item.originalFileName || item.fileName || '-')}</td><td>${htmlEscape(item.title || '-')}</td><td>${htmlEscape(item.contentType || '-')}</td><td>${htmlEscape(item.storageKind || '-')}</td><td>${item.chunkCount ?? 0}</td><td>${htmlEscape(item.lastIndexedAt || item.ingestedAt || '-')}</td><td><button type="button" class="mini-btn" data-file-purge="${htmlEscape(item.fileId)}">삭제</button></td></tr>`).join('')}</tbody></table>`;
    panel.querySelectorAll('[data-file-purge]').forEach(btn => btn.addEventListener('click', () => purgeSourceFile(btn.dataset.filePurge)));
  }
  function selectSourceByIndex(index) { const item = loadedSources[index]; if (!item) return; applyManifestSelection(item); renderSourcesTable(loadedSources); loadSourceFiles(); }
  async function loadSources() {
    try {
      const url = `/debug/api/rag/sources?category=${encodeURIComponent(currentCategory())}`;
      loadedSources = await fetchJson(url);
      renderSourcesTable(loadedSources);
      renderJson('resultPanel', loadedSources);
      if (loadedSources.length === 1) selectSourceByIndex(0);
      updateActionButtons();
    } catch (e) {
      setText('resultPanel', 'source 목록 조회 실패: ' + e.message);
    }
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
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: ensure(currentVersion(), 'version은 필수입니다.'), deleteRegistry };
      renderJson('resultPanel', await fetchJson('/debug/api/rag/source/purge', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) }));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', 'source 삭제 실패: ' + e.message); }
  }
  async function purgeSourceFile(fileId) {
    try {
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: ensure(currentVersion(), 'version은 필수입니다.'), fileId, deleteRegistry: true };
      renderJson('resultPanel', await fetchJson('/debug/api/rag/source/file/purge', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) }));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', '파일 삭제 실패: ' + e.message); }
  }
  async function reindexSource(copyToNewVersion) {
    try {
      const payload = { category: currentCategory(), source: ensure(currentSource(), 'source는 필수입니다.'), version: ensure(currentVersion(), 'version은 필수입니다.'), targetVersion: val('targetVersion'), purgeBeforeReindex: true, copyToNewVersion };
      const data = await fetchJson('/debug/api/rag/source/reindex', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderJson('resultPanel', data);
      if (copyToNewVersion && val('targetVersion')) setVal('sourceVersion', val('targetVersion'));
      await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) { setText('resultPanel', '재색인 실패: ' + e.message); }
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
      const payload = { category: currentCategory(), source: uploadSource(), version: uploadVersion(), title: uploadTitle(), text: val('ingestTextBody'), metadata: parseMetadata(val('ingestMetadata')) };
      ensure(payload.text, '적재할 텍스트를 입력하세요.');
      const data = await fetchJson('/debug/api/rag/ingest-text', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderJson('ingestPanel', data); applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles); await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', 'Text 적재 실패: ' + e.message); }
  }
  async function ingestFilesSource() {
    try {
      ensure(uploadSource(), '멀티파일 업로드에서는 source가 필수입니다.'); ensure(uploadVersion(), 'version은 필수입니다.');
      const files = byId('ingestFiles').files; ensure(files.length > 0, '업로드할 파일을 선택하세요.');
      const form = new FormData(); form.append('category', currentCategory()); form.append('source', uploadSource()); form.append('version', uploadVersion()); form.append('title', uploadTitle());
      const metadata = parseMetadata(val('ingestMetadata')); if (Object.keys(metadata).length) form.append('metadataJson', JSON.stringify(metadata));
      Array.from(files).forEach(file => form.append('files', file));
      const data = await fetchJson('/debug/api/rag/ingest-files', { method:'POST', body: form });
      renderJson('ingestPanel', data); applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles); await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', '멀티파일 업로드 실패: ' + e.message); }
  }
  async function ingestUrlSource() {
    try {
      const payload = { category: currentCategory(), url: val('ingestUrl'), source: uploadSource(), version: uploadVersion(), title: uploadTitle(), metadata: parseMetadata(val('ingestMetadata')) };
      ensure(payload.url, '적재할 URL을 입력하세요.');
      const data = await fetchJson('/debug/api/rag/ingest-url', { method:'POST', headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(payload) });
      renderJson('ingestPanel', data); applyManifestSelection(data.manifest); loadedSourceFiles = data.files || []; renderSourceFilesTable(loadedSourceFiles); await loadSources(); await loadDbInfo();
    } catch (e) { setText('ingestPanel', 'URL 적재 실패: ' + e.message); }
  }

  function renderUploadRows(items) {
    const body = byId('uploadResultTable'); if (!body) return;
    if (!items || !items.length) { body.innerHTML = '<tr><td colspan="8">아직 결과가 없습니다.</td></tr>'; return; }
    body.innerHTML = items.map(item => `<tr><td>${htmlEscape(item.fileName || '-')}</td><td>${htmlEscape(item.source || '-')}</td><td>${htmlEscape(item.version || '-')}</td><td>${htmlEscape(item.title || '-')}</td><td>${item.chunkCount ?? 0}</td><td>${item.traceId ? `<button type="button" class="mini-btn" data-trace-id="${htmlEscape(item.traceId)}">${htmlEscape(item.traceId)}</button>` : '-'}</td><td>${item.stored ? 'SUCCESS' : 'FAIL'}</td><td>${htmlEscape(item.message || '-')}</td></tr>`).join('');
    body.querySelectorAll('[data-trace-id]').forEach(btn => btn.addEventListener('click', () => loadTraceById(btn.dataset.traceId)));
  }
  function applyUploadIdentity(result) {
    if (!result) return;
    if (result.source) setVal('ragSource', result.source); if (result.version) setVal('ragVersion', result.version); if (result.title) setVal('ragTitle', result.title); if (result.traceId) setVal('ragTraceId', result.traceId);
  }
  async function uploadSingleFile() {
    const file = byId('ragFile').files[0]; if (!file) { setStatusBox('uploadStatus', '단일 파일 업로드 실패\n파일을 먼저 선택하세요.', 'error'); return; }
    const payload = currentQuickUploadPayload(); const form = new FormData(); form.append('category', payload.category); form.append('source', payload.source); form.append('version', payload.version); form.append('title', payload.title); form.append('file', file);
    try {
      setLoading(true, '단일 파일 업로드 중...'); setStatusBox('uploadStatus', '단일 파일 업로드를 처리 중입니다...', 'running');
      const data = await fetchJson('/debug/api/rag/ingest-file', { method: 'POST', body: form });
      applyUploadIdentity(data.result); renderUploadRows([{ fileName: file.name, source: data.result.source, version: data.result.version, title: data.result.title, chunkCount: data.result.chunkCount, traceId: data.result.traceId, stored: data.result.stored, message: 'stored' }]);
      setStatusBox('uploadStatus', `단일 파일 업로드 완료
source=${data.result.source}
version=${data.result.version}
chunks=${data.result.chunkCount}
trace=${data.result.traceId || '-'}`, 'success');
      applyManifestSelection(data.manifest); await loadTraceById(data.result.traceId); await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) {
      renderUploadRows([{ fileName: file.name, source: '-', version: '-', title: '-', chunkCount: 0, stored: false, message: e.message }]); setStatusBox('uploadStatus', '단일 파일 업로드 실패\n' + e.message, 'error');
    } finally { setLoading(false); }
  }
  async function uploadMultiFiles() {
    const files = Array.from(byId('ragFiles').files || []); if (!files.length) { setStatusBox('uploadStatus', '멀티파일 업로드 실패\n파일을 먼저 선택하세요.', 'error'); return; }
    const payload = currentQuickUploadPayload(); const form = new FormData(); form.append('category', payload.category); form.append('source', payload.source); form.append('version', payload.version); form.append('title', payload.title); files.forEach(file => form.append('files', file));
    try {
      setLoading(true, `멀티파일 업로드 중... (${files.length}개)`); setStatusBox('uploadStatus', `멀티파일 업로드를 처리 중입니다...
선택 파일 수=${files.length}`, 'running');
      const data = await fetchJson('/debug/api/rag/ingest-files', { method: 'POST', body: form });
      applyUploadIdentity(data.result); renderUploadRows(data.result.files || []); setStatusBox('uploadStatus', `멀티파일 업로드 완료
source=${data.result.source}
version=${data.result.version}
성공=${data.result.successCount}, 실패=${data.result.failCount}, chunks=${data.result.totalChunkCount}
trace=${data.result.traceId || '-'}`, data.result.failCount > 0 ? 'running' : 'success');
      applyManifestSelection(data.manifest); await loadTraceById(data.result.traceId); await loadSources(); await loadSourceFiles(); await loadDbInfo();
    } catch (e) {
      setStatusBox('uploadStatus', '멀티파일 업로드 실패\n' + e.message, 'error');
    } finally { setLoading(false); }
  }

  function formatTraceDetails(details) { if (!details || Object.keys(details).length === 0) return '-'; return Object.entries(details).map(([k,v]) => `${k}=${typeof v === 'object' ? JSON.stringify(v) : v}`).join(', '); }
  function renderRagTraceResponse(data) {
    const entries = data.entries || []; const summaries = data.summaries || (data.summary ? [data.summary] : []);
    setText('ragTraceSummary', `trace ${summaries.length}건 / entry ${entries.length}건`);
    if (!entries.length) { setText('ragTracePanel', '조회된 RAG trace가 없습니다.'); return; }
    const lines = []; summaries.forEach(summary => { if (!summary) return; lines.push(`[TRACE ${summary.traceId}] ${summary.operation} | ${summary.finalStatus} | ${summary.lastStage} | ${summary.lastMessage}`); }); if (lines.length) lines.push('');
    entries.forEach(entry => { lines.push(`[${entry.timestamp}] [${entry.traceId}] ${entry.operation} > ${entry.stage} > ${entry.status} :: ${entry.message}`); lines.push(`  - ${formatTraceDetails(entry.details)}`); });
    setText('ragTracePanel', lines.join('\n'));
  }
  async function loadLatestRagTraces() { try { renderRagTraceResponse(await fetchJson(`/debug/api/rag/traces?limit=${encodeURIComponent(val('ragTraceLimit') || '150')}`)); } catch (e) { setText('ragTracePanel', 'RAG trace 조회 실패: ' + e.message); } }
  async function loadTraceById(traceIdValue) {
    try { const traceId = traceIdValue || val('ragTraceId'); if (!traceId) { setText('ragTracePanel', '조회할 Trace ID를 입력하세요.'); return; } setVal('ragTraceId', traceId); renderRagTraceResponse(await fetchJson(`/debug/api/rag/traces/${encodeURIComponent(traceId)}`)); }
    catch (e) { setText('ragTracePanel', 'Trace 상세 조회 실패: ' + e.message); }
  }
  async function clearRagTraces() { try { await fetchJson('/debug/api/rag/traces/clear', { method: 'POST' }); setText('ragTraceSummary', 'RAG trace 비움 완료'); setText('ragTracePanel', '아직 조회하지 않음'); } catch (e) { setText('ragTracePanel', 'RAG trace 비우기 실패: ' + e.message); } }

  function resetStreamUi() { setText('streamLog', ''); Object.keys(agentStateMap).forEach(k => delete agentStateMap[k]); renderAgentChips(); }
  function renderAgentChips() {
    const container = byId('agentChips'); if (!container) return; container.innerHTML = '';
    Object.entries(agentStateMap).forEach(([agent, info]) => { const chip = document.createElement('span'); chip.className = 'chip'; chip.textContent = `${agent} | ${info.status || '-'} | ${info.message || '-'}`; container.appendChild(chip); });
  }
  function logLine(text) { const log = byId('streamLog'); if (!log) return; log.textContent += text + '\n'; log.scrollTop = log.scrollHeight; }
  function buildChatUrl() { let url = `/api/chat/stream?message=${encodeURIComponent(val('message'))}`; if (val('category')) url += `&category=${encodeURIComponent(val('category'))}`; if (val('requestSource')) url += `&source=${encodeURIComponent(val('requestSource'))}`; if (val('requestVersion')) url += `&version=${encodeURIComponent(val('requestVersion'))}`; return url; }
  function startStream() {
    if (eventSource) { eventSource.close(); eventSource = null; }
    resetStreamUi(); if (!val('message')) { logLine('[error] 메시지를 입력하세요.'); return; }
    const url = buildChatUrl(); logLine('[request] ' + url); eventSource = new EventSource(url);
    eventSource.addEventListener('agent', (event) => { try { const data = JSON.parse(event.data); agentStateMap[data.agent] = { status: data.status, message: data.message }; renderAgentChips(); logLine(`[agent] ${data.agent} | ${data.status} | ${data.message}`); } catch { logLine('[agent] ' + event.data); } });
    eventSource.addEventListener('message', (event) => logLine('[message] ' + event.data));
    eventSource.addEventListener('complete', async () => { logLine('[complete] 스트림 종료'); if (eventSource) { eventSource.close(); eventSource = null; } await loadLatestRagTraces(); });
    eventSource.addEventListener('error', async () => { logLine('[error] 스트림 오류 또는 종료'); if (eventSource) { eventSource.close(); eventSource = null; } await loadLatestRagTraces(); });
  }
  async function previewRagSearch() {
    try { let url = `/debug/api/rag/search?category=${encodeURIComponent(currentCategory())}&query=${encodeURIComponent(val('message'))}`; if (val('requestSource')) url += `&source=${encodeURIComponent(val('requestSource'))}`; if (val('requestVersion')) url += `&version=${encodeURIComponent(val('requestVersion'))}`; renderJson('resultPanel', await fetchJson(url)); }
    catch (e) { setText('resultPanel', 'RAG 검색 실패: ' + e.message); }
  }
  async function loadMemory() { try { renderJson('memoryPanel', await fetchJson('/debug/api/memory')); } catch (e) { setText('memoryPanel', '메모리 조회 실패: ' + e.message); } }
  async function clearMemory() { try { const res = await fetch('/debug/api/memory/clear', { method:'POST' }); const text = await res.text(); if (!res.ok) throw new Error(text); setText('memoryPanel', text); } catch (e) { setText('memoryPanel', '메모리 초기화 실패: ' + e.message); } }

  function bindEvent(id, eventName, handler) { const el = byId(id); if (el) el.addEventListener(eventName, handler); }
  document.addEventListener('DOMContentLoaded', async () => {
    bindEvent('example', 'change', applyExample);
    ['sourceKey','sourceVersion','requestSource','requestVersion','targetVersion','ingestSource','ingestVersion'].forEach(id => bindEvent(id, 'input', updateActionButtons));
    bindEvent('ingestFiles', 'change', updateActionButtons);
    bindEvent('ollamaModelSource', 'change', loadOllamaModels); bindEvent('ollamaSearchKeyword', 'input', applyOllamaFilters); bindEvent('ollamaStateFilter', 'change', applyOllamaFilters);
    bindEvent('btnLoadOllamaConfig', 'click', loadOllamaModelConfig); bindEvent('btnSaveOllamaConfig', 'click', saveOllamaModelConfig); bindEvent('btnResetOllamaConfig', 'click', resetOllamaModelConfig);
    bindEvent('btnLoadDebugConfig', 'click', loadDebugConfig); bindEvent('btnSaveDebugConfig', 'click', saveDebugConfig); bindEvent('btnResetDebugConfig', 'click', resetDebugConfig); bindEvent('btnLoadDbInfo', 'click', loadDbInfo);
    bindEvent('btnStream', 'click', startStream); bindEvent('btnPreviewSearch', 'click', previewRagSearch); bindEvent('btnLoadMemory', 'click', loadMemory); bindEvent('btnClearMemory', 'click', clearMemory);
    bindEvent('btnLoadSources', 'click', loadSources); bindEvent('btnLoadSourceFiles', 'click', loadSourceFiles); bindEvent('purgeVectorBtn', 'click', () => purgeSource(false)); bindEvent('purgeRegistryBtn', 'click', () => purgeSource(true)); bindEvent('reindexSameBtn', 'click', () => reindexSource(false)); bindEvent('reindexCopyBtn', 'click', () => reindexSource(true)); bindEvent('compareBtn', 'click', compareVersions);
    bindEvent('btnIngestText', 'click', ingestTextSource); bindEvent('btnIngestFiles', 'click', ingestFilesSource); bindEvent('btnIngestUrl', 'click', ingestUrlSource);
    bindEvent('btnUploadSingle', 'click', uploadSingleFile); bindEvent('btnUploadMulti', 'click', uploadMultiFiles);
    bindEvent('btnLoadLatestRagTraces', 'click', loadLatestRagTraces); bindEvent('btnLoadTraceById', 'click', () => loadTraceById()); bindEvent('btnClearRagTraces', 'click', clearRagTraces);
    updateActionButtons();
    await loadDebugConfig(); await loadOllamaModelConfig(); await loadRagStatus(); await loadDbInfo(); await loadSources(); await loadLatestRagTraces();
  });
})();
