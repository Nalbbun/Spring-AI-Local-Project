package ai.local.nalbbun.admin.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.CategoryParserMode;
import ai.local.nalbbun.domain.category.CategoryResolverMode;
import ai.local.nalbbun.admin.model.DebugRuntimeConfig;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;

/**
 * Debug Runtime Config Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class DebugRuntimeConfigService {

    private final AtomicReference<CategoryResolverMode> resolverMode;
    private final Map<ChatCategory, AtomicReference<CategoryParserMode>> parserModes =
            new EnumMap<>(ChatCategory.class);
    private final String configuredMemoryStore;
    private final String fallbackPolicy;
    private final ConversationMemoryService conversationMemoryService;

    private DebugRuntimeOllamaConnectionService ollamaConnectionService;

    /**
     * Debug Runtime Config Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Ollama Connection Service 값을 설정한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Autowired(required = false)
    public void setOllamaConnectionService(DebugRuntimeOllamaConnectionService ollamaConnectionService) {
        this.ollamaConnectionService = ollamaConnectionService;
    }

    /**
     * Resolver Mode 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public CategoryResolverMode getResolverMode() {
        return resolverMode.get();
    }

    /**
     * Resolver Mode 값을 설정한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void setResolverMode(CategoryResolverMode mode) {
        if (mode != null) {
            resolverMode.set(mode);
        }
    }

    /**
     * Parser Mode 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public CategoryParserMode getParserMode(ChatCategory category) {
        AtomicReference<CategoryParserMode> ref = parserModes.get(category);
        return ref == null ? CategoryParserMode.HYBRID : ref.get();
    }

    /**
     * Parser Mode 값을 설정한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void setParserMode(ChatCategory category, CategoryParserMode mode) {
        if (category == null || mode == null) {
            return;
        }
        parserModes.computeIfAbsent(category, key -> new AtomicReference<>(CategoryParserMode.HYBRID))
                   .set(mode);
    }

    /**
     * Current Config 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * update 작업을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * has Text 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * normalize Memory Store 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String normalizeMemoryStore(String value) {
        return hasText(value) ? value.trim().toLowerCase() : "in-memory";
    }

    /**
     * normalize Fallback Policy 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String normalizeFallbackPolicy(String value) {
        return hasText(value) ? value.trim().toUpperCase() : "BLOCK_OPENAI";
    }

    /**
     * safe Resolver Mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private CategoryResolverMode safeResolverMode(String value) {
        try {
            return CategoryResolverMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryResolverMode.HYBRID;
        }
    }

    /**
     * safe Parser Mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private CategoryParserMode safeParserMode(String value) {
        try {
            return CategoryParserMode.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return CategoryParserMode.HYBRID;
        }
    }
}
