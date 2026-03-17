package ai.local.nalbbun.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.debug.service.DebugRuntimeModelConfigService;

/**
 * RuntimeModelResolverTest는 대상 기능의 동작을 검증하는 테스트 클래스이다.
 * <p>주요 기능: runtime model resolver test 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
class RuntimeModelResolverTest {

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldUseLocalOllamaModelWhenConfigured() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "qwen2.5-coder:14b"), "BLOCK_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.GENERAL, false);

        assertTrue(selection.ollama());
        assertEquals("gemma2:9b", selection.modelName());
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldFallbackToOpenAiWhenPolicyAllowsAndToolModelUnsupported() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "blossom:latest"), "ALLOW_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.TRAVEL_SEARCH, true);

        assertTrue(!selection.ollama());
        assertTrue(selection.fallbackApplied());
        assertEquals("local-model-does-not-support-tools", selection.reason());
    }

    /**
     * 대상 기능의 동작을 검증한다.
     */
    @Test
    void shouldBlockOpenAiFallbackWhenPolicyBlocksExternalTransfer() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("", ""), "BLOCK_OPENAI");

        assertThrows(RuntimeModelResolutionException.class,
                () -> resolver.resolve(RuntimeModelTarget.TRAVEL_PLAN, false));
    }

    /**
     * 대상 기능의 동작을 검증한다.
     *
     * @param generalModel generalModel 값
     * @param travelSearchModel travelSearchModel 값
     * @return DebugRuntimeModelConfigService 타입의 처리 결과
     */
    private DebugRuntimeModelConfigService config(String generalModel, String travelSearchModel) {
        return new DebugRuntimeModelConfigService(
                "RUNNING",
                generalModel,
                "qwen2.5-coder:14b",
                "exaone3.5:7.8b",
                travelSearchModel,
                "deepseek-r1:14b", travelSearchModel, travelSearchModel
        );
    }
}
