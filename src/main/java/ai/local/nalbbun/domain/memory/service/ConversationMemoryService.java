package ai.local.nalbbun.domain.memory.service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.model.MemoryMessage;

import java.util.List;

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

    /**
     * 저장소에서 알려진 conversationId 목록을 반환합니다.
     * InMemory: 현재 메모리에 있는 ID, JDBC: DB에서 조회, Redis: SCAN
     */
    List<String> listConversationIds();
}