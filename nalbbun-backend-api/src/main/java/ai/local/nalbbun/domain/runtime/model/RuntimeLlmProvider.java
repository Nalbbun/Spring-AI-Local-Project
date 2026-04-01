package ai.local.nalbbun.domain.runtime.model;

public enum RuntimeLlmProvider {
    OLLAMA,
    VLLM,
    OPENAI;

    public boolean isApiCompatible() {
        return this == VLLM || this == OPENAI;
    }
}
