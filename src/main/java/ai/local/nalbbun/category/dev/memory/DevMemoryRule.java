package ai.local.nalbbun.category.dev.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DevMemoryRule는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: dev memory rule 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class DevMemoryRule implements CategoryMemoryRule<DevContext> {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    /**
     * extract 기능을 수행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @param assistantResponse assistantResponse 값
     * @return CategoryMemoryUpdate 타입의 처리 결과
     */
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

    /**
     * containsAny 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
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

    /**
     * truncateImportantSentence 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @return 처리 결과 문자열
     */
    private String truncateImportantSentence(String text) {
        if (text == null) return "";
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }
}