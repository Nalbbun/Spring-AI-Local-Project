package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeConfigService;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.service.memory.InMemoryConversationMemoryService;

class RuntimeModelResolverTest {

    @Test
    void shouldUseLocalOllamaModelWhenConfigured() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(
                config("gemma2:9b", "qwen2.5-coder:14b"),
                runtimeConfig("BLOCK_OPENAI")
        );

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.GENERAL, false);

        assertTrue(selection.ollama());
        assertEquals("gemma2:9b", selection.modelName());
    }

    @Test
    void shouldFallbackToOpenAiWhenPolicyAllowsAndToolModelUnsupported() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(
                config("gemma2:9b", "blossom:latest"),
                runtimeConfig("ALLOW_OPENAI")
        );

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.TRAVEL_SEARCH, true);

        assertTrue(!selection.ollama());
        assertTrue(selection.fallbackApplied());
        assertEquals("local-model-does-not-support-tools", selection.reason());
    }

    @Test
    void shouldBlockOpenAiFallbackWhenPolicyBlocksExternalTransfer() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(
                config("", ""),
                runtimeConfig("BLOCK_OPENAI")
        );

        assertThrows(RuntimeModelResolutionException.class,
                () -> resolver.resolve(RuntimeModelTarget.TRAVEL_PLAN, false));
    }

    private DebugRuntimeModelConfigService config(String generalModel, String travelSearchModel) {
        return new DebugRuntimeModelConfigService(
                "RUNNING",
                generalModel,
                "qwen2.5-coder:14b",
                "exaone3.5:7.8b",
                travelSearchModel,
                "deepseek-r1:14b"
        );
    }

    private DebugRuntimeConfigService runtimeConfig(String fallbackPolicy) {
        return new DebugRuntimeConfigService(
                "HYBRID",
                "HYBRID",
                "HYBRID",
                "HYBRID",
                "HYBRID",
                "in-memory",
                fallbackPolicy,
                45_000L,
                2,
                800L,
                true,
                "nalbbun-ai-local",
                8080,
                "jdbc:h2:mem:test",
                "sa",
                "localhost",
                6379,
                "http://localhost:11434",
                new InMemoryConversationMemoryService(),
                new RagProperties(),
                new MockEnvironment().withProperty("spring.profiles.active", "test")
        );
    }
}
