import { createBrowserRouter, Navigate } from 'react-router-dom';
import { MainLayout } from './layouts/MainLayout';
import { GeneralChatPage } from '../pages/chat/GeneralChatPage';
import { RagChatPage } from '../pages/chat/RagChatPage';
import { AgentChatPage } from '../pages/chat/AgentChatPage';
import { SystemSettingsPage } from '../pages/operations/SystemSettingsPage';
import { ModelManagementPage } from '../pages/operations/ModelManagementPage';
import { KeyManagementPage } from '../pages/operations/KeyManagementPage';
import { PromptManagementPage } from '../pages/operations/PromptManagementPage';
import { DeviceManagementPage } from '../pages/operations/DeviceManagementPage';
import { ApiCatalogPage } from '../pages/operations/ApiCatalogPage';
import { RagDocumentsPage } from '../pages/knowledge/RagDocumentsPage';
import { RagSearchTestPage } from '../pages/knowledge/RagSearchTestPage';
import { AgentManagementPage } from '../pages/agent/AgentManagementPage';
import { AgentTracePage } from '../pages/agent/AgentTracePage';
import { ConversationListPage } from '../pages/conversation/ConversationListPage';
import { ConversationDetailPage } from '../pages/conversation/ConversationDetailPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <Navigate to="/chat/general" replace /> },
      { path: 'chat/general', element: <GeneralChatPage /> },
      { path: 'chat/rag', element: <RagChatPage /> },
      { path: 'chat/agent', element: <AgentChatPage /> },
      { path: 'operations/system', element: <SystemSettingsPage /> },
      { path: 'operations/models', element: <ModelManagementPage /> },
      { path: 'operations/keys', element: <KeyManagementPage /> },
      { path: 'operations/prompts', element: <PromptManagementPage /> },
      { path: 'operations/devices', element: <DeviceManagementPage /> },
      { path: 'operations/api-catalog', element: <ApiCatalogPage /> },
      { path: 'knowledge/rag-documents', element: <RagDocumentsPage /> },
      { path: 'knowledge/rag-search-test', element: <RagSearchTestPage /> },
      { path: 'agent/management', element: <AgentManagementPage /> },
      { path: 'agent/trace', element: <AgentTracePage /> },
      { path: 'conversation/list', element: <ConversationListPage /> },
      { path: 'conversation/:conversationId', element: <ConversationDetailPage /> }
    ]
  }
]);
