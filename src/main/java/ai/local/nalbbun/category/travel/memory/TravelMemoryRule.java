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
 * Travel Memory Rule 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelMemoryRule implements CategoryMemoryRule<TravelContext> {

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.TRAVEL;
    }

    /**
     * extract 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}