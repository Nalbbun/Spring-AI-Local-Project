package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.ImportantNote;
import ai.local.nalbbun.model.common.MemoryMessage;
import ai.local.nalbbun.model.common.MemorySummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryConversationMemoryService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: in memory conversation memory service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryConversationMemoryService implements ConversationMemoryService {

    /** MAX_MESSAGES_PER_CONVERSATION 값을 보관한다. */
    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    /** MAX_NOTES_PER_CONVERSATION 값을 보관한다. */
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    /** messageStore 값을 보관한다. */
    private final Map<String, Deque<MemoryMessage>> messageStore = new ConcurrentHashMap<>();
    /** summaryStore 값을 보관한다. */
    private final Map<String, Map<String, MemorySummary>> summaryStore = new ConcurrentHashMap<>();
    /** noteStore 값을 보관한다. */
    private final Map<String, List<ImportantNote>> noteStore = new ConcurrentHashMap<>();

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
        Deque<MemoryMessage> deque = messageStore.getOrDefault(conversationId, new ArrayDeque<>());
        List<MemoryMessage> all = new ArrayList<>(deque);

        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
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
              .append("][").append(message.getCategory()).append("] ")
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
        Map<String, MemorySummary> map = summaryStore.computeIfAbsent(conversationId, key -> new ConcurrentHashMap<>());
        map.put(category.name(), new MemorySummary(category, summary, LocalDateTime.now()));
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
        Map<String, MemorySummary> map = summaryStore.getOrDefault(conversationId, Map.of());
        MemorySummary summary = map.get(category.name());
        return summary == null ? "" : summary.getSummary();
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
        List<ImportantNote> notes = noteStore.computeIfAbsent(conversationId, key -> new ArrayList<>());
        synchronized (notes) {
            notes.add(new ImportantNote(category, note, LocalDateTime.now()));
            while (notes.size() > MAX_NOTES_PER_CONVERSATION) {
                notes.remove(0);
            }
        }
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
        List<ImportantNote> notes = noteStore.getOrDefault(conversationId, List.of());
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
        Map<String, MemorySummary> summaries = summaryStore.getOrDefault(conversationId, Map.of());
        List<ImportantNote> notes = noteStore.getOrDefault(conversationId, List.of());

        return new ConversationMemorySnapshot(
                conversationId,
                recentMessages(conversationId, 20),
                new LinkedHashMap<>(summaries),
                new ArrayList<>(notes)
        );
    }

    /**
     * clear 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     */
    @Override
    public void clear(String conversationId) {
        messageStore.remove(conversationId);
        summaryStore.remove(conversationId);
        noteStore.remove(conversationId);
    }

    /**
     * appendMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param message 사용자 입력 또는 질의 내용
     */
    private void appendMessage(String conversationId, MemoryMessage message) {
        Deque<MemoryMessage> deque = messageStore.computeIfAbsent(conversationId, key -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(message);
            while (deque.size() > MAX_MESSAGES_PER_CONVERSATION) {
                deque.removeFirst();
            }
        }
    } 
}