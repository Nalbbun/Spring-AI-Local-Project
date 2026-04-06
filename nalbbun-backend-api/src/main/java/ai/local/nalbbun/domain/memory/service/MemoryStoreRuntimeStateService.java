package ai.local.nalbbun.domain.memory.service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.memory.model.MemoryStoreRuntimeState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MemoryStoreRuntimeStateService {

    private static final long SINGLE_ID = 1L;
    private final JdbcTemplate apiJdbcTemplate;
    private final int redisSessionTtlMinutes;
    private final int redisMemoryTtlMinutes;

    public MemoryStoreRuntimeStateService(
            @Qualifier("apiJdbcTemplate") ObjectProvider<JdbcTemplate> apiJdbcTemplateProvider,
            @Value("${app.memory.redis.session-ttl-minutes:180}") int redisSessionTtlMinutes,
            @Value("${app.memory.redis.ttl-minutes:1440}") int redisMemoryTtlMinutes) {
        this.apiJdbcTemplate = apiJdbcTemplateProvider.getIfAvailable();
        this.redisSessionTtlMinutes = redisSessionTtlMinutes;
        this.redisMemoryTtlMinutes = redisMemoryTtlMinutes;
    }

    public MemoryStoreRuntimeState load(String fallbackStore) {
        if (apiJdbcTemplate == null) {
            return defaultState(fallbackStore);
        }
        try {
            ensureRow(defaultState(fallbackStore));
            return apiJdbcTemplate.query("SELECT * FROM app_runtime_state WHERE id = ?", rs -> rs.next() ? map(rs) : defaultState(fallbackStore), SINGLE_ID);
        } catch (Exception e) {
            log.warn("런타임 상태 로드 실패. 기본값으로 대체합니다. reason={}", rootMessage(e));
            return defaultState(fallbackStore);
        }
    }


    public MemoryStoreRuntimeState currentState(String fallbackStore) {
        return load(fallbackStore);
    }

    public void saveRequestedStore(String activeStore, String requestedStore) {
        save(activeStore, requestedStore, LocalDateTime.now(), null, null, "REQUESTED_STORE_UPDATED");
    }

    public void markRestartApplied(String activeStore, String requestedStore) {
        save(activeStore, requestedStore, null, LocalDateTime.now(), null, "RESTART_APPLIED");
    }

    public void syncOnStartup(String activeStore, String requestedStore) {
        save(activeStore, requestedStore, null, LocalDateTime.now(), null, "STARTUP_SYNC");
    }

    public void reset(String activeStore) {
        save(activeStore, activeStore, null, LocalDateTime.now(), null, "RESET");
    }

    public void updateRedisMemoryTtlMinutes(String activeStore, String requestedStore, Integer ttlMinutes) {
        save(activeStore, requestedStore, null, LocalDateTime.now(), ttlMinutes, "REDIS_MEMORY_TTL_UPDATED");
    }

    private void save(String activeStore, String requestedStore, LocalDateTime restartRequestedAt, LocalDateTime lastAppliedAt, Integer redisMemoryTtlMinutesOverride, String action) {
        if (apiJdbcTemplate == null) {
            return;
        }
        try {
            ensureRow(defaultState(activeStore));
            apiJdbcTemplate.update("""
                UPDATE app_runtime_state
                   SET active_memory_store = ?,
                       requested_memory_store = ?,
                       restart_requested_at = ?,
                       last_applied_at = ?,
                       redis_session_ttl_minutes = ?,
                       redis_memory_ttl_minutes = ?,
                       last_action = ?
                 WHERE id = ?
                """,
                activeStore,
                requestedStore,
                toTs(restartRequestedAt),
                toTs(lastAppliedAt),
                redisSessionTtlMinutes,
                redisMemoryTtlMinutesOverride == null ? redisMemoryTtlMinutes : Math.max(1, redisMemoryTtlMinutesOverride),
                action,
                SINGLE_ID);
        } catch (Exception e) {
            log.warn("런타임 상태 저장 실패. reason={}", rootMessage(e));
        }
    }

    private void ensureRow(MemoryStoreRuntimeState initial) {
        apiJdbcTemplate.update("""
            INSERT INTO app_runtime_state(id, active_memory_store, requested_memory_store, restart_requested_at, last_applied_at, redis_session_ttl_minutes, redis_memory_ttl_minutes, last_action)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """,
            SINGLE_ID,
            initial.getActiveStore(),
            initial.getRequestedStore(),
            toTs(initial.getRestartRequestedAt()),
            toTs(initial.getLastAppliedAt()),
            initial.getRedisSessionTtlMinutes(),
            initial.getRedisMemoryTtlMinutes(),
            initial.getLastAction());
    }

    private MemoryStoreRuntimeState defaultState(String fallbackStore) {
        return MemoryStoreRuntimeState.builder()
                .activeStore(fallbackStore)
                .requestedStore(fallbackStore)
                .redisSessionTtlMinutes(redisSessionTtlMinutes)
                .redisMemoryTtlMinutes(redisMemoryTtlMinutes)
                .lastAction("DEFAULT")
                .build();
    }

    private MemoryStoreRuntimeState map(ResultSet rs) throws java.sql.SQLException {
        return MemoryStoreRuntimeState.builder()
                .activeStore(rs.getString("active_memory_store"))
                .requestedStore(rs.getString("requested_memory_store"))
                .restartRequestedAt(toLocal(rs.getTimestamp("restart_requested_at")))
                .lastAppliedAt(toLocal(rs.getTimestamp("last_applied_at")))
                .redisSessionTtlMinutes(rs.getInt("redis_session_ttl_minutes"))
                .redisMemoryTtlMinutes(rs.getInt("redis_memory_ttl_minutes"))
                .lastAction(rs.getString("last_action"))
                .build();
    }

    private Timestamp toTs(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocal(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? throwable.getClass().getSimpleName() : cursor.getMessage();
    }
}
