package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.common.CategoryContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryMemoryUpdater {

    private final CategoryMemoryRuleRegistry ruleRegistry;
    private final ConversationMemoryService memoryService;

    public CategoryMemoryUpdater(
            CategoryMemoryRuleRegistry ruleRegistry,
            ConversationMemoryService memoryService
    ) {
        this.ruleRegistry = ruleRegistry;
        this.memoryService = memoryService;
    }

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