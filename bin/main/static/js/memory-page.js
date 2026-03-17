(() => {
  const { qs, setText, fetchJson, pretty, htmlEscape } = window.UiCommon;

  let currentConvId = null;
  let currentSnapshot = null;
  let currentTab = 'messages';

  const logEl = () => qs('eventLog');

  function log(msg) {
    const el = logEl();
    if (!el) return;
    el.textContent += msg + '\n';
    el.scrollTop = el.scrollHeight;
  }

  // ── 요약 조회 ──────────────────────────────────────────
  async function loadSummary() {
    try {
      const d = await fetchJson('/api/memory/summary');
      const type = d.storeType || '-';
      const count = d.conversationCount ?? 0;
      setText('storeTypeInfo', type);
      setText('convCountInfo', `${count}개`);
      setText('storeTypeChip', `store: ${storeLabel(type)}`);
      setText('convCountChip', `conversations: ${count}`);
      log(`[요약] storeType=${type}, count=${count}`);
    } catch (e) {
      log('[요약] 조회 실패: ' + e.message);
    }
  }

  function storeLabel(type) {
    if (!type) return '-';
    if (type.includes('Jdbc'))    return '🐘 PostgreSQL';
    if (type.includes('Redis'))   return '🔴 Redis';
    if (type.includes('InMemory'))return '💡 InMemory';
    return type;
  }

  // ── 대화 목록 조회 ────────────────────────────────────
  async function loadConversations() {
    try {
      const ids = await fetchJson('/api/memory/conversations');
      const tbody = qs('convListTable');
      const statusEl = qs('convListStatus');

      if (!ids || !ids.length) {
        tbody.innerHTML = '<tr><td colspan="2" style="color:#64748b">저장된 대화가 없습니다.</td></tr>';
        if (statusEl) statusEl.textContent = '0개';
        log('[목록] 저장된 대화 없음');
        return;
      }

      tbody.innerHTML = ids.map(id => `
        <tr id="row-${htmlEscape(id)}" class="${id === currentConvId ? 'selected' : ''}">
          <td>
            <a href="#" style="color:#93c5fd;text-decoration:none;font-size:13px"
              onclick="window._selectConv('${htmlEscape(id)}');return false;"
            >${htmlEscape(id)}</a>
          </td>
          <td>
            <button class="red" style="width:auto;padding:4px 8px;font-size:12px"
              onclick="window._deleteConv('${htmlEscape(id)}')">🗑</button>
          </td>
        </tr>`).join('');

      if (statusEl) statusEl.textContent = `${ids.length}개 조회됨`;
      log(`[목록] ${ids.length}개 조회 완료`);

      // 현재 선택된 대화 자동 재선택
      if (currentConvId && ids.includes(currentConvId)) {
        await loadSnapshot(currentConvId);
      }
    } catch (e) {
      log('[목록] 조회 실패: ' + e.message);
    }
  }

  // ── 스냅샷 조회 ────────────────────────────────────────
  async function loadSnapshot(convId) {
    currentConvId = convId;

    // 테이블 선택 표시
    document.querySelectorAll('#convListTable tr').forEach(tr => tr.classList.remove('selected'));
    const row = qs('row-' + convId);
    if (row) row.classList.add('selected');

    setText('detailConvId', `— ${convId}`);
    log(`[선택] ${convId}`);

    try {
      const snap = await fetchJson(`/api/memory/conversations/${encodeURIComponent(convId)}`);
      currentSnapshot = snap;
      renderCurrentTab();
    } catch (e) {
      log('[스냅샷] 조회 실패: ' + e.message);
    }
  }

  window._selectConv = loadSnapshot;

  window._deleteConv = async (convId) => {
    if (!confirm(`대화 "${convId}"를 삭제할까요?\n메시지, 요약, 노트가 모두 삭제됩니다.`)) return;
    try {
      await fetchJson(`/api/memory/conversations/${encodeURIComponent(convId)}`, { method: 'DELETE' });
      log(`[삭제] ${convId} 삭제 완료`);
      if (currentConvId === convId) {
        currentConvId = null;
        currentSnapshot = null;
        clearDetail();
      }
      await loadConversations();
      await loadSummary();
    } catch (e) {
      log('[삭제] 실패: ' + e.message);
    }
  };

  async function deleteAll() {
    const ids = await fetchJson('/api/memory/conversations').catch(() => []);
    if (!ids || !ids.length) { log('[전체삭제] 삭제할 대화 없음'); return; }
    if (!confirm(`전체 ${ids.length}개 대화를 삭제할까요?`)) return;
    let count = 0;
    for (const id of ids) {
      try {
        await fetchJson(`/api/memory/conversations/${encodeURIComponent(id)}`, { method: 'DELETE' });
        count++;
      } catch { /* 무시 */ }
    }
    log(`[전체삭제] ${count}개 삭제 완료`);
    currentConvId = null; currentSnapshot = null;
    clearDetail();
    await loadConversations();
    await loadSummary();
  }

  // ── 탭 전환 ────────────────────────────────────────────
  window.switchTab = (tab) => {
    currentTab = tab;
    document.querySelectorAll('.mem-tab').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tab') === tab);
    });
    document.querySelectorAll('.mem-tab-content').forEach(el => {
      el.style.display = 'none';
    });
    const target = qs('tab-' + tab);
    if (target) target.style.display = '';
    renderCurrentTab();
  };

  function renderCurrentTab() {
    if (!currentSnapshot) return;
    switch (currentTab) {
      case 'messages':  renderMessages(currentSnapshot.recentMessages || []); break;
      case 'summaries': renderSummaries(currentSnapshot.categorySummaries || {}); break;
      case 'notes':     renderNotes(currentSnapshot.importantNotes || []); break;
      case 'raw':       renderRaw(currentSnapshot); break;
    }
  }

  function clearDetail() {
    setText('detailConvId', '');
    qs('messageList').innerHTML  = '<div class="mem-empty">대화를 선택하면 메시지가 표시됩니다.</div>';
    qs('summaryList').textContent = '대화를 선택하면 카테고리 요약이 표시됩니다.';
    qs('noteList').innerHTML     = '<div class="mem-empty">대화를 선택하면 중요 노트가 표시됩니다.</div>';
    qs('rawPanel').textContent   = '대화를 선택하면 전체 스냅샷이 표시됩니다.';
  }

  // ── 렌더러 ─────────────────────────────────────────────
  function renderMessages(messages) {
    const el = qs('messageList');
    if (!messages.length) {
      el.innerHTML = '<div class="mem-empty">메시지가 없습니다.</div>';
      return;
    }
    el.innerHTML = messages.map(m => {
      const role = m.role || 'unknown';
      const cat  = m.category || '';
      const time = (m.createdAt || '').toString().substring(0, 19).replace('T', ' ');
      const body = htmlEscape(m.content || '');
      return `<div class="mem-msg ${htmlEscape(role)}">
        <div class="mem-msg-header">
          <span class="mem-role-badge ${htmlEscape(role)}">${htmlEscape(role.toUpperCase())}</span>
          ${cat ? `<span style="color:#475569">${htmlEscape(cat)}</span>` : ''}
          ${time ? `<span style="margin-left:auto">${time}</span>` : ''}
        </div>
        <div class="mem-msg-body">${body}</div>
      </div>`;
    }).join('');
    el.scrollTop = 0;
  }

  function renderSummaries(summaries) {
    const el = qs('summaryList');
    const entries = Object.entries(summaries);
    if (!entries.length) {
      el.textContent = '카테고리 요약이 없습니다.';
      return;
    }
    el.textContent = entries.map(([cat, s]) => {
      const time = (s.updatedAt || '').toString().substring(0, 19).replace('T', ' ');
      return `[${cat}] ${time ? `(${time})` : ''}\n${s.summary || '(내용 없음)'}`;
    }).join('\n\n─────────────────────────────────\n\n');
  }

  function renderNotes(notes) {
    const el = qs('noteList');
    if (!notes.length) {
      el.innerHTML = '<div class="mem-empty">중요 노트가 없습니다.</div>';
      return;
    }
    el.innerHTML = notes.map(n => {
      const cat  = n.category || '';
      const time = (n.createdAt || '').toString().substring(0, 19).replace('T', ' ');
      return `<div class="mem-note">
        <div class="mem-note-header">${htmlEscape(cat)} ${time ? `| ${time}` : ''}</div>
        <div>${htmlEscape(n.note || '')}</div>
      </div>`;
    }).join('');
    el.scrollTop = 0;
  }

  function renderRaw(snapshot) {
    qs('rawPanel').textContent = pretty(snapshot);
  }

  // ── 초기화 ─────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnRefreshSummary')?.addEventListener('click', async () => {
      await loadSummary();
      await loadConversations();
    });
    qs('btnLoadConversations')?.addEventListener('click', loadConversations);
    qs('btnDeleteAll')?.addEventListener('click', deleteAll);

    // 초기 로드
    loadSummary();
    loadConversations();
  });
})();
