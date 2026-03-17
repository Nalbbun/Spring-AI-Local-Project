package ai.local.nalbbun.internal.model;

import lombok.Data;

/**
 * Debug Runtime Config 타입이다.
 *
 * <p>기능 설명: 스프링 빈과 런타임 설정을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 프로퍼티 값, 환경 변수, 스프링 컨텍스트 정보</p>
 * <p>출력: 빈 등록 결과 또는 런타임 설정 상태</p>
 */
@Data
public class DebugRuntimeConfig {
    private String resolverMode;
    private String generalParserMode;
    private String travelParserMode;
    private String devParserMode;
    private String miceParserMode;
    private String memoryStore;
    private String memoryServiceType;
    private String fallbackPolicy;
    private String conversationId;
    private String ollamaBaseUrl;
}
