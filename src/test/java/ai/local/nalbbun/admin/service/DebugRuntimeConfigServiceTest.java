package ai.local.nalbbun.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ai.local.nalbbun.domain.category.CategoryParserMode;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.admin.model.DebugRuntimeConfig;
import ai.local.nalbbun.admin.service.DebugRuntimeConfigService;
import ai.local.nalbbun.domain.memory.service.InMemoryConversationMemoryService;

/**
 * Debug Runtime Config Service Test 타입이다.
 *
 * <p>기능 설명: 대상 컴포넌트의 기대 동작과 회귀 여부를 검증한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 테스트 데이터, 목 객체, 검증 조건</p>
 * <p>출력: 검증 결과와 회귀 방지용 보장</p>
 */
class DebugRuntimeConfigServiceTest {

    /**
     * Expose Memory Store Metadata Alongside Runtime Config 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
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

    /**
     * Update Parser Modes Without Changing Memory Metadata 기대 동작을 검증한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
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
