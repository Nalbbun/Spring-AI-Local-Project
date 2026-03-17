package ai.local.nalbbun.category.dev.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

/**
 * RuleBasedDevParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: rule based dev parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuleBasedDevParser implements CategoryParsingStrategy<DevContext> {

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return DevContext 타입의 처리 결과
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
     * @return 처리 결과 문자열
     */
    @Override
    public String mode() {
        return "RULE";
    }

    /**
     * containsAny 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
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