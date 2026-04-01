package ai.local.nalbbun.domain.runtime.model;

/**
 * 카테고리별 모델 우선순위 정책.
 */
public enum ModelPriority {

    OLLAMA_FIRST("Ollama 우선 (실패 시 OpenAI fallback)"),
    VLLM_FIRST("vLLM 우선 (실패 시 OpenAI fallback)"),
    OPENAI_FIRST("OpenAI 우선"),
    OLLAMA_ONLY("Ollama 전용"),
    VLLM_ONLY("vLLM 전용"),
    OPENAI_ONLY("OpenAI 전용");

    public final String description;

    ModelPriority(String description) {
        this.description = description;
    }

    public static ModelPriority from(String raw) {
        if (raw == null || raw.isBlank()) return OLLAMA_FIRST;
        try { return valueOf(raw.trim().toUpperCase()); }
        catch (Exception e) { return OLLAMA_FIRST; }
    }
}
