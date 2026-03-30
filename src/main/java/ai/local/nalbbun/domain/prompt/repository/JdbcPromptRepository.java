package ai.local.nalbbun.domain.prompt.repository;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Primary
@ConditionalOnProperty(prefix = "app.prompt", name = "store", havingValue = "jdbc", matchIfMissing = true)
@RequiredArgsConstructor
public class JdbcPromptRepository implements PromptRepository {

    private final JdbcTemplate jdbc;

    @PostConstruct
    void initSchema() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS prompt_entry (
                id          VARCHAR(36)  NOT NULL PRIMARY KEY,
                name        VARCHAR(200) NOT NULL,
                category    VARCHAR(32),
                system_prompt TEXT       NOT NULL,
                description VARCHAR(500),
                is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
                active      BOOLEAN      NOT NULL DEFAULT TRUE,
                created_at  TIMESTAMP    NOT NULL,
                updated_at  TIMESTAMP    NOT NULL
            )
            """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_prompt_category ON prompt_entry (category)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_prompt_default  ON prompt_entry (category, is_default)");
        log.info("prompt_entry schema initialized");
    }

    @Override
    public List<PromptEntry> findAll() {
        return jdbc.query(
                "SELECT * FROM prompt_entry ORDER BY category NULLS LAST, is_default DESC, created_at DESC",
                ROW_MAPPER);
    }

    @Override
    public List<PromptEntry> findByCategory(ChatCategory category) {
        if (category == null) return findAll();
        return jdbc.query(
                "SELECT * FROM prompt_entry WHERE category = ? OR category IS NULL ORDER BY is_default DESC, created_at DESC",
                ROW_MAPPER, category.name());
    }

    @Override
    public Optional<PromptEntry> findById(String id) {
        List<PromptEntry> list = jdbc.query(
                "SELECT * FROM prompt_entry WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<PromptEntry> findDefault(ChatCategory category) {
        String sql = category == null
                ? "SELECT * FROM prompt_entry WHERE is_default = TRUE AND category IS NULL AND active = TRUE LIMIT 1"
                : "SELECT * FROM prompt_entry WHERE is_default = TRUE AND category = ? AND active = TRUE LIMIT 1";
        List<PromptEntry> list = category == null
                ? jdbc.query(sql, ROW_MAPPER)
                : jdbc.query(sql, ROW_MAPPER, category.name());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
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

        jdbc.update("""
            INSERT INTO prompt_entry(id, name, category, system_prompt, description, is_default, active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                entry.getId(), entry.getName(),
                entry.getCategory() == null ? null : entry.getCategory().name(),
                entry.getSystemPrompt(), entry.getDescription(),
                entry.isDefault(), entry.isActive(),
                Timestamp.valueOf(entry.getCreatedAt()), Timestamp.valueOf(entry.getUpdatedAt()));
        return entry;
    }

    @Override
    public PromptEntry update(PromptEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        if (entry.isDefault()) clearDefault(entry.getCategory());

        jdbc.update("""
            UPDATE prompt_entry
            SET name=?, category=?, system_prompt=?, description=?, is_default=?, active=?, updated_at=?
            WHERE id=?
            """,
                entry.getName(),
                entry.getCategory() == null ? null : entry.getCategory().name(),
                entry.getSystemPrompt(), entry.getDescription(),
                entry.isDefault(), entry.isActive(),
                Timestamp.valueOf(entry.getUpdatedAt()), entry.getId());
        return entry;
    }

    @Override
    public void delete(String id) {
        jdbc.update("DELETE FROM prompt_entry WHERE id = ?", id);
    }

    @Override
    public void clearDefault(ChatCategory category) {
        if (category == null) {
            jdbc.update("UPDATE prompt_entry SET is_default = FALSE WHERE category IS NULL");
        } else {
            jdbc.update("UPDATE prompt_entry SET is_default = FALSE WHERE category = ?", category.name());
        }
    }

    private static final RowMapper<PromptEntry> ROW_MAPPER = (rs, rowNum) -> {
        PromptEntry e = new PromptEntry();
        e.setId(rs.getString("id"));
        e.setName(rs.getString("name"));
        String cat = rs.getString("category");
        e.setCategory(cat == null ? null : ChatCategory.valueOf(cat));
        e.setSystemPrompt(rs.getString("system_prompt"));
        e.setDescription(rs.getString("description"));
        e.setDefault(rs.getBoolean("is_default"));
        e.setActive(rs.getBoolean("active"));
        e.setCreatedAt(toLocalDateTime(rs, "created_at"));
        e.setUpdatedAt(toLocalDateTime(rs, "updated_at"));
        return e;
    };

    private static LocalDateTime toLocalDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
