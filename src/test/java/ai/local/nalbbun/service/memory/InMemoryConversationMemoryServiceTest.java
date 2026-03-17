package ai.local.nalbbun.service.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.memory.service.InMemoryConversationMemoryService;

/**
 * In Memory Conversation Memory Service Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class InMemoryConversationMemoryServiceTest {

    private final InMemoryConversationMemoryService memoryService = new InMemoryConversationMemoryService();

    /**
     * Keep Only Latest Messages And Notes Within Configured Limit 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Create Snapshot With Summaries Notes And Messages 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
