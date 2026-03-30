package ai.local.nalbbun.domain.runtime.model;

/**
 * 카테고리별 모델 우선순위 정책.
 *
 * OLLAMA_FIRST  : Ollama 시도 → 실패 시 OpenAI fallback
 * OPENAI_FIRST  : OpenAI 우선 → Ollama fallback
 * OLLAMA_ONLY   : Ollama 전용 (OpenAI 사용 안 함)
 * OPENAI_ONLY   : OpenAI 전용 (Ollama 사용 안 함)
 */
public enum ModelPriority {

    OLLAMA_FIRST("Ollama 우선 (실패 시 OpenAI fallback)"),
    OPENAI_FIRST("OpenAI 우선 (실패 시 Ollama fallback)"),
    OLLAMA_ONLY ("Ollama 전용"),
    OPENAI_ONLY ("OpenAI 전용");

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
