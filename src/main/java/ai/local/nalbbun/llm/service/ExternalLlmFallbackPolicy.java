package ai.local.nalbbun.llm.service;

/**
 * External Llm Fallback Policy 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 열거형 상수는 상태 표현이나 분기 기준으로 사용된다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
public enum ExternalLlmFallbackPolicy {
    ALLOW_OPENAI,
    BLOCK_OPENAI;

    /**
     * from 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public static ExternalLlmFallbackPolicy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALLOW_OPENAI;
        }

        for (ExternalLlmFallbackPolicy policy : values()) {
            if (policy.name().equalsIgnoreCase(raw.trim())) {
                return policy;
            }
        }
        return ALLOW_OPENAI;
    }
}
