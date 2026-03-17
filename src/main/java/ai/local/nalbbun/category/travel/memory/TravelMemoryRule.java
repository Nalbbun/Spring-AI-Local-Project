package ai.local.nalbbun.category.travel.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TravelMemoryRule는 대화 메모리 규칙 또는 저장 처리를 담당하는 컴포넌트이다.
 * <p>주요 기능: travel memory rule 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelMemoryRule implements CategoryMemoryRule<TravelContext> {

    /**
     * category 기능을 수행한다.
     * @return ChatCategory 타입의 처리 결과
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
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

    /**
     * safe 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}