package ai.local.nalbbun.domain.runtime.port;

import ai.local.nalbbun.domain.category.CategoryParserMode;
import ai.local.nalbbun.domain.category.CategoryResolverMode;
import ai.local.nalbbun.domain.category.model.ChatCategory;

public interface RuntimeCategoryPolicyPort {
    CategoryResolverMode getResolverMode();
    CategoryParserMode getParserMode(ChatCategory category);
}
