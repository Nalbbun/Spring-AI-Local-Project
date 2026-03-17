package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InMemoryConversationMemoryServiceTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: in memory conversation memory service test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class InMemoryConversationMemoryServiceTest {

    /** memoryService 값을 보관한다. */
    private final InMemoryConversationMemoryService memoryService = new InMemoryConversationMemoryService();

    /**
     * 대상 기능의 동작을 검증한다.
     */
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

    /**
     * 대상 기능의 동작을 검증한다.
     */
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
