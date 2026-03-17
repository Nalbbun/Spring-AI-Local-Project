package ai.local.nalbbun.service.llm;

/**
 * ExternalLlmFallbackPolicy는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: external llm fallback policy 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public enum ExternalLlmFallbackPolicy {
    /** BLOCK_OPENAI 값을 보관한다. */
    ALLOW_OPENAI,
    BLOCK_OPENAI;

    /**
     * from 기능을 수행한다.
     *
     * @param raw raw 값
     * @return ExternalLlmFallbackPolicy 타입의 처리 결과
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
