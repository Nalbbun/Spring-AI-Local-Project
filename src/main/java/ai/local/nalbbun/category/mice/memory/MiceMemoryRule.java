package ai.local.nalbbun.category.mice.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MiceMemoryRule는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: mice memory rule 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class MiceMemoryRule implements CategoryMemoryRule<MiceContext> {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
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
     * truncate 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param max max 값
     * @return 처리 결과 문자열
     */
    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}