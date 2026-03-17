package ai.local.nalbbun.category.general.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * GeneralMemoryRule는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: general memory rule 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class GeneralMemoryRule implements CategoryMemoryRule<GeneralContext> {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
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

    /**
     * 필요한 결과 객체를 구성한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 결과 문자열
     */
    private String buildSummary(ConversationState state, GeneralContext context) {
        return String.format(
                "general intent=%s, 최근 질문=%s",
                context.getIntent(),
                truncate(state.getUserQuery(), 50)
        );
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