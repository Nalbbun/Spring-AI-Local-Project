package ai.local.nalbbun.debug.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.category.common.CategoryResolverMode;
import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.service.llm.ExternalLlmFallbackPolicy;
import ai.local.nalbbun.service.memory.ConversationMemoryService;

@Service
public class DebugRuntimeConfigService {

    private final AtomicReference<CategoryResolverMode> resolverMode;
    private final Map<ChatCategory, AtomicReference<CategoryParserMode>> parserModes =
            new EnumMap<>(ChatCategory.class);
    private final String configuredMemoryStore;
    private final AtomicReference<String> fallbackPolicy;
    private final ConversationMemoryService conversationMemoryService;
    private final RagProperties ragProperties;
    private final Environment environment;

    private final boolean debugEnabled;
    private final String applicationName;
    private final int serverPort;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String redisHost;
    private final int redisPort;
    private final String ollamaBaseUrl;

    private final AtomicLong llmTimeoutMs;
    private final AtomicReference<Integer> llmRetryAttempts;
    private final AtomicLong llmRetryBackoffMs;

    private final CategoryResolverMode defaultResolverMode;
    private final Map<ChatCategory, CategoryParserMode> defaultParserModes = new EnumMap<>(ChatCategory.class);
    private final String defaultFallbackPolicy;
    private final long defaultLlmTimeoutMs;
    private final int defaultLlmRetryAttempts;
    private final long defaultLlmRetryBackoffMs;
    private final boolean defaultRagEnabled;
    private final int defaultRagTopK;
    private final double defaultRagSimilarityThreshold;
    private final boolean defaultRagIncludeCitations;
    private final boolean defaultRagGeneralEnabled;
    private final boolean defaultRagDevEnabled;
    private final boolean defaultRagMiceEnabled;
    private final boolean defaultRagTravelEnabled;
    private final String defaultRagDatasetLocation;

