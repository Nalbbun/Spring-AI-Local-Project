package ai.local.nalbbun.registry;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CategoryHandlerRegistry는 구현체 또는 메타데이터를 등록하고 조회하는 레지스트리이다.
 * <p>주요 기능: category handler registry 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class CategoryHandlerRegistry {

    /** handlers 값을 보관한다. */
    private final List<CategoryHandler> handlers;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param handlers handlers 목록 정보
     */
    public CategoryHandlerRegistry(List<CategoryHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @return CategoryHandler 타입의 처리 결과
     */
    public CategoryHandler get(ChatCategory category) {
        return handlers.stream()
                .filter(handler -> handler.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 카테고리: " + category));
    }
}