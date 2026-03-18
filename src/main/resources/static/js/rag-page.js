(() => {
  const { qs, val, setVal, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;
  const { logLine } = window.ChatCommon;

  const logEl = () => qs('eventLog');

  function log(msg) { logLine(logEl(), msg); }

  // ── 상태 조회 ──────────────────────────────────────────
  async function loadStatus() {
    try {
      const s = await fetchJson('/debug/api/rag/status');
      setText('ragEnabledChip',  `rag: ${s.enabled ? '✅ on' : '❌ off'}`);
      setText('ragVectorChip',   `vector: ${s.vectorStore || '-'}`);
      setText('ragVectorStore',  s.vectorStore || '-');
      setText('ragSearchParams', `topK: ${s.topK}  threshold: ${s.similarityThreshold}`);
      const cats = s.categories || {};
      setText('ragCategories',
        Object.entries(cats).map(([k, v]) => `${k}: ${v ? '✅' : '❌'}`).join('\n'));
    } catch (e) { log('[status] 조회 실패: ' + e.message); }

    try {
      const d = await fetchJson('/debug/api/rag/db-info');
      setText('ragDbChip', `db: ${d?.jdbc?.connected ? '🟢 ok' : '🔴 offline'} / ${d?.vectorDb?.rowCount ?? 0} chunks`);
    } catch { setText('ragDbChip', 'db: -'); }
  }

  // ── 소스 목록 ──────────────────────────────────────────
  async function listSources() {
    const category = val('listCategory');
    try {
      const items = await fetchJson(`/debug/api/rag/sources?category=${encodeURIComponent(category)}`);
      const tbody = qs('sourceTableBody');
      if (!items?.length) {
        tbody.innerHTML = '<tr><td colspan="8">등록된 소스가 없습니다.</td></tr>';
        return;
      }
      tbody.innerHTML = items.map(item => `
        <tr>
          <td>${htmlEscape(item.source || '-')}</td>
          <td>${htmlEscape(item.version || '-')}</td>
          <td>${htmlEscape(item.title || '-')}</td>
          <td>${item.fileCount ?? 0}</td>
          <td>${item.chunkCount ?? 0}</td>
          <td>${htmlEscape((item.lastIndexedAt || '-').substring(0, 19))}</td>
          <td>${htmlEscape(item.ingestType || '-')}</td>
          <td>
            <div style="display:flex;gap:6px;flex-wrap:wrap">
              <button class="yellow" style="min-width:60px;padding:4px 8px;font-size:12px"
                onclick="reindex('${htmlEscape(category)}','${htmlEscape(item.source)}','${htmlEscape(item.version)}')">재인덱스</button>
              <button class="red" style="min-width:60px;padding:4px 8px;font-size:12px"
                onclick="purgeSource('${htmlEscape(category)}','${htmlEscape(item.source)}','${htmlEscape(item.version)}')">삭제</button>
            </div>
          </td>
        </tr>`).join('');
      log(`[sources] ${category} — ${items.length}개 조회 완료`);
    } catch (e) {
      log('[sources] 조회 실패: ' + e.message);
    }
  }

  window.reindex = async (category, source, version) => {
    if (!confirm(`재인덱스: ${source} / ${version}`)) return;
    try {
      const d = await fetchJson('/debug/api/rag/source/reindex', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ category, source, version }),
      });
      log(`[reindex] 완료: ${source}/${version} — ${pretty(d)}`);
      await listSources();
    } catch (e) { log('[reindex] 실패: ' + e.message); }
  };

  window.purgeSource = async (category, source, version) => {
    if (!confirm(`삭제: ${source} / ${version} — 벡터 DB에서도 제거됩니다.`)) return;
    try {
      await fetchJson('/debug/api/rag/source/purge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ category, source, version }),
      });
      log(`[purge] 완료: ${source}/${version}`);
      await listSources();
      await loadStatus();
    } catch (e) { log('[purge] 실패: ' + e.message); }
  };

  // ── 파일 인제스트 ──────────────────────────────────────
  function ingestMeta() {
    return {
      category: val('ingestCategory'),
      source:   val('ingestSource'),
      version:  val('ingestVersion') || 'v1',
      title:    val('ingestTitle'),
    };
  }

  async function ingestSingle() {
    const file = qs('ingestFile')?.files?.[0];
    if (!file) { setText('fileIngestStatus', '⚠ 파일을 선택하세요.'); return; }
    const meta = ingestMeta();
    const form = new FormData();
    Object.entries(meta).forEach(([k, v]) => form.append(k, v));
    form.append('file', file);
    try {
      setText('fileIngestStatus', `업로드 중: ${file.name}...`);
      const d = await fetchJson('/debug/api/rag/ingest-file', { method: 'POST', body: form });
      setText('fileIngestStatus',
        `✅ 완료: ${file.name}\nsource=${d.result.source}\nversion=${d.result.version}\nchunks=${d.result.chunkCount}`);
      log(`[ingest-file] ${file.name} → ${d.result.source}/${d.result.version} (${d.result.chunkCount} chunks)`);
      await listSources(); await loadStatus();
    } catch (e) {
      setText('fileIngestStatus', '❌ 실패: ' + e.message);
      log('[ingest-file] 실패: ' + e.message);
    }
  }

  async function ingestMulti() {
    const files = Array.from(qs('ingestFiles')?.files || []);
    if (!files.length) { setText('fileIngestStatus', '⚠ 파일을 선택하세요.'); return; }
    const meta = ingestMeta();
    const form = new FormData();
    Object.entries(meta).forEach(([k, v]) => form.append(k, v));
    files.forEach(f => form.append('files', f));
    try {
      setText('fileIngestStatus', `멀티 업로드 중... (${files.length}개)`);
      const d = await fetchJson('/debug/api/rag/ingest-files', { method: 'POST', body: form });
      setText('fileIngestStatus',
        `✅ 완료: ${files.length}개\nsource=${d.result.source}\n성공=${d.result.successCount} / 실패=${d.result.failCount}\ntotal chunks=${d.result.totalChunkCount}`);
      log(`[ingest-files] ${files.length}개 완료 — success=${d.result.successCount} fail=${d.result.failCount}`);
      await listSources(); await loadStatus();
    } catch (e) {
      setText('fileIngestStatus', '❌ 실패: ' + e.message);
      log('[ingest-files] 실패: ' + e.message);
    }
  }

  // ── URL / 텍스트 인제스트 ──────────────────────────────
  function urlMeta() {
    return {
      category: val('urlIngestCategory'),
      source:   val('urlIngestSource'),
      version:  val('urlIngestVersion') || 'v1',
      title:    val('urlIngestTitle'),
    };
  }

  async function ingestUrl() {
    const url = val('ingestUrl');
    if (!url) { setText('urlIngestStatus', '⚠ URL을 입력하세요.'); return; }
    const body = { ...urlMeta(), url };
    try {
      setText('urlIngestStatus', `URL 인제스트 중: ${url}`);
      const d = await fetchJson('/debug/api/rag/ingest-url', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      setText('urlIngestStatus',
        `✅ 완료\nsource=${d.result.source}\nversion=${d.result.version}\nchunks=${d.result.chunkCount}`);
      log(`[ingest-url] ${url} → chunks=${d.result.chunkCount}`);
      await listSources(); await loadStatus();
    } catch (e) {
      setText('urlIngestStatus', '❌ 실패: ' + e.message);
      log('[ingest-url] 실패: ' + e.message);
    }
  }

  async function ingestText() {
    const text = val('ingestText');
    if (!text) { setText('urlIngestStatus', '⚠ 텍스트를 입력하세요.'); return; }
    const body = { ...urlMeta(), text };
    try {
      setText('urlIngestStatus', '텍스트 인제스트 중...');
      const d = await fetchJson('/debug/api/rag/ingest-text', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      setText('urlIngestStatus',
        `✅ 완료\nsource=${d.result.source}\nversion=${d.result.version}\nchunks=${d.result.chunkCount}`);
      log(`[ingest-text] → chunks=${d.result.chunkCount}`);
      await listSources(); await loadStatus();
    } catch (e) {
      setText('urlIngestStatus', '❌ 실패: ' + e.message);
      log('[ingest-text] 실패: ' + e.message);
    }
  }

  // ── RAG 검색 테스트 ────────────────────────────────────
  async function search() {
    const category = val('searchCategory');
    const query    = val('searchQuery');
    if (!query) { log('[search] 쿼리를 입력하세요.'); return; }

    let url = `/debug/api/rag/search?category=${encodeURIComponent(category)}&query=${encodeURIComponent(query)}`;
    const src = val('searchSource');
    const ver = val('searchVersion');
    if (src) url += `&source=${encodeURIComponent(src)}`;
    if (ver) url += `&version=${encodeURIComponent(ver)}`;

    try {
      const data = await fetchJson(url);
      const panel = qs('searchResultPanel');
      log(`[search] category=${category} | applied=${data?.applied} | hits=${data?.documents?.length ?? 0} | reason=${data?.reason}`);

      if (!data?.documents?.length) {
        panel.textContent = `검색 결과 없음 (applied=${data?.applied}, reason=${data?.reason})`;
        return;
      }
      panel.innerHTML = data.documents.map((doc, i) => `
        <div style="border:1px solid #334155;border-radius:8px;padding:10px;margin-bottom:8px;background:#0b1220">
          <div style="font-size:12px;color:#94a3b8;margin-bottom:6px">
            #${i+1} &nbsp;|&nbsp; <b>${htmlEscape(doc.source)}</b> / ${htmlEscape(doc.version)}
            ${doc.score != null ? `&nbsp;|&nbsp; score: <b>${doc.score.toFixed(4)}</b>` : ''}
            &nbsp;|&nbsp; ${htmlEscape(doc.title || '')}
          </div>
          <div style="white-space:pre-wrap;font-size:13px;line-height:1.6">${htmlEscape(doc.text || '')}</div>
        </div>`).join('');
    } catch (e) {
      qs('searchResultPanel').textContent = '검색 실패: ' + e.message;
      log('[search] 실패: ' + e.message);
    }
  }

  // ── 임베딩 모델 설정 ────────────────────────────────────

  async function loadEmbeddingConfig() {
    try {
      const d = await fetchJson('/debug/api/rag/embedding/config');
      setText('embCurrentModel',      d.model      || '-');
      setText('embCurrentKeepAlive',  d.keepAlive  || '-');
      setText('embCurrentDimensions', String(d.dimensions || '-'));
      // 직접 입력 필드에도 반영
      if (qs('embModelInput'))      qs('embModelInput').value      = d.model     || '';
      if (qs('embKeepAliveInput'))  qs('embKeepAliveInput').value  = d.keepAlive || '300s';
      if (qs('embDimensionsInput')) qs('embDimensionsInput').value = d.dimensions || 768;
      setText('embStatus',
          `현재 설정 — model: ${d.model} | keepAlive: ${d.keepAlive} | dimensions: ${d.dimensions}\n` +
          `기본값 — model: ${d.defaultModel} | dimensions: ${d.defaultDimensions}`);
      log(`[임베딩] 현재: ${d.model} (${d.dimensions}dim)`);
    } catch (e) {
      setText('embStatus', '임베딩 설정 조회 실패: ' + e.message);
      log('[임베딩] 조회 실패: ' + e.message);
    }
  }

  async function loadEmbeddingModels() {
    try {
      const d    = await fetchJson('/debug/api/rag/embedding/models');
      const sel  = qs('embModelSelect');
      const list = d.models || [];
      if (!sel) return;
      sel.innerHTML =
          '<option value="">-- 모델을 선택하세요 --</option>' +
          list.map(m => `<option value="${htmlEscape(m)}"${m === d.currentModel ? ' selected' : ''}>${htmlEscape(m)}</option>`).join('');
      log(`[임베딩] 모델 목록 ${list.length}개 조회: ${list.join(', ') || '(없음)'}`);
      if (!list.length) setText('embStatus', '⚠ Ollama에 설치된 모델이 없습니다. 모델을 먼저 Pull하세요.');
    } catch (e) {
      log('[임베딩] 모델 목록 조회 실패: ' + e.message);
    }
  }

  async function saveEmbeddingConfig() {
    const model     = qs('embModelInput')?.value?.trim()      || '';
    const keepAlive = qs('embKeepAliveInput')?.value?.trim()  || '300s';
    const dimensions = parseInt(qs('embDimensionsInput')?.value || '768', 10);

    if (!model) { setText('embStatus', '⚠ 임베딩 모델명을 입력하세요.'); return; }
    if (!dimensions || dimensions < 64) { setText('embStatus', '⚠ Dimensions는 64 이상이어야 합니다.'); return; }

    setText('embStatus', '저장 중...');
    try {
      const d = await fetchJson('/debug/api/rag/embedding/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ model, keepAlive, dimensions }),
      });
      setText('embCurrentModel',      d.model);
      setText('embCurrentKeepAlive',  d.keepAlive);
      setText('embCurrentDimensions', String(d.dimensions));
      setText('embStatus',
          `✅ 적용 완료 — model: ${d.model} | keepAlive: ${d.keepAlive} | dimensions: ${d.dimensions}\n` +
          `⚠ 기존 인덱스된 문서와 dimensions가 다르다면 소스 재인덱스가 필요합니다.`);
      log(`[임베딩] 설정 적용: ${d.model} (${d.dimensions}dim, keepAlive=${d.keepAlive})`);
    } catch (e) {
      setText('embStatus', '설정 저장 실패: ' + e.message);
      log('[임베딩] 저장 실패: ' + e.message);
    }
  }

  async function resetEmbeddingConfig() {
    if (!confirm('임베딩 설정을 기본값으로 초기화할까요?')) return;
    try {
      const d = await fetchJson('/debug/api/rag/embedding/config/reset', { method: 'POST' });
      setText('embCurrentModel',      d.model);
      setText('embCurrentKeepAlive',  d.keepAlive);
      setText('embCurrentDimensions', String(d.dimensions));
      if (qs('embModelInput'))      qs('embModelInput').value      = d.model;
      if (qs('embKeepAliveInput'))  qs('embKeepAliveInput').value  = d.keepAlive;
      if (qs('embDimensionsInput')) qs('embDimensionsInput').value = d.dimensions;
      setText('embStatus', `↩ 기본값으로 초기화 완료 — model: ${d.model} | dimensions: ${d.dimensions}`);
      log(`[임베딩] 초기화: ${d.model} (${d.dimensions}dim)`);
    } catch (e) {
      setText('embStatus', '초기화 실패: ' + e.message);
    }
  }

  // 프리셋 클릭 → 입력 필드 채우기
  window._applyPreset = (model, keepAlive, dimensions) => {
    if (qs('embModelInput'))      qs('embModelInput').value      = model;
    if (qs('embKeepAliveInput'))  qs('embKeepAliveInput').value  = keepAlive;
    if (qs('embDimensionsInput')) qs('embDimensionsInput').value = dimensions;
    setText('embStatus', `프리셋 선택: ${model} (${dimensions}dim) — 저장 버튼을 눌러 적용하세요.`);
    log(`[임베딩] 프리셋 선택: ${model} (${dimensions}dim)`);
  };

  // ── 초기화 ─────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnRefreshStatus')?.addEventListener('click', loadStatus);
    qs('btnListSources')?.addEventListener('click', listSources);
    qs('btnIngestSingle')?.addEventListener('click', ingestSingle);
    qs('btnIngestMulti')?.addEventListener('click', ingestMulti);
    qs('btnIngestUrl')?.addEventListener('click', ingestUrl);
    qs('btnIngestText')?.addEventListener('click', ingestText);
    qs('btnSearch')?.addEventListener('click', search);
    qs('btnSearchClear')?.addEventListener('click', () => {
      if (qs('searchResultPanel')) qs('searchResultPanel').textContent = '검색 결과가 여기에 표시됩니다.';
    });

    // 임베딩 모델 설정
    qs('btnLoadEmbConfig')?.addEventListener('click',  loadEmbeddingConfig);
    qs('btnSaveEmbConfig')?.addEventListener('click',  saveEmbeddingConfig);
    qs('btnResetEmbConfig')?.addEventListener('click', resetEmbeddingConfig);
    qs('btnLoadEmbModels')?.addEventListener('click',  loadEmbeddingModels);
    qs('btnApplySelectedModel')?.addEventListener('click', () => {
      const sel = qs('embModelSelect')?.value;
      if (!sel) { log('[임베딩] 모델을 선택해 주세요.'); return; }
      if (qs('embModelInput')) qs('embModelInput').value = sel;
      setText('embStatus', `선택된 모델: ${sel} — 저장 버튼을 눌러 적용하세요.`);
    });

    loadStatus();
    listSources();
    loadEmbeddingConfig();  // 페이지 진입 시 임베딩 설정 자동 조회
  });
})();
