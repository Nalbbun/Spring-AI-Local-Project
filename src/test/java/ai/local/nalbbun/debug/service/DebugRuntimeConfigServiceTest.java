package ai.local.nalbbun.debug.service;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.service.memory.InMemoryConversationMemoryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugRuntimeConfigServiceTest {

    @Test
    void shouldExposeMemoryStoreMetadataAlongsideRuntimeConfig() {
        DebugRuntimeConfigService service = new DebugRuntimeConfigService(
                "HYBRID",
                "RULE",
                "LLM",
                "HYBRID",
                "RULE",
                "redis",
                "BLOCK_OPENAI",
                new InMemoryConversationMemoryService()
        );

        DebugRuntimeConfig config = service.getCurrentConfig();

        assertEquals("HYBRID", config.getResolverMode());
        assertEquals("redis", config.getMemoryStore());
        assertEquals("InMemoryConversationMemoryService", config.getMemoryServiceType());
        assertEquals("BLOCK_OPENAI", config.getFallbackPolicy());
    }

    @Test
    void shouldUpdateParserModesWithoutChangingMemoryMetadata() {
        DebugRuntimeConfigService service = new DebugRuntimeConfigService(
                "RULE",
                "RULE",
                "RULE",
                "RULE",
                "RULE",
                "jdbc",
                "ALLOW_OPENAI",
                new InMemoryConversationMemoryService()
        );

        DebugRuntimeConfig request = new DebugRuntimeConfig();
        request.setResolverMode("LLM");
        request.setDevParserMode("HYBRID");
        request.setTravelParserMode("LLM");

        DebugRuntimeConfig updated = service.update(request);

        assertEquals("LLM", updated.getResolverMode());
        assertEquals(CategoryParserMode.HYBRID.name(), updated.getDevParserMode());
        assertEquals(CategoryParserMode.LLM.name(), updated.getTravelParserMode());
        assertEquals("jdbc", updated.getMemoryStore());
        assertEquals("InMemoryConversationMemoryService", updated.getMemoryServiceType());
        assertEquals("ALLOW_OPENAI", updated.getFallbackPolicy());
        assertEquals(CategoryParserMode.RULE.name(), service.getParserMode(ChatCategory.GENERAL).name());
    }
}
