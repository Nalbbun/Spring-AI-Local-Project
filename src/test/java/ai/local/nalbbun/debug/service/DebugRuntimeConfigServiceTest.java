package ai.local.nalbbun.debug.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.service.memory.InMemoryConversationMemoryService;

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
                0, 0, 0, false, null, 0, null, null, null, 0, null,
                new InMemoryConversationMemoryService(), new RagProperties(), new MockEnvironment()
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
                0, 0, 0, false, null, 0, null, null, null, 0, null,
                new InMemoryConversationMemoryService(), new RagProperties(), new MockEnvironment()
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
