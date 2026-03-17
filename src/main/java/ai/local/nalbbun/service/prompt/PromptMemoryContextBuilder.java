package ai.local.nalbbun.service.prompt;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.prompt.PromptMemoryContext;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PromptMemoryContextBuilder는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: prompt memory context builder 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class PromptMemoryContextBuilder {

    /** memoryService 값을 보관한다. */
    private final ConversationMemoryService memoryService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param memoryService memoryService 값
     */
    public PromptMemoryContextBuilder(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 필요한 결과 객체를 구성한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @return PromptMemoryContext 타입의 처리 결과
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
     * @param s s 값
     * @return 처리 결과 문자열
     */
    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}