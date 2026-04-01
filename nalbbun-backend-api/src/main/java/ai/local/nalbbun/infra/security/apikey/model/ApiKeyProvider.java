package ai.local.nalbbun.infra.security.apikey.model;

/**
 * 지원하는 API 키 프로바이더 목록.
 */
public enum ApiKeyProvider {

    OPENAI("OpenAI", "GPT 계열 LLM API", "https://platform.openai.com/api-keys"),
    VLLM("vLLM", "vLLM 또는 OpenAI 호환 내부 API 키", null),
    TAVILY("Tavily", "웹 검색 API", "https://app.tavily.com"),
    ANTHROPIC("Anthropic", "Claude API (향후 지원)", "https://console.anthropic.com"),
    CUSTOM("Custom", "사용자 정의 API", null);

    public final String displayName;
    public final String description;
    public final String keyIssueUrl;

    ApiKeyProvider(String displayName, String description, String keyIssueUrl) {
        this.displayName  = displayName;
        this.description  = description;
        this.keyIssueUrl  = keyIssueUrl;
    }
}
