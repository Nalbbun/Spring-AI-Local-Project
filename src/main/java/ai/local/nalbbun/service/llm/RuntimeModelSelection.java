package ai.local.nalbbun.service.llm;

public record RuntimeModelSelection(
        boolean ollama,
        String modelName,
        boolean fallbackApplied,
        String reason
) {
    public String describe() {
        String provider = ollama ? "OLLAMA:" + modelName : "OPENAI:default";
        if (!fallbackApplied) {
            return provider;
        }
        return provider + " (fallback: " + reason + ")";
    }
}
