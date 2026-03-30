package ai.local.nalbbun.domain.memory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.infra.db.memory.jdbc.JdbcConversationMemoryService;

/**
 * Jdbc Conversation Memory Service Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class JdbcConversationMemoryServiceTest {

    private JdbcConversationMemoryService memoryService;

    /**
     * Up 값을 설정한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @BeforeEach
    void setUp() {
        memoryService = new JdbcConversationMemoryService(createDataSource());
//        memoryService.initializeSchema();
    }

    /**
     * Persist And Read Snapshot Using Jdbc Store 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Clear Conversation Data 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * create Data Source 객체를 생성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:nalbbun-memory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
