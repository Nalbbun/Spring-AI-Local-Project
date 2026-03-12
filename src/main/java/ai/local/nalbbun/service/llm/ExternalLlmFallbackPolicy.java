package ai.local.nalbbun.service.llm;

public enum ExternalLlmFallbackPolicy {
    ALLOW_OPENAI,
    BLOCK_OPENAI;

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
