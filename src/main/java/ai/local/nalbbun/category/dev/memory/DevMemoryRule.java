package ai.local.nalbbun.category.dev.memory;

import ai.local.nalbbun.category.common.memory.CategoryMemoryRule;
import ai.local.nalbbun.category.common.memory.CategoryMemoryUpdate;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.category.model.ConversationState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev Memory Rule 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class DevMemoryRule implements CategoryMemoryRule<DevContext> {

    /**
     * category 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public ChatCategory category() {
        return ChatCategory.DEV;
    }

    /**
     * extract 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * truncate Important Sentence 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String truncateImportantSentence(String text) {
        if (text == null) return "";
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }
}