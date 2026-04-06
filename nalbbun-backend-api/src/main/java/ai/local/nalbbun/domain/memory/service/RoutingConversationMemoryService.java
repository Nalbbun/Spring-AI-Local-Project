package ai.local.nalbbun.domain.memory.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.model.MemoryStoreRuntimeState;
import ai.local.nalbbun.domain.memory.model.MemoryMessage;
import ai.local.nalbbun.infra.db.memory.jdbc.JdbcConversationMemoryService;
import ai.local.nalbbun.infra.db.memory.redis.RedisConversationMemoryService;
import lombok.extern.slf4j.Slf4j;

/**
 * 메모리 저장소 라우팅 서비스다.
 *
 * <p>현재 적용 저장소(active)와 수정 요청 저장소(requested)를 분리하여 관리한다.
 * 저장소 전환 시 기존 대화는 다른 저장소로 마이그레이션하지 않는다.</p>
 */
@Slf4j
@Service
@Primary
public class RoutingConversationMemoryService implements ConversationMemoryService {

    public static final String IN_MEMORY = "in-memory";
    public static final String JDBC = "jdbc";
    public static final String REDIS = "redis";

    private final Map<String, ConversationMemoryService> delegates = new LinkedHashMap<>();
    private final MemoryStoreRuntimeStateService runtimeStateService;
    private volatile String activeStore = IN_MEMORY;
    private volatile String requestedStore = IN_MEMORY;

    public RoutingConversationMemoryService(
            InMemoryConversationMemoryService inMemoryConversationMemoryService,
            ObjectProvider<JdbcConversationMemoryService> jdbcConversationMemoryServiceProvider,
            ObjectProvider<RedisConversationMemoryService> redisConversationMemoryServiceProvider,
            MemoryStoreRuntimeStateService runtimeStateService,
            @Value("${app.memory.store:in-memory}") String configuredStore
    ) {
        this.runtimeStateService = runtimeStateService;
        delegates.put(IN_MEMORY, inMemoryConversationMemoryService);
        jdbcConversationMemoryServiceProvider.ifAvailable(service -> delegates.put(JDBC, service));
        redisConversationMemoryServiceProvider.ifAvailable(service -> delegates.put(REDIS, service));
        if (!delegates.containsKey(REDIS)) {
            log.warn("Redis 메모리 저장소 bean 이 등록되지 않아 available 목록에서 제외됩니다. Redis 연결/빈 구성을 확인하세요.");
        }

        String configured = resolveAvailableStore(normalizeStore(configuredStore));
        MemoryStoreRuntimeState state = runtimeStateService.load(configured);
        this.requestedStore = resolveAvailableStore(normalizeStore(state.getRequestedStore()));
        this.activeStore = resolveAvailableStore(normalizeStore(state.getActiveStore()));
        if (!Objects.equals(this.activeStore, this.requestedStore)) {
            this.activeStore = this.requestedStore;
            runtimeStateService.syncOnStartup(this.activeStore, this.requestedStore);
            log.info("메모리 저장소 재시작 적용 처리 완료. active={}", this.activeStore);
        } else {
            runtimeStateService.syncOnStartup(this.activeStore, this.requestedStore);
        }
        log.info("메모리 저장소 라우터 초기화 완료. active={}, requested={}, available={}", this.activeStore, this.requestedStore, delegates.keySet());
    }

    public synchronized String updateRequestedStore(String store) {
        String normalized = resolveAvailableStore(normalizeStore(store));
        this.requestedStore = normalized;
        runtimeStateService.saveRequestedStore(activeStore, normalized);
        return normalized;
    }

    public synchronized String applyRequestedStore() {
        String next = resolveAvailableStore(requestedStore);
        String current = activeStore;
        if (!Objects.equals(current, next)) {
            log.warn("메모리 저장소 전환 적용. from={} to={} (대화 마이그레이션 없음)", current, next);
            activeStore = next;
        }
        runtimeStateService.markRestartApplied(activeStore, requestedStore);
        return activeStore;
    }

    public String getActiveStore() {
        return activeStore;
    }

    public String getRequestedStore() {
        return requestedStore;
    }

    public boolean isApplyRequired() {
        return !Objects.equals(activeStore, requestedStore);
    }

    public List<String> getAvailableStores() {
        return new ArrayList<>(delegates.keySet());
    }

    public String getActiveServiceType() {
        return delegate().getClass().getSimpleName();
    }

    public String memoryStoreNotice() {
        return "저장소 전환 시 기존 대화 이력은 자동 이전되지 않습니다. 적용 이후 신규/조회 기준은 선택한 저장소를 따릅니다.";
    }

    private ConversationMemoryService delegate() {
        ConversationMemoryService service = delegates.get(activeStore);
        if (service != null) {
            return service;
        }
        return delegates.get(IN_MEMORY);
    }

    private String resolveAvailableStore(String requested) {
        if (delegates.containsKey(requested)) {
            return requested;
        }
        return IN_MEMORY;
    }

    private String normalizeStore(String value) {
        if (value == null || value.isBlank()) {
            return IN_MEMORY;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case JDBC -> JDBC;
            case REDIS -> REDIS;
            default -> IN_MEMORY;
        };
    }

    @Override
    public void addUserMessage(String conversationId, ChatCategory category, String content) {
        delegate().addUserMessage(conversationId, category, content);
    }

    @Override
    public void addAssistantMessage(String conversationId, ChatCategory category, String content) {
        delegate().addAssistantMessage(conversationId, category, content);
    }

    @Override
    public void addSystemMessage(String conversationId, ChatCategory category, String content) {
        delegate().addSystemMessage(conversationId, category, content);
    }

    @Override
    public List<MemoryMessage> recentMessages(String conversationId, int limit) {
        return delegate().recentMessages(conversationId, limit);
    }

    @Override
    public String formatRecentConversation(String conversationId, int limit) {
        return delegate().formatRecentConversation(conversationId, limit);
    }

    @Override
    public void updateCategorySummary(String conversationId, ChatCategory category, String summary) {
        delegate().updateCategorySummary(conversationId, category, summary);
    }

    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        return delegate().getCategorySummary(conversationId, category);
    }

    @Override
    public void addImportantNote(String conversationId, ChatCategory category, String note) {
        delegate().addImportantNote(conversationId, category, note);
    }

    @Override
    public List<String> getImportantNotes(String conversationId, ChatCategory category) {
        return delegate().getImportantNotes(conversationId, category);
    }

    @Override
    public ConversationMemorySnapshot snapshot(String conversationId) {
        return delegate().snapshot(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate().clear(conversationId);
    }

    @Override
    public List<String> listConversationIds() {
        return delegate().listConversationIds();
    }
}
