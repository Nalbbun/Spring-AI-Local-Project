package ai.local.nalbbun.infra.db.memory.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.model.ImportantNote;
import ai.local.nalbbun.domain.memory.model.MemoryMessage;
import ai.local.nalbbun.domain.memory.model.MemorySummary;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;

@Service
@Primary
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "app.memory", name = "store", havingValue = "jdbc")
/**
 * JDBC 기반 대화 메모리 저장소 서비스다.
 * 스키마 생성은 Flyway(db/migration)에서 관리한다.
 */
public class JdbcConversationMemoryService implements ConversationMemoryService {

    private static final int MAX_MESSAGES_PER_CONVERSATION = 50;
    private static final int MAX_NOTES_PER_CONVERSATION = 20;

    private final DataSource dataSource;

    public JdbcConversationMemoryService(DataSource dataSource) {
        this.dataSource = dataSource;
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
        String sql = """
                SELECT role, content, category, created_at
                FROM conversation_message
                WHERE conversation_id = ?
                ORDER BY created_at ASC
                """;
        List<MemoryMessage> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MemoryMessage(
                            rs.getString("role"),
                            rs.getString("content"),
                            ChatCategory.valueOf(rs.getString("category")),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("최근 메시지 조회에 실패했습니다.", e);
        }

        if (result.size() <= limit) {
            return result;
        }
        return result.subList(result.size() - limit, result.size());
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
        String updateSql = """
                UPDATE conversation_summary
                SET summary = ?, updated_at = ?
                WHERE conversation_id = ? AND category = ?
                """;
        String insertSql = """
                INSERT INTO conversation_summary(conversation_id, category, summary, updated_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, summary);
                update.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                update.setString(3, conversationId);
                update.setString(4, category.name());
                if (update.executeUpdate() == 0) {
                    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                        insert.setString(1, conversationId);
                        insert.setString(2, category.name());
                        insert.setString(3, summary);
                        insert.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("카테고리 요약 저장에 실패했습니다.", e);
        }
    }

    @Override
    public String getCategorySummary(String conversationId, ChatCategory category) {
        String sql = "SELECT summary FROM conversation_summary WHERE conversation_id = ? AND category = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("summary");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("카테고리 요약 조회에 실패했습니다.", e);
        }
        return "";
    }

    @Override
    public void addImportantNote(String conversationId, ChatCategory category, String note) {
        String insertSql = """
                INSERT INTO conversation_note(conversation_id, category, note, created_at)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, conversationId);
            ps.setString(2, category.name());
            ps.setString(3, note);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("중요 노트 저장에 실패했습니다.", e);
        }
        trimNotes(conversationId);
    }

    @Override
    public List<String> getImportantNotes(String conversationId, ChatCategory category) {
        String sql = """
                SELECT note
                FROM conversation_note
                WHERE conversation_id = ? AND category = ?
                ORDER BY created_at ASC
                """;
        List<String> notes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(rs.getString("note"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("중요 노트 조회에 실패했습니다.", e);
        }
        return notes;
    }

    @Override
    public ConversationMemorySnapshot snapshot(String conversationId) {
        return new ConversationMemorySnapshot(
                conversationId,
                recentMessages(conversationId, 20),
                loadSummaries(conversationId),
                loadNotes(conversationId)
        );
    }

    @Override
    public void clear(String conversationId) {
        deleteByConversation("DELETE FROM conversation_message WHERE conversation_id = ?", conversationId);
        deleteByConversation("DELETE FROM conversation_summary WHERE conversation_id = ?", conversationId);
        deleteByConversation("DELETE FROM conversation_note WHERE conversation_id = ?", conversationId);
    }

    @Override
    public List<String> listConversationIds() {
        String sql = """
                SELECT DISTINCT conversation_id, MAX(created_at) AS last_active
                FROM conversation_message
                GROUP BY conversation_id
                ORDER BY last_active DESC
                """;
        List<String> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString("conversation_id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("대화 목록 조회에 실패했습니다.", e);
        }
        return ids;
    }

    private void appendMessage(String conversationId, MemoryMessage message) {
        String sql = """
                INSERT INTO conversation_message(conversation_id, role, content, category, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.setString(2, message.getRole());
            ps.setString(3, message.getContent());
            ps.setString(4, message.getCategory().name());
            ps.setTimestamp(5, Timestamp.valueOf(message.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("메시지 저장에 실패했습니다.", e);
        }
        trimMessages(conversationId);
    }

    private Map<String, MemorySummary> loadSummaries(String conversationId) {
        String sql = "SELECT category, summary, updated_at FROM conversation_summary WHERE conversation_id = ?";
        Map<String, MemorySummary> summaries = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChatCategory category = ChatCategory.valueOf(rs.getString("category"));
                    summaries.put(category.name(), new MemorySummary(
                            category,
                            rs.getString("summary"),
                            rs.getTimestamp("updated_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("요약 스냅샷 조회에 실패했습니다.", e);
        }
        return summaries;
    }

    private List<ImportantNote> loadNotes(String conversationId) {
        String sql = """
                SELECT category, note, created_at
                FROM conversation_note
                WHERE conversation_id = ?
                ORDER BY created_at ASC
                """;
        List<ImportantNote> notes = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(new ImportantNote(
                            ChatCategory.valueOf(rs.getString("category")),
                            rs.getString("note"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("노트 스냅샷 조회에 실패했습니다.", e);
        }
        return notes;
    }

    private void trimMessages(String conversationId) {
        int deleteCount = Math.max(0, count("conversation_message", conversationId) - MAX_MESSAGES_PER_CONVERSATION);
        if (deleteCount <= 0) {
            return;
        }
        deleteOldestIds(findOldestIds("conversation_message", conversationId, deleteCount), "conversation_message");
    }

    private void trimNotes(String conversationId) {
        int deleteCount = Math.max(0, count("conversation_note", conversationId) - MAX_NOTES_PER_CONVERSATION);
        if (deleteCount <= 0) {
            return;
        }
        deleteOldestIds(findOldestIds("conversation_note", conversationId, deleteCount), "conversation_note");
    }

    private int count(String tableName, String conversationId) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE conversation_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("대화 건수 집계에 실패했습니다.", e);
        }
        return 0;
    }

    private List<Long> findOldestIds(String tableName, String conversationId, int limit) {
        String sql = "SELECT id FROM " + tableName + " WHERE conversation_id = ? ORDER BY created_at ASC";
        List<Long> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && ids.size() < limit) {
                    ids.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("오래된 메모리 조회에 실패했습니다.", e);
        }
        return ids;
    }

    private void deleteOldestIds(List<Long> ids, String tableName) {
        if (ids.isEmpty()) {
            return;
        }

        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Long id : ids) {
                ps.setLong(1, id);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("오래된 메모리 trim 처리에 실패했습니다.", e);
        }
    }

    private void deleteByConversation(String sql, String conversationId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("대화 삭제에 실패했습니다.", e);
        }
    }
}
