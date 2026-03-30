package ai.local.nalbbun.infra.db.prompt.redis;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 기반 프롬프트 저장소.
 * Key 구조:
 *   prompt:entry:{id}   — JSON 직렬화된 PromptEntry
 *   prompt:index        — 전체 ID Set
 *   prompt:default:{category|ALL} — 기본 프롬프트 ID
 */
@Slf4j
@Repository
@ConditionalOnProperty(prefix = "app.prompt", name = "store", havingValue = "redis")
public class RedisPromptRepository implements PromptRepository {

    private static final String KEY_PREFIX  = "prompt:entry:";
    private static final String INDEX_KEY   = "prompt:index";
    private static final String DEFAULT_PRE = "prompt:default:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisPromptRepository(StringRedisTemplate redis) {
        this.redis  = redis;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public List<PromptEntry> findAll() {
        Set<String> ids = redis.opsForSet().members(INDEX_KEY);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(id -> decode(redis.opsForValue().get(KEY_PREFIX + id)))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(e -> e.getCreatedAt() == null ? "" : e.getCreatedAt().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptEntry> findByCategory(ChatCategory category) {
        return findAll().stream()
                .filter(e -> category == null || e.getCategory() == null || e.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PromptEntry> findById(String id) {
        String json = redis.opsForValue().get(KEY_PREFIX + id);
        return Optional.ofNullable(decode(json));
    }

    @Override
    public Optional<PromptEntry> findDefault(ChatCategory category) {
        String key  = DEFAULT_PRE + (category == null ? "ALL" : category.name());
        String defId = redis.opsForValue().get(key);
        if (defId == null) return Optional.empty();
        return findById(defId).filter(PromptEntry::isActive);
    }

    @Override
    public PromptEntry save(PromptEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) {
            entry.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        if (entry.isDefault()) clearDefault(entry.getCategory());
        persist(entry);
        return entry;
    }

    @Override
    public PromptEntry update(PromptEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        if (entry.isDefault()) clearDefault(entry.getCategory());
        persist(entry);
        return entry;
    }

    @Override
    public void delete(String id) {
        findById(id).ifPresent(e -> {
            redis.delete(KEY_PREFIX + id);
            redis.opsForSet().remove(INDEX_KEY, id);
            String defKey = DEFAULT_PRE + (e.getCategory() == null ? "ALL" : e.getCategory().name());
            String defId  = redis.opsForValue().get(defKey);
            if (id.equals(defId)) redis.delete(defKey);
        });
    }

    @Override
    public void clearDefault(ChatCategory category) {
        String key = DEFAULT_PRE + (category == null ? "ALL" : category.name());
        String defId = redis.opsForValue().get(key);
        if (defId != null) {
            findById(defId).ifPresent(e -> { e.setDefault(false); persist(e); });
            redis.delete(key);
        }
    }

    // ── 내부 유틸 ──────────────────────────────────────
    private void persist(PromptEntry entry) {
        try {
            redis.opsForValue().set(KEY_PREFIX + entry.getId(), mapper.writeValueAsString(entry));
            redis.opsForSet().add(INDEX_KEY, entry.getId());
            if (entry.isDefault()) {
                String key = DEFAULT_PRE + (entry.getCategory() == null ? "ALL" : entry.getCategory().name());
                redis.opsForValue().set(key, entry.getId());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 프롬프트 직렬화 실패", e);
        }
    }

    private PromptEntry decode(String json) {
        if (json == null || json.isBlank()) return null;
        try { return mapper.readValue(json, PromptEntry.class); }
        catch (Exception e) { log.warn("Redis 프롬프트 역직렬화 실패: {}", e.getMessage()); return null; }
    }
}
