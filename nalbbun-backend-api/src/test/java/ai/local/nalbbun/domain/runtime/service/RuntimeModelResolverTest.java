package ai.local.nalbbun.domain.runtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.admin.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.admin.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.admin.service.DebugRuntimeOpenAiConnectionService;
import ai.local.nalbbun.admin.service.DebugRuntimeVllmConnectionService;
import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import ai.local.nalbbun.domain.runtime.model.RuntimeLlmProvider;
import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyCrypto;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService;

class RuntimeModelResolverTest {

    @Test
    void shouldUseLocalOllamaModelWhenConfigured() {
        RuntimeModelResolver resolver = resolver(config("gemma2:9b", "qwen2.5-coder:14b"), "BLOCK_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.GENERAL, false);

        assertEquals(RuntimeLlmProvider.OLLAMA, selection.provider());
        assertEquals("gemma2:9b", selection.modelName());
    }

    @Test
    void shouldFallbackToOpenAiWhenPolicyAllowsAndToolModelUnsupported() {
        RuntimeModelResolver resolver = resolver(config("gemma2:9b", "blossom:latest"), "ALLOW_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.TRAVEL_SEARCH, true);

        assertEquals(RuntimeLlmProvider.OPENAI, selection.provider());
        assertTrue(selection.fallbackApplied());
        assertEquals("ollama-first:tool-not-supported", selection.reason());
    }

    @Test
    void shouldBlockOpenAiFallbackWhenPolicyBlocksExternalTransfer() {
        RuntimeModelResolver resolver = resolver(config("", ""), "BLOCK_OPENAI");

        assertThrows(RuntimeModelResolutionException.class,
                () -> resolver.resolve(RuntimeModelTarget.TRAVEL_PLAN, false));
    }

    private RuntimeModelResolver resolver(DebugRuntimeModelConfigService configService, String fallbackPolicy) {
        ApiKeyRepository repository = new ApiKeyRepository() {
            @Override public List<ApiKeyEntry> findAll() { return List.of(); }
            @Override public List<ApiKeyEntry> findByProvider(String provider) { return List.of(); }
            @Override public Optional<ApiKeyEntry> findById(String id) { return Optional.empty(); }
            @Override public Optional<ApiKeyEntry> findActiveByProvider(String provider) { return Optional.empty(); }
            @Override public ApiKeyEntry save(ApiKeyEntry entry) { return entry; }
            @Override public ApiKeyEntry update(ApiKeyEntry entry) { return entry; }
            @Override public void delete(String id) {}
        };
        ApiKeyCrypto crypto = new ApiKeyCrypto("0123456789abcdef0123456789abcdef");
        ApiKeyService apiKeyService = new ApiKeyService(repository, crypto, null);
        return new RuntimeModelResolver(
                configService,
                new DebugRuntimeOllamaConnectionService("http://127.0.0.1:11434"),
                new DebugRuntimeVllmConnectionService("http://127.0.0.1:8000/v1", "", "VLLM", fallbackPolicy, fallbackPolicy, fallbackPolicy, fallbackPolicy, apiKeyService),
                new DebugRuntimeOpenAiConnectionService("https://api.openai.com/v1", "gpt-4.1-mini", "OPENAI", fallbackPolicy, fallbackPolicy, fallbackPolicy, fallbackPolicy, apiKeyService),
                new CategoryModelPriorityService(),
                fallbackPolicy
        );
    }

    private DebugRuntimeModelConfigService config(String generalModel, String travelSearchModel) {
        return new DebugRuntimeModelConfigService(
                "RUNNING",
                generalModel,
                "qwen2.5-coder:14b",
                "exaone3.5:7.8b",
                travelSearchModel,
                "deepseek-r1:14b", "", "24h"
        );
    }
}
