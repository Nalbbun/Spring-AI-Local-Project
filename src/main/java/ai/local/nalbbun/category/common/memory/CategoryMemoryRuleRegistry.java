package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CategoryMemoryRuleRegistry는 구현체 또는 메타데이터를 등록하고 조회하는 레지스트리이다.
 * <p>주요 기능: category memory rule registry 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class CategoryMemoryRuleRegistry {

    /** rules 값을 보관한다. */
    private final List<CategoryMemoryRule<?>> rules;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param rules rules 목록 정보
     */
    public CategoryMemoryRuleRegistry(List<CategoryMemoryRule<?>> rules) {
        this.rules = rules;
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @return CategoryMemoryRule<?> 타입의 처리 결과
     */
    public CategoryMemoryRule<?> get(ChatCategory category) {
        return rules.stream()
                .filter(rule -> rule.category() == category)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("메모리 규칙이 없는 카테고리: " + category));
    }
}