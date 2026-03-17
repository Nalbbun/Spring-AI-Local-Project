package ai.local.nalbbun.category.common.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Category Memory Rule Registry 타입이다.
 *
 * <p>기능 설명: 구성 요소를 등록하고 조회한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class CategoryMemoryRuleRegistry {

    private final List<CategoryMemoryRule<?>> rules;

    /**
     * Category Memory Rule Registry 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public CategoryMemoryRuleRegistry(List<CategoryMemoryRule<?>> rules) {
        this.rules = rules;
    }

    /**
     * get 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public CategoryMemoryRule<?> get(ChatCategory category) {
        return rules.stream()
                .filter(rule -> rule.category() == category)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("메모리 규칙이 없는 카테고리: " + category));
    }
}