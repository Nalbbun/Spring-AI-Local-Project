package ai.local.nalbbun.category.dev.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DevMemoryRule implements CategoryMemoryRule<DevContext> {

    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    @Override
    public CategoryMemoryUpdate extract(ConversationState state, DevContext context, String assistantResponse) {
        String summary = String.format(
                "dev taskType=%s, topic=%s, stack=%s",
                context.getTaskType(),
                context.getTopic(),
                context.getStackKeywords()
        );

        List<String> notes = new ArrayList<>();

        if (containsAny(state.getUserQuery(), "리팩토링", "구조", "아키텍처")) {
            notes.add("사용자는 구조화된 리팩토링/일반화 방향을 선호함");
        }

        if (containsAny(state.getUserQuery(), "gradle", "build.gradle")) {
            notes.add("Gradle 기반 프로젝트 구조와 build.gradle 설계를 중요하게 다룸");
        }

        if (containsAny(assistantResponse, "권장", "우선", "핵심")) {
            notes.add(truncateImportantSentence(assistantResponse));
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

    private String truncateImportantSentence(String text) {
        if (text == null) return "";
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }
}