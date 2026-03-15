package ai.local.nalbbun.debug.model;

import java.util.List;

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

    private Boolean debugEnabled;
    private List<String> activeProfiles;
    private String applicationName;
    private Integer serverPort;

    private String datasourceUrl;
    private String datasourceUsername;
    private String redisHost;
    private Integer redisPort;
    private String ollamaBaseUrl;
    private String multipartMaxFileSize;
    private String multipartMaxRequestSize;

    private Boolean ragEnabled;
    private Integer ragTopK;
    private Double ragSimilarityThreshold;
    private Boolean ragIncludeCitations;
    private Boolean ragGeneralEnabled;
    private Boolean ragDevEnabled;
    private Boolean ragMiceEnabled;
    private Boolean ragTravelEnabled;
    private String ragVectorStore;
    private String ragRegistryBaseDir;
    private String ragDatasetLocation;
    private Integer ragMaxUploadFileCount;

    private Long llmTimeoutMs;
    private Integer llmRetryAttempts;
    private Long llmRetryBackoffMs;
}