    public DebugRuntimeConfigService(
            @Value("${app.category.resolver.mode:HYBRID}") String resolverMode,
            @Value("${app.parser.general.mode:HYBRID}") String generalMode,
            @Value("${app.parser.travel.mode:HYBRID}") String travelMode,
            @Value("${app.parser.dev.mode:HYBRID}") String devMode,
            @Value("${app.parser.mice.mode:HYBRID}") String miceMode,
            @Value("${app.memory.store:in-memory}") String configuredMemoryStore,
            @Value("${app.llm.fallback-policy:BLOCK_OPENAI}") String fallbackPolicy,
            @Value("${app.llm.timeout-ms:45000}") long llmTimeoutMs,
            @Value("${app.llm.retry-attempts:2}") int llmRetryAttempts,
            @Value("${app.llm.retry-backoff-ms:800}") long llmRetryBackoffMs,
            @Value("${app.debug.enabled:false}") boolean debugEnabled,
            @Value("${spring.application.name:nalbbun-ai-local}") String applicationName,
            @Value("${server.port:8080}") int serverPort,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.data.redis.host:}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${app.ollama.base-url:}") String ollamaBaseUrl,
            ConversationMemoryService conversationMemoryService,
            RagProperties ragProperties,
            Environment environment
    ) {
        this.defaultResolverMode = safeResolverMode(resolverMode);
        this.resolverMode = new AtomicReference<>(this.defaultResolverMode);
        this.configuredMemoryStore = configuredMemoryStore;
        this.defaultFallbackPolicy = normalizeFallbackPolicy(fallbackPolicy);
        this.fallbackPolicy = new AtomicReference<>(this.defaultFallbackPolicy);
        this.conversationMemoryService = conversationMemoryService;
        this.ragProperties = ragProperties;
        this.environment = environment;

        this.debugEnabled = debugEnabled;
        this.applicationName = applicationName;
        this.serverPort = serverPort;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.ollamaBaseUrl = ollamaBaseUrl;

        this.defaultLlmTimeoutMs = Math.max(1000, llmTimeoutMs);
        this.defaultLlmRetryAttempts = Math.max(1, llmRetryAttempts);
        this.defaultLlmRetryBackoffMs = Math.max(0, llmRetryBackoffMs);
        this.llmTimeoutMs = new AtomicLong(this.defaultLlmTimeoutMs);
        this.llmRetryAttempts = new AtomicReference<>(this.defaultLlmRetryAttempts);
        this.llmRetryBackoffMs = new AtomicLong(this.defaultLlmRetryBackoffMs);

        CategoryParserMode generalParser = safeParserMode(generalMode);
        CategoryParserMode travelParser = safeParserMode(travelMode);
        CategoryParserMode devParser = safeParserMode(devMode);
        CategoryParserMode miceParser = safeParserMode(miceMode);
        defaultParserModes.put(ChatCategory.GENERAL, generalParser);
        defaultParserModes.put(ChatCategory.TRAVEL, travelParser);
        defaultParserModes.put(ChatCategory.DEV, devParser);
        defaultParserModes.put(ChatCategory.MICE, miceParser);
        parserModes.put(ChatCategory.GENERAL, new AtomicReference<>(generalParser));
        parserModes.put(ChatCategory.TRAVEL, new AtomicReference<>(travelParser));
        parserModes.put(ChatCategory.DEV, new AtomicReference<>(devParser));
        parserModes.put(ChatCategory.MICE, new AtomicReference<>(miceParser));

        this.defaultRagEnabled = ragProperties.isEnabled();
        this.defaultRagTopK = ragProperties.getTopK();
        this.defaultRagSimilarityThreshold = ragProperties.getSimilarityThreshold();
        this.defaultRagIncludeCitations = ragProperties.isIncludeCitations();
        this.defaultRagGeneralEnabled = ragProperties.getCategories().isGeneral();
        this.defaultRagDevEnabled = ragProperties.getCategories().isDev();
        this.defaultRagMiceEnabled = ragProperties.getCategories().isMice();
        this.defaultRagTravelEnabled = ragProperties.getCategories().isTravel();
        this.defaultRagDatasetLocation = ragProperties.getEvaluation().getDatasetLocation();
    }

    public CategoryResolverMode getResolverMode() {
        return resolverMode.get();
    }

    public void setResolverMode(CategoryResolverMode mode) {
        if (mode != null) {
            resolverMode.set(mode);
        }
    }

    public CategoryParserMode getParserMode(ChatCategory category) {
        AtomicReference<CategoryParserMode> ref = parserModes.get(category);
        return ref == null ? CategoryParserMode.HYBRID : ref.get();
    }

    public void setParserMode(ChatCategory category, CategoryParserMode mode) {
        if (category == null || mode == null) {
            return;
        }
        parserModes.computeIfAbsent(category, key -> new AtomicReference<>(CategoryParserMode.HYBRID))
                   .set(mode);
    }

    public ExternalLlmFallbackPolicy getFallbackPolicyEnum() {
        return ExternalLlmFallbackPolicy.from(fallbackPolicy.get());
    }

    public String getFallbackPolicy() {
        return fallbackPolicy.get();
    }

    public long getLlmTimeoutMs() {
        return llmTimeoutMs.get();
    }

    public int getLlmRetryAttempts() {
        return llmRetryAttempts.get();
    }

    public long getLlmRetryBackoffMs() {
        return llmRetryBackoffMs.get();
    }

    public DebugRuntimeConfig getCurrentConfig() {
        DebugRuntimeConfig config = new DebugRuntimeConfig();
        config.setResolverMode(getResolverMode().name());
        config.setGeneralParserMode(getParserMode(ChatCategory.GENERAL).name());
        config.setTravelParserMode(getParserMode(ChatCategory.TRAVEL).name());
        config.setDevParserMode(getParserMode(ChatCategory.DEV).name());
        config.setMiceParserMode(getParserMode(ChatCategory.MICE).name());
        config.setMemoryStore(normalizeMemoryStore(configuredMemoryStore));
        config.setMemoryServiceType(conversationMemoryService.getClass().getSimpleName());
        config.setFallbackPolicy(getFallbackPolicy());

        config.setDebugEnabled(debugEnabled);
        config.setActiveProfiles(activeProfiles());
        config.setApplicationName(applicationName);
        config.setServerPort(serverPort);
        config.setDatasourceUrl(datasourceUrl);
        config.setDatasourceUsername(datasourceUsername);
        config.setRedisHost(redisHost);
        config.setRedisPort(redisPort);
        config.setOllamaBaseUrl(ollamaBaseUrl);

        config.setRagEnabled(ragProperties.isEnabled());
        config.setRagTopK(ragProperties.getTopK());
        config.setRagSimilarityThreshold(ragProperties.getSimilarityThreshold());
        config.setRagIncludeCitations(ragProperties.isIncludeCitations());
        config.setRagGeneralEnabled(ragProperties.getCategories().isGeneral());
        config.setRagDevEnabled(ragProperties.getCategories().isDev());
        config.setRagMiceEnabled(ragProperties.getCategories().isMice());
        config.setRagTravelEnabled(ragProperties.getCategories().isTravel());
        config.setRagVectorStore(ragProperties.getVectorStore());
        config.setRagRegistryBaseDir(ragProperties.getRegistry().getBaseDir());
        config.setRagDatasetLocation(ragProperties.getEvaluation().getDatasetLocation());

        config.setLlmTimeoutMs(getLlmTimeoutMs());
        config.setLlmRetryAttempts(getLlmRetryAttempts());
        config.setLlmRetryBackoffMs(getLlmRetryBackoffMs());
        return config;
    }

    public DebugRuntimeConfig update(DebugRuntimeConfig request) {
        if (request == null) {
            return getCurrentConfig();
        }

        if (hasText(request.getResolverMode())) {
            setResolverMode(safeResolverMode(request.getResolverMode()));
        }
        if (hasText(request.getGeneralParserMode())) {
            setParserMode(ChatCategory.GENERAL, safeParserMode(request.getGeneralParserMode()));
        }
        if (hasText(request.getTravelParserMode())) {
            setParserMode(ChatCategory.TRAVEL, safeParserMode(request.getTravelParserMode()));
        }
        if (hasText(request.getDevParserMode())) {
            setParserMode(ChatCategory.DEV, safeParserMode(request.getDevParserMode()));
        }
        if (hasText(request.getMiceParserMode())) {
            setParserMode(ChatCategory.MICE, safeParserMode(request.getMiceParserMode()));
        }
        if (hasText(request.getFallbackPolicy())) {
            fallbackPolicy.set(normalizeFallbackPolicy(request.getFallbackPolicy()));
        }
        if (request.getLlmTimeoutMs() != null) {
            llmTimeoutMs.set(Math.max(1000, request.getLlmTimeoutMs()));
        }
        if (request.getLlmRetryAttempts() != null) {
            llmRetryAttempts.set(Math.max(1, request.getLlmRetryAttempts()));
        }
        if (request.getLlmRetryBackoffMs() != null) {
            llmRetryBackoffMs.set(Math.max(0, request.getLlmRetryBackoffMs()));
        }
        if (request.getRagEnabled() != null) {
            ragProperties.setEnabled(request.getRagEnabled());
        }
        if (request.getRagTopK() != null) {
            ragProperties.setTopK(Math.max(1, request.getRagTopK()));
        }
        if (request.getRagSimilarityThreshold() != null) {
            double threshold = request.getRagSimilarityThreshold();
            if (threshold < 0d) threshold = 0d;
            if (threshold > 1d) threshold = 1d;
            ragProperties.setSimilarityThreshold(threshold);
        }
        if (request.getRagIncludeCitations() != null) {
            ragProperties.setIncludeCitations(request.getRagIncludeCitations());
        }
        if (request.getRagGeneralEnabled() != null) {
            ragProperties.getCategories().setGeneral(request.getRagGeneralEnabled());
        }
        if (request.getRagDevEnabled() != null) {
            ragProperties.getCategories().setDev(request.getRagDevEnabled());
        }
        if (request.getRagMiceEnabled() != null) {
            ragProperties.getCategories().setMice(request.getRagMiceEnabled());
        }
        if (request.getRagTravelEnabled() != null) {
            ragProperties.getCategories().setTravel(request.getRagTravelEnabled());
        }
        if (hasText(request.getRagDatasetLocation())) {
            ragProperties.getEvaluation().setDatasetLocation(request.getRagDatasetLocation().trim());
        }

        return getCurrentConfig();
    }

    public DebugRuntimeConfig reset() {
        setResolverMode(defaultResolverMode);
        parserModes.forEach((category, ref) -> ref.set(defaultParserModes.getOrDefault(category, CategoryParserMode.HYBRID)));
        fallbackPolicy.set(defaultFallbackPolicy);
        llmTimeoutMs.set(defaultLlmTimeoutMs);
        llmRetryAttempts.set(defaultLlmRetryAttempts);
        llmRetryBackoffMs.set(defaultLlmRetryBackoffMs);
        ragProperties.setEnabled(defaultRagEnabled);
        ragProperties.setTopK(defaultRagTopK);
        ragProperties.setSimilarityThreshold(defaultRagSimilarityThreshold);
        ragProperties.setIncludeCitations(defaultRagIncludeCitations);
        ragProperties.getCategories().setGeneral(defaultRagGeneralEnabled);
        ragProperties.getCategories().setDev(defaultRagDevEnabled);
        ragProperties.getCategories().setMice(defaultRagMiceEnabled);
        ragProperties.getCategories().setTravel(defaultRagTravelEnabled);
        ragProperties.getEvaluation().setDatasetLocation(defaultRagDatasetLocation);
        return getCurrentConfig();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeMemoryStore(String value) {
        return hasText(value) ? value.trim().toLowerCase() : "in-memory";
    }

    private String normalizeFallbackPolicy(String value) {
        return hasText(value) ? value.trim().toUpperCase() : "BLOCK_OPENAI";
    }

    private CategoryResolverMode safeResolverMode(String value) {
        try {
            return CategoryResolverMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryResolverMode.HYBRID;
        }
    }

    private CategoryParserMode safeParserMode(String value) {
        try {
            return CategoryParserMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryParserMode.HYBRID;
        }
    }

    private List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles == null || profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.asList(profiles);
    }
}
