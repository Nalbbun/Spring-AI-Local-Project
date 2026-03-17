package ai.local.nalbbun.category.dev.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

/**
 * Rule Based Dev Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class RuleBasedDevParser implements CategoryParsingStrategy<DevContext> {

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public DevContext parse(ConversationState state, DevContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        if (containsAny(q, "리팩토링", "구조", "아키텍처")) {
            context.setTaskType("refactoring");
        } else if (containsAny(q, "에러", "오류", "예외", "실패")) {
            context.setTaskType("troubleshooting");
        } else if (containsAny(q, "설치", "설정", "세팅", "구성")) {
            context.setTaskType("setup");
        } else {
            context.setTaskType("implementation");
        }

        if (containsAny(q, "spring", "스프링")) context.getStackKeywords().add("spring");
        if (containsAny(q, "java", "자바")) context.getStackKeywords().add("java");
        if (containsAny(q, "docker", "도커")) context.getStackKeywords().add("docker");
        if (containsAny(q, "kubernetes", "쿠버네티스", "k8s")) context.getStackKeywords().add("kubernetes");
        if (containsAny(q, "gradle")) context.getStackKeywords().add("gradle");
        if (containsAny(q, "jenkins")) context.getStackKeywords().add("jenkins");

        context.setTopic(context.getStackKeywords().isEmpty()
                ? "general-dev"
                : String.join(",", context.getStackKeywords()));

        return context;
    }

    /**
     * mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String mode() {
        return "RULE";
    }

    /**
     * contains Any 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}