package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.ImportantNote;
import ai.local.nalbbun.model.common.MemoryMessage;
import ai.local.nalbbun.model.common.MemorySummary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryConversationMemoryService implements ConversationMemoryService {

    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    private final Map<String, Deque<MemoryMessage>> messageStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, MemorySummary>> summaryStore = new ConcurrentHashMap<>();
    private final Map<String, List<ImportantNote>> noteStore = new ConcurrentHashMap<>();

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
        Deque<MemoryMessage> deque = messageStore.getOrDefault(conversationId, new ArrayDeque<>());
        List<MemoryMessage> all = new ArrayList<>(deque);

        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
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
              .append("][").append(message.getCategory()).append("] ")
              .append(message.getContent())
              .append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public void updateCategorySummary(String conversationId, ChatCategory category, String summary) {
        Map<String, MemorySummary> map = summaryStore.computeIfAbsent(conversationId, key -> new ConcurrentHashMap<>());
        map.put(category.name(), new MemorySummary(category, summary, LocalDateTime.now()));
    }

    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        Map<String, MemorySummary> map = summaryStore.getOrDefault(conversationId, Map.of());
        MemorySummary summary = map.get(category.name());
        return summary == null ? "" : summary.getSummary();
    }

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

    @Override
    public void clear(String conversationId) {
        messageStore.remove(conversationId);
        summaryStore.remove(conversationId);
        noteStore.remove(conversationId);
    }

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