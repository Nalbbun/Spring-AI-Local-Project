package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * CategoryMemoryUpdater는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: category memory updater 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class CategoryMemoryUpdater {

    /** ruleRegistry 값을 보관한다. */
    private final CategoryMemoryRuleRegistry ruleRegistry;
    /** memoryService 값을 보관한다. */
    private final ConversationMemoryService memoryService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ruleRegistry ruleRegistry 값
     * @param memoryService memoryService 값
     */
    public CategoryMemoryUpdater(
            CategoryMemoryRuleRegistry ruleRegistry,
            ConversationMemoryService memoryService
    ) {
        this.ruleRegistry = ruleRegistry;
        this.memoryService = memoryService;
    }

    /**
     * 대상 값을 갱신한다.
     *
     * @param state 현재 처리 상태 정보
     * @param assistantResponse assistantResponse 값
     * @return CategoryMemoryUpdateResult 타입의 처리 결과
     */
    @SuppressWarnings("unchecked")
    public CategoryMemoryUpdateResult update(ConversationState state, String assistantResponse) {
        if (state == null || state.getResolvedCategory() == null || state.getCategoryContext() == null) {
            return CategoryMemoryUpdateResult.builder()
                    .summaryUpdated(false)
                    .addedNotes(List.of())
                    .build();
        }

        CategoryMemoryRule<CategoryContext> rule =
                (CategoryMemoryRule<CategoryContext>) ruleRegistry.get(state.getResolvedCategory());

        CategoryMemoryUpdate update = rule.extract(state, state.getCategoryContext(), assistantResponse);
        if (update == null) {
            return CategoryMemoryUpdateResult.builder()
                    .summaryUpdated(false)
                    .addedNotes(List.of())
                    .build();
        }

        boolean summaryUpdated = false;
        List<String> addedNotes = new ArrayList<>();

        if (update.getSummary() != null && !update.getSummary().isBlank()) {
            String previous = memoryService.getCategorySummary(
                    state.getConversationId(),
                    state.getResolvedCategory()
            );

            if (!update.getSummary().equals(previous)) {
                memoryService.updateCategorySummary(
                        state.getConversationId(),
                        state.getResolvedCategory(),
                        update.getSummary()
                );
                summaryUpdated = true;
            }
        }

        if (update.getImportantNotes() != null) {
            List<String> existingNotes = memoryService.getImportantNotes(
                    state.getConversationId(),
                    state.getResolvedCategory()
            );

            for (String note : update.getImportantNotes()) {
                if (note == null || note.isBlank()) {
                    continue;
                }
                if (!existingNotes.contains(note)) {
                    memoryService.addImportantNote(
                            state.getConversationId(),
                            state.getResolvedCategory(),
                            note
                    );
                    addedNotes.add(note);
                }
            }
        }

        return CategoryMemoryUpdateResult.builder()
                .summaryUpdated(summaryUpdated)
                .addedNotes(addedNotes)
                .build();
    }
}