import { NavLink, Outlet } from 'react-router-dom';

const navGroups = [
  {
    title: '채팅',
    items: [
      { to: '/chat/general', label: '일반 채팅' },
      { to: '/chat/rag', label: 'RAG 채팅' },
      { to: '/chat/agent', label: '에이전트 채팅' }
    ]
  },
  {
    title: '환경설정',
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
    items: [
      { to: '/knowledge/rag-documents', label: 'RAG 문서 관리' },
      { to: '/knowledge/rag-search-test', label: '검색 테스트' }
    ]
  },
  {
    title: '에이전트 설정',
    items: [
      { to: '/agent/management', label: '에이전트 관리' },
      { to: '/agent/trace', label: '실행 추적' }
    ]
  },
  {
    title: '대화 관리',
    items: [{ to: '/conversation/list', label: '대화 목록' }]
  }
];

export function MainLayout() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-title">Nalbbun AI</div>
          <div className="brand-subtitle">Web Console</div>
        </div>
        {navGroups.map(group => (
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
          <div>
            <h1>Spring AI + React + vite 이용한 AI Test Tool</h1>
           </div>
        </header>
        <main className="content-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
