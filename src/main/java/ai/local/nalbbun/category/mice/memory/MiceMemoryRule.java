package ai.local.nalbbun.category.mice.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MiceMemoryRule implements CategoryMemoryRule<MiceContext> {

    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    @Override
    public CategoryMemoryUpdate extract(ConversationState state, MiceContext context, String assistantResponse) {
        String summary = String.format(
                "mice eventType=%s, deliverable=%s, targetRegion=%s",
                context.getEventType(),
                context.getDeliverableType(),
                context.getTargetRegion()
        );

        List<String> notes = new ArrayList<>();

        if (containsAny(state.getUserQuery(), "배경", "목표", "방향")) {
            notes.add("사용자는 배경-목표-방향 구조를 선호함");
        }

        if (containsAny(state.getUserQuery(), "슬로건", "브랜딩", "메시지")) {
            notes.add("사용자는 행사 커뮤니케이션 메시지와 슬로건 설계를 중요하게 다룸");
        }

        if (containsAny(assistantResponse, "핵심", "권장", "전략")) {
            notes.add(truncate(assistantResponse, 120));
        }

        return CategoryMemoryUpdate.builder()
                .summary(summary)
                .importantNotes(notes)
                .build();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}