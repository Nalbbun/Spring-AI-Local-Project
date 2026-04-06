package ai.local.nalbbun.domain.memory.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.model.MemoryMessage;
import ai.local.nalbbun.domain.memory.model.MemorySnapshotRecord;
import ai.local.nalbbun.domain.memory.model.MemorySummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class MemorySnapshotService {

    private final ConversationMemoryService conversationMemoryService;
    private final JdbcTemplate apiJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public MemorySnapshotService(
            ConversationMemoryService conversationMemoryService,
            @Qualifier("apiJdbcTemplate") ObjectProvider<JdbcTemplate> apiJdbcTemplateProvider
    ) {
        this.conversationMemoryService = conversationMemoryService;
        this.apiJdbcTemplate = apiJdbcTemplateProvider.getIfAvailable();
    }

    public List<MemorySnapshotRecord> list(String conversationId) {
        if (apiJdbcTemplate == null) return List.of();
        return apiJdbcTemplate.query(
                "SELECT snapshot_id, conversation_id, label, snapshot_json, created_at FROM memory_snapshot WHERE conversation_id = ? ORDER BY created_at DESC, snapshot_id DESC",
                (rs, rowNum) -> new MemorySnapshotRecord(
                        rs.getLong("snapshot_id"),
                        rs.getString("conversation_id"),
                        rs.getString("label"),
                        parse(rs.getString("snapshot_json")),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()),
                conversationId);
    }

    public MemorySnapshotRecord create(String conversationId, String label) {
        if (apiJdbcTemplate == null) throw new IllegalStateException("API DB가 없어 메모리 스냅샷 기능을 사용할 수 없습니다.");
        ConversationMemorySnapshot snapshot = conversationMemoryService.snapshot(conversationId);
        String normalizedLabel = (label == null || label.isBlank()) ? ("Snapshot " + LocalDateTime.now()) : label.trim();
        apiJdbcTemplate.update(
                "INSERT INTO memory_snapshot(conversation_id, label, snapshot_json, created_at) VALUES (?, ?, ?, ?)",
                conversationId,
                normalizedLabel,
                stringify(snapshot),
                Timestamp.valueOf(LocalDateTime.now()));
        return list(conversationId).stream().findFirst().orElseThrow();
    }

    public ConversationMemorySnapshot restore(String conversationId, Long snapshotId) {
        MemorySnapshotRecord record = list(conversationId).stream()
                .filter(item -> item.snapshotId().equals(snapshotId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("스냅샷을 찾을 수 없습니다: " + snapshotId));
        ConversationMemorySnapshot snapshot = record.snapshot();
        conversationMemoryService.clear(conversationId);
        for (MemoryMessage message : snapshot.getRecentMessages()) {
            ChatCategory category = message.getCategory();
            String role = message.getRole() == null ? "user" : message.getRole().trim().toLowerCase();
            switch (role) {
                case "assistant" -> conversationMemoryService.addAssistantMessage(conversationId, category, message.getContent());
                case "system" -> conversationMemoryService.addSystemMessage(conversationId, category, message.getContent());
                default -> conversationMemoryService.addUserMessage(conversationId, category, message.getContent());
            }
        }
        if (snapshot.getCategorySummaries() != null) {
            for (MemorySummary summary : snapshot.getCategorySummaries().values()) {
                if (summary != null && summary.getCategory() != null && summary.getSummary() != null) {
                    conversationMemoryService.updateCategorySummary(conversationId, summary.getCategory(), summary.getSummary());
                }
            }
        }
        if (snapshot.getImportantNotes() != null) {
            snapshot.getImportantNotes().forEach(note -> {
                if (note != null && note.getCategory() != null && note.getNote() != null) {
                    conversationMemoryService.addImportantNote(conversationId, note.getCategory(), note.getNote());
                }
            });
        }
        return conversationMemoryService.snapshot(conversationId);
    }

    public void delete(String conversationId, Long snapshotId) {
        if (apiJdbcTemplate == null) return;
        apiJdbcTemplate.update("DELETE FROM memory_snapshot WHERE conversation_id = ? AND snapshot_id = ?", conversationId, snapshotId);
    }

    private String stringify(ConversationMemorySnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("메모리 스냅샷 직렬화에 실패했습니다.", e);
        }
    }

    private ConversationMemorySnapshot parse(String json) {
        try {
            return objectMapper.readValue(json, ConversationMemorySnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("메모리 스냅샷 역직렬화에 실패했습니다.", e);
        }
    }
}
