package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMemoryRuleRegistry {

    private final List<CategoryMemoryRule<?>> rules;

    public CategoryMemoryRuleRegistry(List<CategoryMemoryRule<?>> rules) {
        this.rules = rules;
    }

    public CategoryMemoryRule<?> get(ChatCategory category) {
        return rules.stream()
                .filter(rule -> rule.category() == category)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("메모리 규칙이 없는 카테고리: " + category));
    }
}