package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.ImportantNote;
import ai.local.nalbbun.model.common.MemoryMessage;
import ai.local.nalbbun.model.common.MemorySummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

/**
 * RedisConversationMemoryService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: redis conversation memory service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@Primary
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "redis")
public class RedisConversationMemoryService implements ConversationMemoryService {

    /** MAX_MESSAGES_PER_CONVERSATION 값을 보관한다. */
    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    /** MAX_NOTES_PER_CONVERSATION 값을 보관한다. */
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    /** redisTemplate 값을 보관한다. */
    private final StringRedisTemplate redisTemplate;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper;
    /** ttl 값을 보관한다. */
    private final Duration ttl;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param redisTemplate redisTemplate 값
     * @param ttlHours ttlHours 값
     */
    public RedisConversationMemoryService(
            StringRedisTemplate redisTemplate,
            @Value("${app.memory.redis.ttl-hours:24}") long ttlHours
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.ttl = Duration.ofHours(Math.max(1, ttlHours));
    }

    /**
     * addUserMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    @Override
    public void addUserMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("user", content, category, LocalDateTime.now()));
    }

    /**
     * addAssistantMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    @Override
    public void addAssistantMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("assistant", content, category, LocalDateTime.now()));
    }

    /**
     * addSystemMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    @Override
    public void addSystemMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("system", content, category, LocalDateTime.now()));
    }

    /**
     * recentMessages 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param limit limit 값
     * @return 조회 또는 생성된 목록
     */
    @Override
    public List<MemoryMessage> recentMessages(String conversationId, int limit) {
        List<String> encoded = redisTemplate.opsForList().range(messagesKey(conversationId), 0, -1);
        List<MemoryMessage> all = decodeList(encoded, new TypeReference<MemoryMessage>() {});
        if (all.size() <= limit) {
            return all;
        }
        return new ArrayList<>(all.subList(all.size() - limit, all.size()));
    }

    /**
     * formatRecentConversation 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param limit limit 값
     * @return 처리 결과 문자열
     */
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

    /**
     * 대상 값을 갱신한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param summary summary 값
     */
    @Override
    public void updateCategorySummary(String conversationId, ChatCategory category, String summary) {
        MemorySummary payload = new MemorySummary(category, summary, LocalDateTime.now());
        redisTemplate.opsForHash().put(summariesKey(conversationId), category.name(), encode(payload));
        touch(conversationId);
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @return 처리 결과 문자열
     */
    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        Object raw = redisTemplate.opsForHash().get(summariesKey(conversationId), category.name());
        if (!(raw instanceof String encoded) || encoded.isBlank()) {
            return "";
        }
        MemorySummary summary = decode(encoded, MemorySummary.class);
        return summary == null ? "" : Objects.toString(summary.getSummary(), "");
    }

    /**
     * addImportantNote 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param note note 값
     */
    @Override
    public void addImportantNote(String conversationId, ChatCategory category, String note) {
        ImportantNote payload = new ImportantNote(category, note, LocalDateTime.now());
        redisTemplate.opsForList().rightPush(notesKey(conversationId), encode(payload));
        redisTemplate.opsForList().trim(notesKey(conversationId), -MAX_NOTES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    /**
     * 지정된 정보를 조회한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @return 조회 또는 생성된 목록
     */
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

    /**
     * snapshot 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @return ConversationMemorySnapshot 타입의 처리 결과
     */
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

    /**
     * clear 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     */
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(List.of(messagesKey(conversationId), summariesKey(conversationId), notesKey(conversationId)));
    }

    /**
     * appendMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param message 사용자 입력 또는 질의 내용
     */
    private void appendMessage(String conversationId, MemoryMessage message) {
        redisTemplate.opsForList().rightPush(messagesKey(conversationId), encode(message));
        redisTemplate.opsForList().trim(messagesKey(conversationId), -MAX_MESSAGES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    /**
     * 현재 상태를 다른 표현 형태로 변환한다.
     *
     * @param conversationId 대화 식별자
     */
    private void touch(String conversationId) {
        redisTemplate.expire(messagesKey(conversationId), ttl);
        redisTemplate.expire(summariesKey(conversationId), ttl);
        redisTemplate.expire(notesKey(conversationId), ttl);
    }

    /**
     * messagesKey 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @return 처리 결과 문자열
     */
    private String messagesKey(String conversationId) {
        return "conv:" + conversationId + ":messages";
    }

    /**
     * summariesKey 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @return 처리 결과 문자열
     */
    private String summariesKey(String conversationId) {
        return "conv:" + conversationId + ":summaries";
    }

    /**
     * notesKey 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @return 처리 결과 문자열
     */
    private String notesKey(String conversationId) {
        return "conv:" + conversationId + ":notes";
    }

    /**
     * encode 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 메모리 직렬화에 실패했습니다.", e);
        }
    }

    /**
     * decode 기능을 수행한다.
     *
     * @param value value 값
     * @param type type 값
     * @return T 타입의 처리 결과
     */
    private <T> T decode(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 메모리 역직렬화에 실패했습니다.", e);
        }
    }

    /**
     * decodeList 기능을 수행한다.
     *
     * @param values values 목록 정보
     * @param typeReference typeReference 값
     * @return 조회 또는 생성된 목록
     */
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
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Redis 메모리 목록 역직렬화에 실패했습니다.", e);
            }
        }
        return result;
    }
}
