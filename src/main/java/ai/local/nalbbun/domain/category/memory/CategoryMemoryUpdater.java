package ai.local.nalbbun.domain.category.memory;

import ai.local.nalbbun.domain.category.model.CategoryContext;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Category Memory Updater 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class CategoryMemoryUpdater {

    private final CategoryMemoryRuleRegistry ruleRegistry;
    private final ConversationMemoryService memoryService;

    /**
     * Category Memory Updater 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public CategoryMemoryUpdater(
            CategoryMemoryRuleRegistry ruleRegistry,
            ConversationMemoryService memoryService
    ) {
        this.ruleRegistry = ruleRegistry;
        this.memoryService = memoryService;
    }

    /**
     * update 작업을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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