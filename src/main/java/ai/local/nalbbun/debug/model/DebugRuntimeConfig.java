package ai.local.nalbbun.debug.model;

import lombok.Data;

/**
 * DebugRuntimeConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: debug runtime config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DebugRuntimeConfig {
    /** resolverMode 값을 보관한다. */
    private String resolverMode;
    /** generalParserMode 값을 보관한다. */
    private String generalParserMode;
    /** travelParserMode 값을 보관한다. */
    private String travelParserMode;
    /** devParserMode 값을 보관한다. */
    private String devParserMode;
    /** miceParserMode 값을 보관한다. */
    private String miceParserMode;
    /** memoryStore 값을 보관한다. */
    private String memoryStore;
    /** memoryServiceType 값을 보관한다. */
    private String memoryServiceType;
    /** fallbackPolicy 값을 보관한다. */
    private String fallbackPolicy;
    /** conversationId 값을 보관한다. */
    private String conversationId;
    /** ollamaBaseUrl 값을 보관한다. */
    private String ollamaBaseUrl;
}
