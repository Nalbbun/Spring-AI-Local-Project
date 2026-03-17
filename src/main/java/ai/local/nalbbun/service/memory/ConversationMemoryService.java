package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.MemoryMessage;

import java.util.List;

/**
 * Conversation Memory Service 인터페이스이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 구현체가 따라야 할 계약을 정의한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
public interface ConversationMemoryService {

    void addUserMessage(String conversationId, ChatCategory category, String content);

    void addAssistantMessage(String conversationId, ChatCategory category, String content);

    void addSystemMessage(String conversationId, ChatCategory category, String content);

    List<MemoryMessage> recentMessages(String conversationId, int limit);

    String formatRecentConversation(String conversationId, int limit);

    void updateCategorySummary(String conversationId, ChatCategory category, String summary);

    String getCategorySummary(String conversationId, ChatCategory category);

    void addImportantNote(String conversationId, ChatCategory category, String note);

    List<String> getImportantNotes(String conversationId, ChatCategory category);

    ConversationMemorySnapshot snapshot(String conversationId);

    void clear(String conversationId);
}