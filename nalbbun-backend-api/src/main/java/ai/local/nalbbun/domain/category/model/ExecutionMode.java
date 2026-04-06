package ai.local.nalbbun.domain.category.model;

public enum ExecutionMode {
    AUTO,
    CHAT,
    RAG,
    AGENT;

    public static ExecutionMode from(String value, ExecutionMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ExecutionMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
