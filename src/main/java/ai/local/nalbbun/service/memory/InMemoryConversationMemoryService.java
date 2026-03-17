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
 * In Memory Conversation Memory Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryConversationMemoryService implements ConversationMemoryService {

    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    private final Map<String, Deque<MemoryMessage>> messageStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, MemorySummary>> summaryStore = new ConcurrentHashMap<>();
    private final Map<String, List<ImportantNote>> noteStore = new ConcurrentHashMap<>();

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
        Deque<MemoryMessage> deque = messageStore.getOrDefault(conversationId, new ArrayDeque<>());
        List<MemoryMessage> all = new ArrayList<>(deque);

        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
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
              .append("][").append(message.getCategory()).append("] ")
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
        Map<String, MemorySummary> map = summaryStore.computeIfAbsent(conversationId, key -> new ConcurrentHashMap<>());
        map.put(category.name(), new MemorySummary(category, summary, LocalDateTime.now()));
    }

    /**
     * Category Summary 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        Map<String, MemorySummary> map = summaryStore.getOrDefault(conversationId, Map.of());
        MemorySummary summary = map.get(category.name());
        return summary == null ? "" : summary.getSummary();
    }

    /**
     * add Important Note 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * Important Notes 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    @Override
    public void clear(String conversationId) {
        messageStore.remove(conversationId);
        summaryStore.remove(conversationId);
        noteStore.remove(conversationId);
    }

    /**
     * append Message 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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