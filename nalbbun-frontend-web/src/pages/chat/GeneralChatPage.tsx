import { ChatWorkspace } from '../../components/chat/ChatWorkspace';

export function GeneralChatPage() {
  return <ChatWorkspace title="일반 채팅" description="legacy index/chat 페이지의 일반 대화 기능을 React 스트리밍 화면으로 옮긴 버전입니다." defaultCategory="GENERAL" defaultMessage="안녕하세요. 일반 채팅 테스트입니다." />;
}
