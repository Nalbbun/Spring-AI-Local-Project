package ai.local.nalbbun.category.mice.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mice Memory Rule 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class MiceMemoryRule implements CategoryMemoryRule<MiceContext> {

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.MICE;
    }

    /**
     * extract 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * contains Any 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}