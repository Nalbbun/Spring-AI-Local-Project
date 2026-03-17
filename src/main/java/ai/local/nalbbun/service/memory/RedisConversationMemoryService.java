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
 * Redis Conversation Memory Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
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

    /**
     * Redis Conversation Memory Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * add User Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void addUserMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("user", content, category, LocalDateTime.now()));
    }

    /**
     * add Assistant Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void addAssistantMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("assistant", content, category, LocalDateTime.now()));
    }

    /**
     * add System Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void addSystemMessage(String conversationId, ChatCategory category, String content) {
        appendMessage(conversationId, new MemoryMessage("system", content, category, LocalDateTime.now()));
    }

    /**
     * recent Messages 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * format Recent Conversation 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * update Category Summary 작업을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void updateCategorySummary(String conversationId, ChatCategory category, String summary) {
        MemorySummary payload = new MemorySummary(category, summary, LocalDateTime.now());
        redisTemplate.opsForHash().put(summariesKey(conversationId), category.name(), encode(payload));
        touch(conversationId);
    }

    /**
     * Category Summary 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * add Important Note 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void addImportantNote(String conversationId, ChatCategory category, String note) {
        ImportantNote payload = new ImportantNote(category, note, LocalDateTime.now());
        redisTemplate.opsForList().rightPush(notesKey(conversationId), encode(payload));
        redisTemplate.opsForList().trim(notesKey(conversationId), -MAX_NOTES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    /**
     * Important Notes 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(List.of(messagesKey(conversationId), summariesKey(conversationId), notesKey(conversationId)));
    }

    /**
     * append Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void appendMessage(String conversationId, MemoryMessage message) {
        redisTemplate.opsForList().rightPush(messagesKey(conversationId), encode(message));
        redisTemplate.opsForList().trim(messagesKey(conversationId), -MAX_MESSAGES_PER_CONVERSATION, -1);
        touch(conversationId);
    }

    /**
     * touch 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    private void touch(String conversationId) {
        redisTemplate.expire(messagesKey(conversationId), ttl);
        redisTemplate.expire(summariesKey(conversationId), ttl);
        redisTemplate.expire(notesKey(conversationId), ttl);
    }

    /**
     * messages Key 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String messagesKey(String conversationId) {
        return "conv:" + conversationId + ":messages";
    }

    /**
     * summaries Key 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String summariesKey(String conversationId) {
        return "conv:" + conversationId + ":summaries";
    }

    /**
     * notes Key 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String notesKey(String conversationId) {
        return "conv:" + conversationId + ":notes";
    }

    /**
     * encode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private <T> T decode(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 메모리 역직렬화에 실패했습니다.", e);
        }
    }

    /**
     * decode List 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
