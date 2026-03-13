package ai.local.nalbbun.registry;

import ai.local.nalbbun.category.common.CategoryHandler;
import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryHandlerRegistry {

    private final List<CategoryHandler> handlers;

    public CategoryHandlerRegistry(List<CategoryHandler> handlers) {
        this.handlers = handlers;
    }

    public CategoryHandler get(ChatCategory category) {
        return handlers.stream()
                .filter(handler -> handler.supports(category))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 카테고리: " + category));
    }
}