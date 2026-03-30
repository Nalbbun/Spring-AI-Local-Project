package ai.local.nalbbun.domain.category.general.memory;

import ai.local.nalbbun.domain.category.memory.CategoryMemoryRule;
import ai.local.nalbbun.domain.category.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.domain.category.general.model.GeneralContext;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * General Memory Rule 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class GeneralMemoryRule implements CategoryMemoryRule<GeneralContext> {

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.GENERAL;
    }

    /**
     * extract 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * build Summary 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}