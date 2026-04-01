import { useEffect, useMemo, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { runtimeApi } from '../../services/settingsApi';
import type { RuntimeMeta } from '../../types/api';

const navGroups = [
  {
    title: '채팅',
    requiresAdmin: false,
    items: [
      { to: '/chat/general', label: '일반 채팅' },
      { to: '/chat/rag', label: 'RAG 채팅' },
      { to: '/chat/agent', label: '에이전트 채팅' }
    ]
  },
  {
    title: '환경설정',
    requiresAdmin: true,
    items: [
      { to: '/operations/system', label: '시스템 설정' },
      { to: '/operations/models', label: '모델 관리' },
      { to: '/operations/keys', label: '키 관리' },
      { to: '/operations/prompts', label: '프롬프트 관리' },
      { to: '/operations/api-catalog', label: 'API 목록' }
    ]
  },
  {
    title: 'RAG 설정',
    requiresAdmin: true,
    items: [
      { to: '/knowledge/rag-documents', label: 'RAG 문서 관리' },
      { to: '/knowledge/rag-search-test', label: '검색 테스트' }
    ]
  },
  {
    title: '에이전트 설정',
    requiresAdmin: true,
    items: [
      { to: '/agent/management', label: '에이전트 관리' },
      { to: '/agent/trace', label: '실행 추적' }
    ]
  },
  {
    title: '대화 관리',
    requiresAdmin: false,
    items: [{ to: '/conversation/list', label: '대화 목록' }]
  }
] as const;

const SIDEBAR_STORAGE_KEY = 'nalbbun.sidebar.collapsed';

export function MainLayout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [runtimeMeta, setRuntimeMeta] = useState<RuntimeMeta | null>(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const saved = window.localStorage.getItem(SIDEBAR_STORAGE_KEY);
    setSidebarCollapsed(saved === 'true');
  }, []);

  useEffect(() => {
    runtimeApi.getMeta()
      .then(setRuntimeMeta)
      .catch(() => setRuntimeMeta({ adminConsoleEnabled: false, debugEnabled: false }));
  }, []);

  const visibleNavGroups = useMemo(() => {
    const adminEnabled = Boolean(runtimeMeta?.adminConsoleEnabled);
    return navGroups.filter((group) => !group.requiresAdmin || adminEnabled);
  }, [runtimeMeta]);

  const toggleSidebar = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(SIDEBAR_STORAGE_KEY, String(next));
      }
      return next;
    });
  };

  return (
    <div className={`app-shell ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      <aside className={`sidebar ${sidebarCollapsed ? 'collapsed' : ''}`}>
        <div className="brand-block">
          <div className="brand-row">
            <div>
              <div className="brand-title">Nalbbun AI</div>
              <div className="brand-subtitle">Web Console</div>
            </div>
            <button type="button" className="sidebar-toggle" onClick={toggleSidebar} aria-label={sidebarCollapsed ? '메뉴 열기' : '메뉴 닫기'}>
              {sidebarCollapsed ? '»' : '«'}
            </button>
          </div>
        </div>
        {!sidebarCollapsed && visibleNavGroups.map(group => (
          <div className="nav-group" key={group.title}>
            <div className="nav-group-title">{group.title}</div>
            {group.items.map(item => (
              <NavLink key={item.to} to={item.to} className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}
      </aside>
      <div className="content-area">
        <header className="content-header">
          <div className="content-header-row">
            <div className="header-left-group">
              {sidebarCollapsed && (
                <button type="button" className="sidebar-toggle header-toggle" onClick={toggleSidebar} aria-label="메뉴 열기">
                  ≡
                </button>
              )}
              <div>
                <h1>Spring AI + React + vite 이용한 AI Test Tool</h1>
                {runtimeMeta && !runtimeMeta.adminConsoleEnabled && (
                  <div className="content-header-note">운영 모드에서는 debug/admin 메뉴가 숨김 처리됩니다.</div>
                )}
              </div>
            </div>
          </div>
        </header>
        <main className="content-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
