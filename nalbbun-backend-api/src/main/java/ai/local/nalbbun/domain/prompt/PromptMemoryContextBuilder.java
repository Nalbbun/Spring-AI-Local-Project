package ai.local.nalbbun.domain.prompt;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.PromptMemoryContext;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prompt Memory Context Builder 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Component
public class PromptMemoryContextBuilder {

    private final ConversationMemoryService memoryService;

    /**
     * Prompt Memory Context Builder 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public PromptMemoryContextBuilder(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * build 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public PromptMemoryContext build(String conversationId, ChatCategory category) {
        String summary = safe(memoryService.getCategorySummary(conversationId, category));
        List<String> notes = memoryService.getImportantNotes(conversationId, category);
        String recentConversation = safe(memoryService.formatRecentConversation(conversationId, 10));

        String importantNotesBlock = notes == null || notes.isEmpty()
                ? "(중요 메모 없음)"
                : notes.stream()
                    .map(note -> "- " + note)
                    .collect(Collectors.joining("\n"));

        if (summary.isBlank()) {
            summary = "(카테고리 요약 없음)";
        }

        if (recentConversation.isBlank()) {
            recentConversation = "(최근 대화 없음)";
        }

        return new PromptMemoryContext(
                summary,
                importantNotesBlock,
                recentConversation
        );
    }

    /**
     * safe 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}