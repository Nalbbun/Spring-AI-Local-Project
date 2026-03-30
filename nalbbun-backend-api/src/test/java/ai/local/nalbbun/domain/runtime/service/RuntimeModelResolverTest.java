package ai.local.nalbbun.domain.runtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.domain.runtime.model.RuntimeModelTarget;
import ai.local.nalbbun.admin.service.DebugRuntimeModelConfigService;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelResolutionException;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelResolver;
import ai.local.nalbbun.domain.runtime.service.RuntimeModelSelection;

/**
 * Runtime Model Resolver Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class RuntimeModelResolverTest {

    /**
     * Use Local Ollama Model When Configured 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldUseLocalOllamaModelWhenConfigured() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "qwen2.5-coder:14b"), null, "BLOCK_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.GENERAL, false);

        assertTrue(selection.ollama());
        assertEquals("gemma2:9b", selection.modelName());
    }

    /**
     * Fallback To Open Ai When Policy Allows And Tool Model Unsupported 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldFallbackToOpenAiWhenPolicyAllowsAndToolModelUnsupported() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("gemma2:9b", "blossom:latest"), null, "ALLOW_OPENAI");

        RuntimeModelSelection selection = resolver.resolve(RuntimeModelTarget.TRAVEL_SEARCH, true);

        assertTrue(!selection.ollama());
        assertTrue(selection.fallbackApplied());
        assertEquals("local-model-does-not-support-tools", selection.reason());
    }

    /**
     * Block Open Ai Fallback When Policy Blocks External Transfer 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Test
    void shouldBlockOpenAiFallbackWhenPolicyBlocksExternalTransfer() {
        RuntimeModelResolver resolver = new RuntimeModelResolver(config("", ""), null, "BLOCK_OPENAI");

        assertThrows(RuntimeModelResolutionException.class,
                () -> resolver.resolve(RuntimeModelTarget.TRAVEL_PLAN, false));
    }

    /**
     * config 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
