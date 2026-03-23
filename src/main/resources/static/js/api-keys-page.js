/**
 * api-keys-page.js — API 키 관리 페이지
 * 키 값은 서버에서 마스킹 처리됨. 👁 버튼 클릭 시에만 /reveal 호출로 복호화 표시.
 */
(() => {
  const { qs, val, setVal, setText, fetchJson, htmlEscape } = window.UiCommon;

  let allKeys     = [];
  let currentId   = null;
  let currentProv = '';
  let keyVisible  = false;

  // 프로바이더별 키 발급 URL
  const ISSUE_URLS = {
    OPENAI:    { url: 'https://platform.openai.com/api-keys',   hint: 'OpenAI Platform → API Keys에서 발급하세요.' },
    TAVILY:    { url: 'https://app.tavily.com',                 hint: 'Tavily 대시보드에서 API Key를 발급하세요.' },
    ANTHROPIC: { url: 'https://console.anthropic.com',          hint: 'Anthropic Console → API Keys (향후 지원 예정).' },
    CUSTOM:    { url: null,                                      hint: '사용할 외부 API의 키를 입력하세요.' },
  };

  function log(msg) {
    const el = qs('eventLog');
    if (!el) return;
    el.textContent += msg + '\n';
    el.scrollTop = el.scrollHeight;
  }

  // ── 프로바이더 현황 ───────────────────────────────────────
  async function loadProviders() {
    try {
      const list = await fetchJson('/api/api-keys/providers');
      const grid = qs('providerGrid');
      if (!grid) return;

      grid.innerHTML = list.map(p => {
        const hasKey = p.hasActiveKey;
        const color  = hasKey ? '#6ee7b7' : '#f87171';
        const badge  = hasKey
            ? `<span style="color:${color}">✅ 활성 — <code style="font-size:12px">${htmlEscape(p.maskedKey || '')}</code></span>`
            : `<span style="color:${color}">⚠ 미설정</span>`;
        const issueUrl = p.keyIssueUrl
            ? `<a href="${htmlEscape(p.keyIssueUrl)}" target="_blank" style="font-size:12px;color:#93c5fd">🔗 키 발급</a>`
            : '';
        return `<div style="background:#0b1220;border:1px solid ${hasKey ? '#14532d' : '#7f1d1d'};border-radius:10px;padding:12px 16px">
          <div style="font-weight:700;font-size:14px;color:#e2e8f0;margin-bottom:4px">${htmlEscape(p.displayName)}</div>
          <div style="font-size:12px;color:#64748b;margin-bottom:6px">${htmlEscape(p.description)}</div>
          <div style="display:flex;justify-content:space-between;align-items:center">
            ${badge}
            ${issueUrl}
          </div>
        </div>`;
      }).join('');

      // 헤더 칩 업데이트
      const openai  = list.find(p => p.provider === 'OPENAI');
      const tavily  = list.find(p => p.provider === 'TAVILY');
      if (openai)  setText('chipOpenai', `OpenAI: ${openai.hasActiveKey  ? '✅' : '⚠'}`);
      if (tavily)  setText('chipTavily', `Tavily: ${tavily.hasActiveKey  ? '✅' : '⚠'}`);
    } catch (e) { log('[프로바이더] 조회 실패: ' + e.message); }
  }

  // ── 목록 ─────────────────────────────────────────────────
  async function loadList(prov) {
    currentProv = prov ?? currentProv;
    try {
      const url = currentProv
          ? `/api/api-keys?provider=${encodeURIComponent(currentProv)}`
          : '/api/api-keys';
      allKeys = await fetchJson(url);
      renderTable(allKeys);
      setText('listStatus', `${allKeys.length}개 조회됨`);
    } catch (e) { log('[목록] 조회 실패: ' + e.message); }
  }

  function renderTable(list) {
    const tbody = qs('keyTable');
    if (!list || !list.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:#64748b">등록된 API 키가 없습니다.</td></tr>';
      return;
    }
    tbody.innerHTML = list.map(k => {
      const activeLabel = k.active
          ? '<span style="color:#6ee7b7;font-size:12px">●&nbsp;활성</span>'
          : '<span style="color:#64748b;font-size:12px">○&nbsp;비활성</span>';
      const sel = k.id === currentId ? 'style="background:#1e3a5f"' : '';
      return `<tr id="krow-${htmlEscape(k.id)}" ${sel}>
        <td style="font-size:13px">
          <a href="#" style="color:#e2e8f0;text-decoration:none"
             onclick="window._selectKey('${htmlEscape(k.id)}');return false">${htmlEscape(k.label)}</a>
          ${k.description ? `<div style="font-size:11px;color:#64748b;margin-top:2px">${htmlEscape(k.description)}</div>` : ''}
        </td>
        <td><span style="font-size:12px;background:#1e293b;padding:2px 7px;border-radius:5px;color:#94a3b8">${htmlEscape(k.provider)}</span></td>
        <td>
          <code style="font-size:12px;color:#94a3b8">${htmlEscape(k.maskedKey)}</code>
          <button onclick="window._revealKey('${htmlEscape(k.id)}')" type="button"
            style="margin-left:6px;padding:2px 7px;font-size:11px;background:#1e293b;border:1px solid #334155;border-radius:5px;color:#93c5fd;cursor:pointer">👁</button>
        </td>
        <td>${activeLabel}</td>
        <td>
          <div style="display:flex;gap:4px">
            <button class="primary" style="width:auto;padding:3px 7px;font-size:12px"
              onclick="window._selectKey('${htmlEscape(k.id)}')">✏️</button>
            <button class="green" style="width:auto;padding:3px 7px;font-size:12px"
              onclick="window._activateKey('${htmlEscape(k.id)}')">⚡</button>
            <button class="red" style="width:auto;padding:3px 7px;font-size:12px"
              onclick="window._deleteKey('${htmlEscape(k.id)}')">🗑</button>
          </div>
        </td>
      </tr>`;
    }).join('');
  }

  // ── 키 값 표시 (👁 버튼) ──────────────────────────────────
  window._revealKey = async (id) => {
    try {
      const r    = await fetchJson(`/api/api-keys/${encodeURIComponent(id)}/reveal`);
      const krow = qs('krow-' + id);
      if (!krow) return;
      // 기존 reveal-box 제거
      krow.querySelectorAll('.key-reveal-inline').forEach(el => el.remove());
      const td = krow.querySelector('td:nth-child(3)');
      const box = document.createElement('div');
      box.className = 'key-reveal-inline key-reveal-box';
      box.textContent = r.keyValue;
      box.style.cursor = 'pointer';
      box.title = '클릭하면 숨김';
      box.onclick = () => box.remove();
      td.appendChild(box);
      log(`[뷰] ${id} 키 복호화 표시 (클릭하면 숨김)`);
    } catch (e) { log('[뷰] 복호화 실패: ' + e.message); }
  };

  // ── 폼 토글 (👁 버튼) ────────────────────────────────────
  window._toggleKeyVisibility = () => {
    const inp = qs('fKeyValue');
    if (!inp) return;
    keyVisible = !keyVisible;
    inp.type   = keyVisible ? 'text' : 'password';
    const btn  = qs('btnToggleKey');
    if (btn) btn.textContent = keyVisible ? '🙈' : '👁';
  };

  // ── 프로바이더 변경 → 키 발급 힌트 표시 ──────────────────
  function onProviderChange() {
    const prov = val('fProvider');
    const hint = qs('keyIssueHint');
    if (!hint) return;
    const info = ISSUE_URLS[prov];
    if (info) {
      hint.style.display = 'block';
      hint.innerHTML = info.url
          ? `💡 ${htmlEscape(info.hint)} <a href="${info.url}" target="_blank" style="color:#93c5fd">🔗 발급 페이지 바로가기</a>`
          : `💡 ${htmlEscape(info.hint)}`;
    } else {
      hint.style.display = 'none';
    }
  }

  // 키 강도 표시
  function showKeyStrength(val) {
    const el = qs('keyStrength');
    if (!el || !val) { if (el) el.textContent = ''; return; }
    if (val.length < 20) el.textContent = '⚠ 키가 너무 짧습니다 (일반적으로 40자 이상)';
    else if (val.startsWith('sk-'))   el.textContent = '✅ OpenAI 키 형식';
    else if (val.startsWith('tvly-')) el.textContent = '✅ Tavily 키 형식';
    else el.textContent = `✅ 길이 ${val.length}자`;
    el.style.color = val.length < 20 ? '#f59e0b' : '#6ee7b7';
  }

  // ── 폼 조작 ──────────────────────────────────────────────
  function clearForm() {
    currentId = null;
    keyVisible = false;
    setText('formTitle', '🔑 API 키 등록');
    setVal('fProvider', '');
    setVal('fLabel', '');
    setVal('fDesc', '');
    const kv = qs('fKeyValue');
    if (kv) { kv.value = ''; kv.type = 'password'; }
    const btn = qs('btnToggleKey');
    if (btn) btn.textContent = '👁';
    if (qs('fActive'))    qs('fActive').checked = true;
    if (qs('keyIssueHint')) qs('keyIssueHint').style.display = 'none';
    if (qs('keyStrength')) qs('keyStrength').textContent = '';
    setText('formStatus', 'API 키를 등록하세요.');
    const del = qs('btnDelete');
    if (del) del.style.display = 'none';
    document.querySelectorAll('#keyTable tr').forEach(r => r.removeAttribute('style'));
  }

  window._selectKey = (id) => {
    const k = allKeys.find(x => x.id === id);
    if (!k) return;
    currentId = id;
    setText('formTitle', `✏️ 편집: ${k.label}`);
    setVal('fProvider', k.provider || '');
    setVal('fLabel', k.label || '');
    setVal('fDesc', k.description || '');
    const kv = qs('fKeyValue');
    if (kv) { kv.value = ''; kv.type = 'password'; kv.placeholder = '변경 시에만 입력 (비우면 기존 키 유지)'; }
    if (qs('fActive')) qs('fActive').checked = k.active !== false;
    setText('formStatus',
        `ID: ${k.id} | 프로바이더: ${k.provider} | 마스킹: ${k.maskedKey}\n생성: ${(k.createdAt||'').toString().substring(0,19).replace('T',' ')}`);
    const del = qs('btnDelete');
    if (del) del.style.display = '';
    document.querySelectorAll('#keyTable tr').forEach(r => r.removeAttribute('style'));
    const row = qs('krow-' + id);
    if (row) row.setAttribute('style', 'background:#1e3a5f');
    onProviderChange();
  };

  async function saveKey() {
    const provider  = val('fProvider');
    const label     = val('fLabel').trim();
    const desc      = val('fDesc').trim();
    const keyValue  = qs('fKeyValue')?.value?.trim() || '';
    const active    = qs('fActive')?.checked ?? true;

    if (!provider) { setText('formStatus', '⚠ 프로바이더를 선택하세요.'); return; }
    if (!label)    { setText('formStatus', '⚠ 레이블을 입력하세요.'); return; }
    if (!currentId && !keyValue) { setText('formStatus', '⚠ API 키 값을 입력하세요.'); return; }

    setText('formStatus', '저장 중...');
    try {
      const body = { provider, label, description: desc, active };
      if (keyValue) body.keyValue = keyValue;

      let result;
      if (currentId) {
        result = await fetchJson(`/api/api-keys/${encodeURIComponent(currentId)}`, {
          method: 'PUT', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        });
        log(`[수정] ${result.label} (${result.provider})`);
      } else {
        body.keyValue = keyValue;
        result = await fetchJson('/api/api-keys', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        });
        log(`[등록] ${result.label} (${result.provider}) → 마스킹: ${result.maskedKey}`);
      }
      setText('formStatus', `✅ 저장 완료 — ${result.label} | ${result.maskedKey}${active ? ' | 런타임 적용됨' : ''}`);
      currentId = result.id;
      await loadList();
      await loadProviders();
    } catch (e) {
      setText('formStatus', '저장 실패: ' + e.message);
      log('[저장] 실패: ' + e.message);
    }
  }

  window._deleteKey = async (id) => {
    const k = allKeys.find(x => x.id === id);
    if (!confirm(`"${k?.label || id}" 키를 삭제할까요?`)) return;
    try {
      await fetchJson(`/api/api-keys/${encodeURIComponent(id)}`, { method: 'DELETE' });
      log(`[삭제] ${id}`);
      if (currentId === id) clearForm();
      await loadList();
      await loadProviders();
    } catch (e) { log('[삭제] 실패: ' + e.message); }
  };

  window._activateKey = async (id) => {
    try {
      const r = await fetchJson(`/api/api-keys/${encodeURIComponent(id)}/activate`, { method: 'POST' });
      log(`[활성화] ${r.label} → 런타임 적용 완료`);
      await loadList();
      await loadProviders();
    } catch (e) { log('[활성화] 실패: ' + e.message); }
  };

  window._filterProv = (btn, prov) => {
    document.querySelectorAll('.mem-tab[data-prov]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    loadList(prov);
  };

  // ── 초기화 ────────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', () => {
    qs('btnRefresh')?.addEventListener('click', async () => { await loadProviders(); await loadList(); });
    qs('btnNew')?.addEventListener('click', clearForm);
    qs('btnSave')?.addEventListener('click', saveKey);
    qs('btnCancel')?.addEventListener('click', clearForm);
    qs('btnDelete')?.addEventListener('click', () => currentId && window._deleteKey(currentId));
    qs('fProvider')?.addEventListener('change', onProviderChange);
    qs('fKeyValue')?.addEventListener('input', e => showKeyStrength(e.target.value));

    loadProviders();
    loadList();
    clearForm();
  });
})();
