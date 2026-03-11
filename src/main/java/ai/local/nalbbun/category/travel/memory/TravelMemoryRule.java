package ai.local.nalbbun.category.travel.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TravelMemoryRule implements CategoryMemoryRule<TravelContext> {

    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    @Override
    public CategoryMemoryUpdate extract(ConversationState state, TravelContext context, String assistantResponse) {
        String summary = String.format(
                "travel destination=%s, days=%s, budget=%s, parser=%s, replan=%s",
                safe(context.getDestination()),
                safe(context.getDays()),
                safe(context.getMaxBudget()),
                safe(context.getParserMode()),
                context.isReplan()
        );

        List<String> notes = new ArrayList<>();

        if (context.getDestination() != null) {
            notes.add("최근 여행지는 " + context.getDestination());
        }

        if (context.getMaxBudget() != null) {
            notes.add("최근 여행 예산 기준은 " + String.format("%,d원", context.getMaxBudget()));
        }

        if (context.isReplan()) {
            notes.add("예산 초과로 재계획을 수행한 이력이 있음");
        }

        if (assistantResponse != null && assistantResponse.contains("예산 초과")) {
            notes.add("사용자는 예산 적합 여부를 중요하게 확인함");
        }

        return CategoryMemoryUpdate.builder()
                .summary(summary)
                .importantNotes(notes)
                .build();
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}