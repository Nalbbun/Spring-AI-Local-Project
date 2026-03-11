package ai.local.nalbbun.category.general.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GeneralMemoryRule implements CategoryMemoryRule<GeneralContext> {

    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    @Override
    public CategoryMemoryUpdate extract(ConversationState state, GeneralContext context, String assistantResponse) {
        String summary = buildSummary(state, context);
        List<String> notes = new ArrayList<>();

        if (assistantResponse != null && assistantResponse.contains("한 문장")) {
            notes.add("사용자는 설명을 더 짧게 요약하는 후속 요청을 할 수 있음");
        }

        return CategoryMemoryUpdate.builder()
                .summary(summary)
                .importantNotes(notes)
                .build();
    }

    private String buildSummary(ConversationState state, GeneralContext context) {
        return String.format(
                "general intent=%s, 최근 질문=%s",
                context.getIntent(),
                truncate(state.getUserQuery(), 50)
        );
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}