package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryConversationMemoryServiceTest {

    private final InMemoryConversationMemoryService memoryService = new InMemoryConversationMemoryService();

    @Test
    void shouldKeepOnlyLatestMessagesAndNotesWithinConfiguredLimit() {
        String conversationId = "conv-in-memory-001";

        for (int i = 1; i <= 55; i++) {
            memoryService.addUserMessage(conversationId, ChatCategory.DEV, "user-message-" + i);
        }
        for (int i = 1; i <= 25; i++) {
            memoryService.addImportantNote(conversationId, ChatCategory.DEV, "important-note-" + i);
        }

        List<String> notes = memoryService.getImportantNotes(conversationId, ChatCategory.DEV);
        List<?> messages = memoryService.recentMessages(conversationId, 100);

        assertEquals(50, messages.size());
        assertEquals(20, notes.size());
        assertTrue(memoryService.formatRecentConversation(conversationId, 3).contains("user-message-55"));
        assertEquals("important-note-6", notes.get(0));
        assertEquals("important-note-25", notes.get(notes.size() - 1));
    }

    @Test
    void shouldCreateSnapshotWithSummariesNotesAndMessages() {
        String conversationId = "conv-in-memory-002";

        memoryService.addUserMessage(conversationId, ChatCategory.TRAVEL, "부산 여행 일정 짜줘");
        memoryService.addAssistantMessage(conversationId, ChatCategory.TRAVEL, "2박 3일 일정을 제안합니다.");
        memoryService.updateCategorySummary(conversationId, ChatCategory.TRAVEL, "부산 2박 3일 여행 계획 논의");
        memoryService.addImportantNote(conversationId, ChatCategory.TRAVEL, "예산 상한 80만원");

        ConversationMemorySnapshot snapshot = memoryService.snapshot(conversationId);

        assertEquals(conversationId, snapshot.getConversationId());
        assertEquals(2, snapshot.getRecentMessages().size());
        assertEquals("부산 2박 3일 여행 계획 논의",
                snapshot.getCategorySummaries().get(ChatCategory.TRAVEL.name()).getSummary());
        assertEquals(1, snapshot.getImportantNotes().size());
        assertEquals("예산 상한 80만원", snapshot.getImportantNotes().get(0).getNote());
    }
}
