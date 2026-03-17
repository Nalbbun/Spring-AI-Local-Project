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

/**
 * DebugRuntimeConfigService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: debug runtime config service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class DebugRuntimeConfigService {

    /** resolverMode 값을 보관한다. */
    private final AtomicReference<CategoryResolverMode> resolverMode;
    /** parserModes 값을 보관한다. */
    private final Map<ChatCategory, AtomicReference<CategoryParserMode>> parserModes =
            new EnumMap<>(ChatCategory.class);
    /** configuredMemoryStore 값을 보관한다. */
    private final String configuredMemoryStore;
    /** fallbackPolicy 값을 보관한다. */
    private final String fallbackPolicy;
    /** conversationMemoryService 값을 보관한다. */
    private final ConversationMemoryService conversationMemoryService;

    /** ollamaConnectionService 값을 보관한다. */
    private DebugRuntimeOllamaConnectionService ollamaConnectionService;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param resolverMode resolverMode 값
     * @param generalMode generalMode 값
     * @param travelMode travelMode 값
     * @param devMode devMode 값
     * @param miceMode miceMode 값
     * @param configuredMemoryStore configuredMemoryStore 값
     * @param fallbackPolicy fallbackPolicy 값
     * @param conversationMemoryService conversationMemoryService 값
     */
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

    /**
     * 대상 값을 설정한다.
     *
     * @param ollamaConnectionService ollamaConnectionService 값
     */
    @Autowired(required = false)
    public void setOllamaConnectionService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
    }

    /**
     * 지정된 정보를 조회한다.
     * @return CategoryResolverMode 타입의 처리 결과
     */
    public CategoryResolverMode getResolverMode() {
        return resolverMode.get();
    }

    /**
     * 대상 값을 설정한다.
     *
     * @param mode mode 값
     */
    public void setResolverMode(CategoryResolverMode mode) {
        if (mode != null) {
            resolverMode.set(mode);
        }
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param category 대상 카테고리 정보
     * @return CategoryParserMode 타입의 처리 결과
     */
    public CategoryParserMode getParserMode(ChatCategory category) {
        AtomicReference<CategoryParserMode> ref = parserModes.get(category);
        return ref == null ? CategoryParserMode.HYBRID : ref.get();
    }

    /**
     * 대상 값을 설정한다.
     *
     * @param category 대상 카테고리 정보
     * @param mode mode 값
     */
    public void setParserMode(ChatCategory category, CategoryParserMode mode) {
        if (category == null || mode == null) {
            return;
        }
        parserModes.computeIfAbsent(category, key -> new AtomicReference<>(CategoryParserMode.HYBRID))
                   .set(mode);
    }

    /**
     * 지정된 정보를 조회한다.
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
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

    /**
     * 대상 값을 갱신한다.
     *
     * @param request HTTP 요청 객체
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
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

    /**
     * reset 기능을 수행한다.
     * @return DebugRuntimeConfig 타입의 처리 결과
     */
    public DebugRuntimeConfig reset() {
        setResolverMode(CategoryResolverMode.HYBRID);
        setParserMode(ChatCategory.GENERAL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.TRAVEL, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.DEV, CategoryParserMode.HYBRID);
        setParserMode(ChatCategory.MICE, CategoryParserMode.HYBRID);
        return getCurrentConfig();
    }

    /**
     * 조건 충족 여부를 확인한다.
     *
     * @param value value 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * normalizeMemoryStore 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String normalizeMemoryStore(String value) {
        return hasText(value) ? value.trim().toLowerCase() : "in-memory";
    }

    /**
     * normalizeFallbackPolicy 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String normalizeFallbackPolicy(String value) {
        return hasText(value) ? value.trim().toUpperCase() : "BLOCK_OPENAI";
    }

    /**
     * safeResolverMode 기능을 수행한다.
     *
     * @param value value 값
     * @return CategoryResolverMode 타입의 처리 결과
     */
    private CategoryResolverMode safeResolverMode(String value) {
        try {
            return CategoryResolverMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryResolverMode.HYBRID;
        }
    }

    /**
     * safeParserMode 기능을 수행한다.
     *
     * @param value value 값
     * @return CategoryParserMode 타입의 처리 결과
     */
    private CategoryParserMode safeParserMode(String value) {
        try {
            return CategoryParserMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryParserMode.HYBRID;
        }
    }
}
