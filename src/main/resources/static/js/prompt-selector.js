/**
 * prompt-selector.js — 채팅 페이지 공통 프롬프트 선택 모듈
 * 각 채팅 페이지에서 window.PromptSelector.init(selectId) 로 활성화합니다.
 */
window.PromptSelector = (() => {
  const { fetchJson, htmlEscape } = window.UiCommon;

  /**
   * selectId: 프롬프트 선택 <select> 요소의 id
   * category: ChatCategory 문자열 (없으면 전체 조회)
   */
  async function init(selectId, category) {
    const sel = document.getElementById(selectId);
    if (!sel) return;

    try {
      const url = category
          ? `/api/prompt-entries?category=${encodeURIComponent(category)}`
          : '/api/prompt-entries';
      const list = await fetchJson(url);

      const active = (list || []).filter(p => p.active !== false);
      const defaultItem = active.find(p => p.default);

      sel.innerHTML =
          '<option value="">🔧 기본 프롬프트</option>' +
          active.map(p =>
              `<option value="${htmlEscape(p.id)}" ${p.id === defaultItem?.id ? 'selected' : ''}>` +
              `${p.default ? '⭐ ' : ''}${htmlEscape(p.name)}` +
              `${p.category ? ` [${p.category}]` : ''}` +
              `</option>`
          ).join('');

      // 기본 프롬프트가 있으면 자동 선택
      if (defaultItem) sel.value = defaultItem.id;
    } catch {
      sel.innerHTML = '<option value="">기본 프롬프트 (조회 실패)</option>';
    }
  }

  /** 현재 선택된 promptId 반환 */
  function selected(selectId) {
    return document.getElementById(selectId)?.value || '';
  }

  return { init, selected };
})();
