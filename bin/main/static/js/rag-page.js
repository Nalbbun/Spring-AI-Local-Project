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
    loadStatus();
    listSources();
  });
})();
