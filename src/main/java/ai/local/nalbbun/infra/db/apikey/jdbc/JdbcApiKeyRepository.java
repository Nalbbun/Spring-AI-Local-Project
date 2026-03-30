package ai.local.nalbbun.infra.db.apikey.jdbc;

import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API 키 JDBC 저장소 구현체다.
 * 스키마 생성은 Flyway(db/migration)에서 관리한다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcApiKeyRepository implements ApiKeyRepository {

    private final JdbcTemplate jdbc;

    public List<ApiKeyEntry> findAll() {
        return jdbc.query(
            "SELECT * FROM api_key_entry ORDER BY provider, created_at DESC",
            ROW_MAPPER);
    }

    public List<ApiKeyEntry> findByProvider(String provider) {
        return jdbc.query(
            "SELECT * FROM api_key_entry WHERE provider = ? ORDER BY active DESC, created_at DESC",
            ROW_MAPPER, provider);
    }

    public Optional<ApiKeyEntry> findById(String id) {
        var list = jdbc.query(
            "SELECT * FROM api_key_entry WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** provider 의 활성 키 중 가장 최근 것 */
    public Optional<ApiKeyEntry> findActiveByProvider(String provider) {
        var list = jdbc.query(
            "SELECT * FROM api_key_entry WHERE provider = ? AND active = TRUE ORDER BY updated_at DESC LIMIT 1",
            ROW_MAPPER, provider);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public ApiKeyEntry save(ApiKeyEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) {
            entry.setId(UUID.randomUUID().toString());
        }
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        jdbc.update("""
            INSERT INTO api_key_entry(id, provider, label, description, key_value, active, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?)
            """,
            entry.getId(), entry.getProvider(), entry.getLabel(),
            entry.getDescription(), entry.getKeyValue(),
            entry.isActive(),
            Timestamp.valueOf(entry.getCreatedAt()),
            Timestamp.valueOf(entry.getUpdatedAt()));
        return entry;
    }

    public ApiKeyEntry update(ApiKeyEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        jdbc.update("""
            UPDATE api_key_entry
            SET provider=?, label=?, description=?, key_value=?, active=?, updated_at=?
            WHERE id=?
            """,
            entry.getProvider(), entry.getLabel(), entry.getDescription(),
            entry.getKeyValue(), entry.isActive(),
            Timestamp.valueOf(entry.getUpdatedAt()), entry.getId());
        return entry;
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM api_key_entry WHERE id = ?", id);
    }

    private static final RowMapper<ApiKeyEntry> ROW_MAPPER = (rs, rowNum) -> {
        ApiKeyEntry e = new ApiKeyEntry();
        e.setId(rs.getString("id"));
        e.setProvider(rs.getString("provider"));
        e.setLabel(rs.getString("label"));
        e.setDescription(rs.getString("description"));
        e.setKeyValue(rs.getString("key_value"));
        e.setActive(rs.getBoolean("active"));
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        e.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        e.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        return e;
    };
}
