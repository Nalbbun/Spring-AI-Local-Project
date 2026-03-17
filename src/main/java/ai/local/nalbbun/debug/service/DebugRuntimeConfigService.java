package ai.local.nalbbun.debug.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.category.common.CategoryParserMode;
import ai.local.nalbbun.category.common.CategoryResolverMode;
import ai.local.nalbbun.debug.model.DebugRuntimeConfig;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.service.memory.ConversationMemoryService;

@Service
public class DebugRuntimeConfigService {

    private final AtomicReference<CategoryResolverMode> resolverMode;
    private final Map<ChatCategory, AtomicReference<CategoryParserMode>> parserModes =
            new EnumMap<>(ChatCategory.class);
    private final String configuredMemoryStore;
    private final String fallbackPolicy;
    private final ConversationMemoryService conversationMemoryService;

    private DebugRuntimeOllamaConnectionService ollamaConnectionService;

    public DebugRuntimeConfigService(
            @Value("${app.category.resolver.mode:HYBRID}") String resolverMode,
            @Value("${app.parser.general.mode:HYBRID}") String generalMode,
            @Value("${app.parser.travel.mode:HYBRID}") String travelMode,
            @Value("${app.parser.dev.mode:HYBRID}") String devMode,
            @Value("${app.parser.mice.mode:HYBRID}") String miceMode,
            @Value("${app.memory.store:in-memory}") String configuredMemoryStore,
            @Value("${app.llm.fallback-policy:BLOCK_OPENAI}") String fallbackPolicy,
            ConversationMemoryService conversationMemoryService
    ) {
        this.resolverMode = new AtomicReference<>(safeResolverMode(resolverMode));
        this.configuredMemoryStore = configuredMemoryStore;
        this.fallbackPolicy = normalizeFallbackPolicy(fallbackPolicy);
        this.conversationMemoryService = conversationMemoryService;

        parserModes.put(ChatCategory.GENERAL, new AtomicReference<>(safeParserMode(generalMode)));
        parserModes.put(ChatCategory.TRAVEL, new AtomicReference<>(safeParserMode(travelMode)));
        parserModes.put(ChatCategory.DEV, new AtomicReference<>(safeParserMode(devMode)));
        parserModes.put(ChatCategory.MICE, new AtomicReference<>(safeParserMode(miceMode)));
    }

    @Autowired(required = false)
    public void setOllamaConnectionService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
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

    public DebugRuntimeConfig getCurrentConfig() {
        DebugRuntimeConfig config = new DebugRuntimeConfig();
        config.setResolverMode(getResolverMode().name());
        config.setGeneralParserMode(getParserMode(ChatCategory.GENERAL).name());
        config.setTravelParserMode(getParserMode(ChatCategory.TRAVEL).name());
        config.setDevParserMode(getParserMode(ChatCategory.DEV).name());
        config.setMiceParserMode(getParserMode(ChatCategory.MICE).name());
        config.setMemoryStore(normalizeMemoryStore(configuredMemoryStore));
        config.setMemoryServiceType(conversationMemoryService.getClass().getSimpleName());
        config.setFallbackPolicy(fallbackPolicy);
        config.setOllamaBaseUrl(ollamaConnectionService == null ? null : ollamaConnectionService.getBaseUrl());
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

        return getCurrentConfig();
    }

    public DebugRuntimeConfig reset() {
        setResolverMode(CategoryResolverMode.HYBRID);
        setParserMode(ChatCategory.GENERAL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.TRAVEL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.DEV, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.MICE, CategoryParserMode.HYBRID);
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
}
