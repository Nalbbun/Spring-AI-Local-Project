package ai.local.nalbbun.admin.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.admin.model.DebugRuntimeConfig;
import ai.local.nalbbun.domain.category.CategoryParserMode;
import ai.local.nalbbun.domain.category.CategoryResolverMode;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.category.model.ExecutionMode;
import ai.local.nalbbun.domain.memory.model.MemoryStoreRuntimeState;
import ai.local.nalbbun.domain.memory.service.MemoryStoreRuntimeStateService;
import ai.local.nalbbun.domain.memory.service.RoutingConversationMemoryService;
import ai.local.nalbbun.domain.runtime.port.RuntimeCategoryPolicyPort;

@Service
public class DebugRuntimeConfigService implements RuntimeCategoryPolicyPort {

    private final AtomicReference<CategoryResolverMode> resolverMode;
    private final Map<ChatCategory, AtomicReference<CategoryParserMode>> parserModes = new EnumMap<>(ChatCategory.class);
    private final Map<ChatCategory, AtomicReference<ExecutionMode>> executionModes = new EnumMap<>(ChatCategory.class);
    private final String fallbackPolicy;
    private final RoutingConversationMemoryService routingConversationMemoryService;
    private final MemoryStoreRuntimeStateService runtimeStateService;

    private DebugRuntimeOllamaConnectionService ollamaConnectionService;

    public DebugRuntimeConfigService(
            @Value("${app.category.resolver.mode:HYBRID}") String resolverMode,
            @Value("${app.parser.general.mode:HYBRID}") String generalMode,
            @Value("${app.parser.travel.mode:HYBRID}") String travelMode,
            @Value("${app.parser.dev.mode:HYBRID}") String devMode,
            @Value("${app.parser.mice.mode:HYBRID}") String miceMode,
            @Value("${app.execution.general.mode:CHAT}") String generalExecutionMode,
            @Value("${app.execution.travel.mode:AGENT}") String travelExecutionMode,
            @Value("${app.execution.dev.mode:RAG}") String devExecutionMode,
            @Value("${app.execution.mice.mode:CHAT}") String miceExecutionMode,
            @Value("${app.llm.fallback-policy:BLOCK_OPENAI}") String fallbackPolicy,
            RoutingConversationMemoryService routingConversationMemoryService,
            MemoryStoreRuntimeStateService runtimeStateService
    ) {
        this.resolverMode = new AtomicReference<>(safeResolverMode(resolverMode));
        this.fallbackPolicy = normalizeFallbackPolicy(fallbackPolicy);
        this.routingConversationMemoryService = routingConversationMemoryService;
        this.runtimeStateService = runtimeStateService;

        parserModes.put(ChatCategory.GENERAL, new AtomicReference<>(safeParserMode(generalMode)));
        parserModes.put(ChatCategory.TRAVEL, new AtomicReference<>(safeParserMode(travelMode)));
        parserModes.put(ChatCategory.DEV, new AtomicReference<>(safeParserMode(devMode)));
        parserModes.put(ChatCategory.MICE, new AtomicReference<>(safeParserMode(miceMode)));

        executionModes.put(ChatCategory.GENERAL, new AtomicReference<>(safeExecutionMode(generalExecutionMode, ExecutionMode.CHAT)));
        executionModes.put(ChatCategory.TRAVEL, new AtomicReference<>(safeExecutionMode(travelExecutionMode, ExecutionMode.AGENT)));
        executionModes.put(ChatCategory.DEV, new AtomicReference<>(safeExecutionMode(devExecutionMode, ExecutionMode.RAG)));
        executionModes.put(ChatCategory.MICE, new AtomicReference<>(safeExecutionMode(miceExecutionMode, ExecutionMode.CHAT)));
    }

