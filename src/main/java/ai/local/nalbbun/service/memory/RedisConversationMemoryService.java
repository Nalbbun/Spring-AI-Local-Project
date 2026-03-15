package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.ImportantNote;
import ai.local.nalbbun.model.common.MemoryMessage;
import ai.local.nalbbun.model.common.MemorySummary;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Primary
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "redis")
public class RedisConversationMemoryService implements ConversationMemoryService {

    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisConversationMemoryService(
            StringRedisTemplate redisTemplate,
            @Value("${app.memory.redis.ttl-hours:24}") long ttlHours
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
        this.ttl = Duration.ofHours(Math.max(1, ttlHours));
    }

    @Override
    public void addUserMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("user", content, category, LocalDateTime.now()));
    }

    @Override
    public void addAssistantMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("assistant", content, category, LocalDateTime.now()));
    }

    @Override
    public void addSystemMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("system", content, category, LocalDateTime.now()));
    }

    @Override
    public List<MemoryMessage> recentMessages(String conversationId, int limit) {
        List<String> encoded = redisTemplate.opsForList().range(messagesKey(conversationId), 0, -1);
        List<MemoryMessage> all = decodeList(encoded, new TypeReference<MemoryMessage>() {});
        if (all.size() <= limit) {
            return all;
        }
        return new ArrayList<>(all.subList(all.size() - limit, all.size()));
    }

    @Override
    public String formatRecentConversation(String conversationId, int limit) {
        List<MemoryMessage> messages = recentMessages(conversationId, limit);
        if (messages.isEmpty()) {
            return "(이전 대화 없음)";
        }

        StringBuilder sb = new StringBuilder();
        for (MemoryMessage message : messages) {
            sb.append("[")
                    .append(message.getRole())
                    .append("][")
                    .append(message.getCategory())
                    .append("] ")
                    .append(message.getContent())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public void updateCategorySummary(String conversationId, ChatCategory category, String summary) {
        MemorySummary payload = new MemorySummary(category, summary, LocalDateTime.now());
        redisTemplate.opsForHash().put(summariesKey(conversationId), category.name(), encode(payload));
        touch(conversationId);
    }

    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        Object raw = redisTemplate.opsForHash().get(summariesKey(conversationId), category.name());
        if (!(raw instanceof String encoded) || encoded.isBlank()) {
            return "";
        }
        MemorySummary summary = decode(encoded, MemorySummary.class);
        return summary == null ? "" : Objects.toString(summary.getSummary(), "");
    }

    @Override
    public void addImportantNote(String conversationId, ChatCategory category, String note) {
        ImportantNote payload = new ImportantNote(category, note, LocalDateTime.now());
        redisTemplate.opsForList().rightPush(notesKey(conversationId), encode(payload));
        redisTemplate.opsForList().trim(notesKey(conversationId), -MAX_NOTES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    @Override
    public List<String> getImportantNotes(String conversationId, ChatCategory category) {
        List<String> encoded = redisTemplate.opsForList().range(notesKey(conversationId), 0, -1);
        List<ImportantNote> notes = decodeList(encoded, new TypeReference<ImportantNote>() {});
        List<String> result = new ArrayList<>();
        for (ImportantNote note : notes) {
            if (note.getCategory() == category) {
                result.add(note.getNote());
            }
        }
        return result;
    }

    @Override
    public ConversationMemorySnapshot snapshot(String conversationId) {
        Map<Object, Object> summaryEntries = redisTemplate.opsForHash().entries(summariesKey(conversationId));
        Map<String, MemorySummary> summaries = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : summaryEntries.entrySet()) {
            if (entry.getKey() instanceof String key && entry.getValue() instanceof String encoded) {
                MemorySummary summary = decode(encoded, MemorySummary.class);
                if (summary != null) {
                    summaries.put(key, summary);
                }
            }
        }

        List<String> encodedNotes = redisTemplate.opsForList().range(notesKey(conversationId), 0, -1);
        List<ImportantNote> notes = decodeList(encodedNotes, new TypeReference<ImportantNote>() {});

        return new ConversationMemorySnapshot(
                conversationId,
                recentMessages(conversationId, 20),
                summaries,
                notes
        );
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(List.of(messagesKey(conversationId), summariesKey(conversationId), notesKey(conversationId)));
    }

    private void appendMessage(String conversationId, MemoryMessage message) {
        redisTemplate.opsForList().rightPush(messagesKey(conversationId), encode(message));
        redisTemplate.opsForList().trim(messagesKey(conversationId), -MAX_MESSAGES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    private void touch(String conversationId) {
        redisTemplate.expire(messagesKey(conversationId), ttl);
        redisTemplate.expire(summariesKey(conversationId), ttl);
        redisTemplate.expire(notesKey(conversationId), ttl);
    }

    private String messagesKey(String conversationId) {
        return "conv:" + conversationId + ":messages";
    }

    private String summariesKey(String conversationId) {
        return "conv:" + conversationId + ":summaries";
    }

    private String notesKey(String conversationId) {
        return "conv:" + conversationId + ":notes";
    }

    private String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Redis 메모리 직렬화에 실패했습니다.", e);
        }
    }

    private <T> T decode(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("Redis 메모리 역직렬화에 실패했습니다.", e);
        }
    }

    private <T> List<T> decodeList(List<String> values, TypeReference<T> typeReference) {
        List<T> result = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return result;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                result.add(objectMapper.readValue(value, objectMapper.constructType(typeReference)));
            } catch (Exception e) {
                throw new IllegalStateException("Redis 메모리 목록 역직렬화에 실패했습니다.", e);
            }
        }
        return result;
    }
}
