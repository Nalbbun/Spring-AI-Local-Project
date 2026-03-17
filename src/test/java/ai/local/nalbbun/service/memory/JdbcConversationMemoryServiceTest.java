package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JdbcConversationMemoryServiceTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: jdbc conversation memory service test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class JdbcConversationMemoryServiceTest {

    /** memoryService 값을 보관한다. */
    private JdbcConversationMemoryService memoryService;

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @BeforeEach
    void setUp() {
        memoryService = new JdbcConversationMemoryService(createDataSource());
        memoryService.initializeSchema();
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldPersistAndReadSnapshotUsingJdbcStore() {
        String conversationId = "conv-jdbc-001";

        memoryService.addUserMessage(conversationId, ChatCategory.MICE, "국제포럼 제안서 배경 정리");
        memoryService.addAssistantMessage(conversationId, ChatCategory.MICE, "배경/목표/방향 순으로 정리하겠습니다.");
        memoryService.updateCategorySummary(conversationId, ChatCategory.MICE, "국제포럼 제안서 초안 작성 대화");
        memoryService.addImportantNote(conversationId, ChatCategory.MICE, "VIP 의전 동선 포함 필요");

        ConversationMemorySnapshot snapshot = memoryService.snapshot(conversationId);

        assertEquals(2, snapshot.getRecentMessages().size());
        assertEquals("국제포럼 제안서 초안 작성 대화",
                snapshot.getCategorySummaries().get(ChatCategory.MICE.name()).getSummary());
        assertEquals(1, snapshot.getImportantNotes().size());
        assertEquals("VIP 의전 동선 포함 필요", snapshot.getImportantNotes().get(0).getNote());
        assertTrue(memoryService.formatRecentConversation(conversationId, 5).contains("배경/목표/방향"));
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldClearConversationData() {
        String conversationId = "conv-jdbc-002";

        memoryService.addUserMessage(conversationId, ChatCategory.DEV, "Spring Boot 리팩토링 순서");
        memoryService.updateCategorySummary(conversationId, ChatCategory.DEV, "리팩토링 논의 중");
        memoryService.addImportantNote(conversationId, ChatCategory.DEV, "Gradle 멀티모듈 고려");

        memoryService.clear(conversationId);

        ConversationMemorySnapshot snapshot = memoryService.snapshot(conversationId);
        assertEquals(0, snapshot.getRecentMessages().size());
        assertEquals(0, snapshot.getCategorySummaries().size());
        assertEquals(0, snapshot.getImportantNotes().size());
    }

    /**
     * 대상 기능의 동작을 검증한다.
     * @return DataSource 타입의 처리 결과
     */
    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:nalbbun-memory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
