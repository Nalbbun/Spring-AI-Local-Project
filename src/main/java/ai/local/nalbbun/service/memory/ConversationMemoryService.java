package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.MemoryMessage;

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
}