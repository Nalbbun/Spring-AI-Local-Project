package ai.local.nalbbun.debug.model;

import lombok.Data;

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
