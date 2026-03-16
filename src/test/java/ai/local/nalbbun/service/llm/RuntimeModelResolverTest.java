package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

class RuntimeModelResolverTest {

    @Test
    void shouldUseLocalOllamaModelWhenConfigured() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "qwen2.5-coder:14b"), "BLOCK_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.GENERAL, false);

        assertTrue(selection.ollama());
        assertEquals("gemma2:9b", selection.modelName());
    }

    @Test
    void shouldFallbackToOpenAiWhenPolicyAllowsAndToolModelUnsupported() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "blossom:latest"), "ALLOW_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.TRAVEL_SEARCH, true);

        assertTrue(!selection.ollama());
        assertTrue(selection.fallbackApplied());
        assertEquals("local-model-does-not-support-tools", selection.reason());
    }

    @Test
    void shouldBlockOpenAiFallbackWhenPolicyBlocksExternalTransfer() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("", ""), "BLOCK_OPENAI");

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
                "deepseek-r1:14b", travelSearchModel, travelSearchModel, false
        );
    }
}