    @Autowired(required = false)
    public void setOllamaConnectionService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
    }

    public DebugRuntimeConfig getCurrentConfig() {
        MemoryStoreRuntimeState state = runtimeStateService.currentState(routingConversationMemoryService.getActiveStore());

        DebugRuntimeConfig config = new DebugRuntimeConfig();
        config.setResolverMode(getResolverMode().name());
        config.setGeneralParserMode(getParserMode(ChatCategory.GENERAL).name());
        config.setTravelParserMode(getParserMode(ChatCategory.TRAVEL).name());
        config.setDevParserMode(getParserMode(ChatCategory.DEV).name());
        config.setMiceParserMode(getParserMode(ChatCategory.MICE).name());
        config.setGeneralExecutionMode(getDefaultExecutionMode(ChatCategory.GENERAL).name());
        config.setDevExecutionMode(getDefaultExecutionMode(ChatCategory.DEV).name());
        config.setMiceExecutionMode(getDefaultExecutionMode(ChatCategory.MICE).name());
        config.setTravelExecutionMode(getDefaultExecutionMode(ChatCategory.TRAVEL).name());
        config.setMemoryStore(state.getRequestedStore());
        config.setActiveMemoryStore(routingConversationMemoryService.getActiveStore());
        config.setRequestedMemoryStore(state.getRequestedStore());
        config.setMemoryServiceType(routingConversationMemoryService.getActiveServiceType());
        config.setRestartRequired(routingConversationMemoryService.isApplyRequired());
        config.setRestartSupported(true);
        config.setMemoryStoreNotice(routingConversationMemoryService.memoryStoreNotice());
        config.setAvailableMemoryStores(routingConversationMemoryService.getAvailableStores());
        config.setRestartRequestedAt(state.getRestartRequestedAt());
        config.setLastAppliedAt(state.getLastAppliedAt());
        config.setRedisSessionTtlMinutes(state.getRedisSessionTtlMinutes());
        config.setRedisMemoryTtlMinutes(state.getRedisMemoryTtlMinutes());
        config.setRestartAction(state.getLastAction());
        config.setFallbackPolicy(fallbackPolicy);
        config.setOllamaBaseUrl(ollamaConnectionService == null ? null : ollamaConnectionService.getBaseUrl());
        return config;
    }

    public DebugRuntimeConfig update(DebugRuntimeConfig request) {
        if (request == null) {
            return getCurrentConfig();
        }

        if (hasText(request.getResolverMode())) setResolverMode(safeResolverMode(request.getResolverMode()));
        if (hasText(request.getGeneralParserMode())) setParserMode(ChatCategory.GENERAL, safeParserMode(request.getGeneralParserMode()));
        if (hasText(request.getTravelParserMode())) setParserMode(ChatCategory.TRAVEL, safeParserMode(request.getTravelParserMode()));
        if (hasText(request.getDevParserMode())) setParserMode(ChatCategory.DEV, safeParserMode(request.getDevParserMode()));
        if (hasText(request.getMiceParserMode())) setParserMode(ChatCategory.MICE, safeParserMode(request.getMiceParserMode()));

        if (hasText(request.getGeneralExecutionMode())) setExecutionMode(ChatCategory.GENERAL, safeExecutionMode(request.getGeneralExecutionMode(), ExecutionMode.CHAT));
        if (hasText(request.getTravelExecutionMode())) setExecutionMode(ChatCategory.TRAVEL, safeExecutionMode(request.getTravelExecutionMode(), ExecutionMode.AGENT));
        if (hasText(request.getDevExecutionMode())) setExecutionMode(ChatCategory.DEV, safeExecutionMode(request.getDevExecutionMode(), ExecutionMode.RAG));
        if (hasText(request.getMiceExecutionMode())) setExecutionMode(ChatCategory.MICE, safeExecutionMode(request.getMiceExecutionMode(), ExecutionMode.CHAT));

        String requestedMemoryStore = hasText(request.getRequestedMemoryStore()) ? request.getRequestedMemoryStore() : request.getMemoryStore();
        if (hasText(requestedMemoryStore)) {
            routingConversationMemoryService.updateRequestedStore(requestedMemoryStore);
        }
        if (request.getRedisMemoryTtlMinutes() != null) {
            runtimeStateService.updateRedisMemoryTtlMinutes(
                    routingConversationMemoryService.getActiveStore(),
                    routingConversationMemoryService.getRequestedStore(),
                    request.getRedisMemoryTtlMinutes());
        }
        return getCurrentConfig();
    }

    public DebugRuntimeConfig applyRequestedMemoryStore() {
        routingConversationMemoryService.applyRequestedStore();
        return getCurrentConfig();
    }

    public DebugRuntimeConfig reset() {
        setResolverMode(CategoryResolverMode.HYBRID);
        setParserMode(ChatCategory.GENERAL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.TRAVEL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.DEV, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.MICE, CategoryParserMode.HYBRID);
        setExecutionMode(ChatCategory.GENERAL, ExecutionMode.CHAT);
        setExecutionMode(ChatCategory.TRAVEL, ExecutionMode.AGENT);
        setExecutionMode(ChatCategory.DEV, ExecutionMode.RAG);
        setExecutionMode(ChatCategory.MICE, ExecutionMode.CHAT);
        routingConversationMemoryService.updateRequestedStore(routingConversationMemoryService.getActiveStore());
        runtimeStateService.reset(routingConversationMemoryService.getActiveStore());
        return getCurrentConfig();
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
        if (category == null || mode == null) return;
        parserModes.computeIfAbsent(category, key -> new AtomicReference<>(CategoryParserMode.HYBRID)).set(mode);
    }

    @Override
    public ExecutionMode getDefaultExecutionMode(ChatCategory category) {
        AtomicReference<ExecutionMode> ref = executionModes.get(category);
        if (ref != null) {
            return ref.get();
        }
        return switch (category == null ? ChatCategory.GENERAL : category) {
            case DEV -> ExecutionMode.RAG;
            case TRAVEL -> ExecutionMode.AGENT;
            case GENERAL, MICE -> ExecutionMode.CHAT;
        };
    }

    public void setExecutionMode(ChatCategory category, ExecutionMode mode) {
        if (category == null || mode == null) return;
        executionModes.computeIfAbsent(category, key -> new AtomicReference<>(getDefaultExecutionMode(key))).set(mode);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private ExecutionMode safeExecutionMode(String value, ExecutionMode fallback) {
        return ExecutionMode.from(value, fallback);
    }
}
