package ai.local.nalbbun.category.dev.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedDevParser implements CategoryParsingStrategy<DevContext> {

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

    @Override
    public String mode() {
        return "RULE";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}