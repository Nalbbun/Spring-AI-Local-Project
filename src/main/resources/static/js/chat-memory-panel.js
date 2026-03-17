/**
 * chat-memory-panel.js
 * 채팅 페이지의 인라인 메모리 패널 공통 모듈.
 * 각 채팅 JS에서 initMemoryPanel() 을 호출하여 활성화합니다.
 */
window.ChatMemoryPanel = (() => {
  const { qs, fetchJson, pretty, htmlEscape } = window.UiCommon;

  function init({ showBtnId = 'btnShowMemory', clearBtnId = 'btnClearMemory', panelId = 'memoryPanel' } = {}) {
    const showBtn  = qs(showBtnId);
    const clearBtn = qs(clearBtnId);
    if (showBtn)  showBtn.addEventListener('click',  () => loadAndRender(panelId));
    if (clearBtn) clearBtn.addEventListener('click', () => clearMemory(panelId));
  }

  async function loadAndRender(panelId) {
    const panel = qs(panelId);
    if (!panel) return;
    panel.innerHTML = '<div class="mem-inline-empty">조회 중...</div>';
    try {
      const snap = await fetchJson('/debug/api/memory');
      renderSnapshot(panel, snap);
    } catch (e) {
      panel.innerHTML = `<div class="mem-inline-empty" style="color:#f87171">조회 실패: ${htmlEscape(e.message)}</div>`;
    }
  }

  async function clearMemory(panelId) {
    const panel = qs(panelId);
    try {
      await fetchJson('/debug/api/memory/clear', { method: 'POST' });
      if (panel) panel.innerHTML = '<div class="mem-inline-empty">대화 메모리가 초기화되었습니다.</div>';
    } catch (e) {
      if (panel) panel.innerHTML = `<div class="mem-inline-empty" style="color:#f87171">초기화 실패: ${htmlEscape(e.message)}</div>`;
    }
  }

  function renderSnapshot(panel, snap) {
    if (!snap) { panel.innerHTML = '<div class="mem-inline-empty">데이터 없음</div>'; return; }

    const convId   = snap.conversationId || '-';
    const messages = snap.recentMessages || [];
    const summaries = snap.categorySummaries || {};
    const notes    = snap.importantNotes || [];

    const msgCount  = messages.length;
    const noteCount = notes.length;
    const sumCount  = Object.keys(summaries).length;

    // 통계 바
    let html = `
      <div class="mem-inline-header">
        <span class="mem-inline-stat">🆔 ${htmlEscape(convId)}</span>
        <span class="mem-inline-stat">💬 메시지 ${msgCount}개</span>
        <span class="mem-inline-stat">📋 요약 ${sumCount}개</span>
        <span class="mem-inline-stat">📌 노트 ${noteCount}개</span>
        <a href="/memory" style="margin-left:auto;font-size:12px;color:#93c5fd">📋 전체 관리 →</a>
      </div>`;

    // 최근 메시지 (최대 5개)
    if (messages.length) {
      const recent = messages.slice(-5);
      html += `<div class="mem-inline-section">
        <div class="mem-inline-section-title">최근 메시지 (최근 ${recent.length}개)</div>
        <div class="mem-inline-msgs">`;
      html += recent.map(m => {
        const role = m.role || 'unknown';
        const cat  = m.category ? ` · ${m.category}` : '';
        const time = (m.createdAt || '').toString().substring(0, 16).replace('T', ' ');
        const body = htmlEscape((m.content || '').substring(0, 200)) + (m.content?.length > 200 ? '...' : '');
        return `<div class="mem-inline-msg ${htmlEscape(role)}">
          <div class="mem-inline-msg-meta">
            <span class="mem-role-badge ${htmlEscape(role)}">${htmlEscape(role.toUpperCase())}</span>
            <span style="color:#475569">${htmlEscape(cat)}</span>
            <span style="margin-left:auto;color:#475569">${time}</span>
          </div>
          <div class="mem-inline-msg-body">${body}</div>
        </div>`;
      }).join('');
      html += `</div></div>`;
    }

    // 카테고리 요약
    const sumEntries = Object.entries(summaries);
    if (sumEntries.length) {
      html += `<div class="mem-inline-section">
        <div class="mem-inline-section-title">카테고리 요약</div>`;
      html += sumEntries.map(([cat, s]) => {
        const text = htmlEscape((s.summary || '').substring(0, 300)) + (s.summary?.length > 300 ? '...' : '');
        return `<div class="mem-inline-summary">
          <span class="mem-inline-cat-badge">${htmlEscape(cat)}</span>
          <span>${text}</span>
        </div>`;
      }).join('');
      html += `</div>`;
    }

    // 중요 노트 (최대 3개)
    if (notes.length) {
      const recentNotes = notes.slice(-3);
      html += `<div class="mem-inline-section">
        <div class="mem-inline-section-title">중요 노트 (최근 ${recentNotes.length}개)</div>`;
      html += recentNotes.map(n => {
        const cat  = n.category || '';
        const text = htmlEscape((n.note || '').substring(0, 200)) + (n.note?.length > 200 ? '...' : '');
        return `<div class="mem-note" style="margin-bottom:6px">
          <div class="mem-note-header">${htmlEscape(cat)}</div>
          <div>${text}</div>
        </div>`;
      }).join('');
      html += `</div>`;
    }

    panel.innerHTML = html;
  }

  return { init, loadAndRender, clearMemory };
})();
