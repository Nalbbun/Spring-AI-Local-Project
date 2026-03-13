window.UiCommon = (() => {
  function qs(id){return document.getElementById(id);} 
  function val(id){const el=qs(id);return el && el.value != null ? el.value.trim() : '';} 
  function rawVal(id){const el=qs(id);return el && el.value != null ? el.value : '';} 
  function setVal(id,v){const el=qs(id); if(el) el.value = v || '';}
  function setText(id,v){const el=qs(id); if(el) el.textContent = v == null ? '' : String(v);} 
  function pretty(v){return JSON.stringify(v,null,2);} 
  async function fetchJson(url,opt={}){
    const res = await fetch(url,opt);
    const text = await res.text();
    let json = null;
    try { json = text ? JSON.parse(text) : {}; } catch { json = { raw:text }; }
    if(!res.ok){
      const msg = json && (json.message || json.error || json.raw) ? (json.message || json.error || json.raw) : text;
      throw new Error(`HTTP ${res.status}: ${msg}`);
    }
    return json;
  }
  function parseMetadata(text){
    const raw = (text || '').trim();
    if(!raw) return {};
    const parsed = JSON.parse(raw);
    if(!parsed || Array.isArray(parsed) || typeof parsed !== 'object') throw new Error('JSON 객체 형식이어야 합니다.');
    return parsed;
  }
  function ensure(value, message){ if(!value) throw new Error(message); return value; }
  function renderJson(id, data){ setText(id, pretty(data)); }
  function htmlEscape(v){ return String(v ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;'); }
  return { qs, val, rawVal, setVal, setText, pretty, fetchJson, parseMetadata, ensure, renderJson, htmlEscape };
})();
