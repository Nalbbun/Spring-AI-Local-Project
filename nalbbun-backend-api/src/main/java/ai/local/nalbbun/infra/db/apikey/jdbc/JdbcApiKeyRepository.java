package ai.local.nalbbun.infra.db.apikey.jdbc;

import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
@RequiredArgsConstructor
public class JdbcApiKeyRepository implements ApiKeyRepository {

    private final JdbcTemplate jdbc;

    @Override
    public List<ApiKeyEntry> findAll() {
        try {
            return jdbc.query(
                    "SELECT * FROM api_key_entry ORDER BY provider, created_at DESC",
                    ROW_MAPPER);
        } catch (DataAccessException e) {
            log.warn("API 키 전체 조회 실패 - 빈 목록으로 대체 (reason={})", rootMessage(e));
            return List.of();
        }
    }

    @Override
    public List<ApiKeyEntry> findByProvider(String provider) {
        try {
            return jdbc.query(
                    "SELECT * FROM api_key_entry WHERE provider = ? ORDER BY active DESC, created_at DESC",
                    ROW_MAPPER, provider);
        } catch (DataAccessException e) {
            log.warn("API 키 provider 조회 실패 - 빈 목록으로 대체 (provider={}, reason={})", provider, rootMessage(e));
            return List.of();
        }
    }

    @Override
    public Optional<ApiKeyEntry> findById(String id) {
        try {
            var list = jdbc.query(
                    "SELECT * FROM api_key_entry WHERE id = ?", ROW_MAPPER, id);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (DataAccessException e) {
            log.warn("API 키 단건 조회 실패 - 빈 결과로 대체 (id={}, reason={})", id, rootMessage(e));
            return Optional.empty();
        }
    }

    @Override
    public Optional<ApiKeyEntry> findActiveByProvider(String provider) {
        try {
            var list = jdbc.query(
                    "SELECT * FROM api_key_entry WHERE provider = ? AND active = TRUE ORDER BY updated_at DESC LIMIT 1",
                    ROW_MAPPER, provider);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (DataAccessException e) {
            log.warn("API 키 활성 조회 실패 - 빈 결과로 대체 (provider={}, reason={})", provider, rootMessage(e));
            return Optional.empty();
        }
    }

    @Override
    public ApiKeyEntry save(ApiKeyEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) {
            entry.setId(UUID.randomUUID().toString());
        }
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        try {
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
        } catch (DataAccessException e) {
            throw new IllegalStateException("API 키 저장에 실패했습니다. DB 연결 및 api_key_entry 테이블 상태를 확인하세요.", e);
        }
    }

    @Override
    public ApiKeyEntry update(ApiKeyEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        try {
            jdbc.update("""
                UPDATE api_key_entry
                SET provider=?, label=?, description=?, key_value=?, active=?, updated_at=?
                WHERE id=?
                """,
                    entry.getProvider(), entry.getLabel(), entry.getDescription(),
                    entry.getKeyValue(), entry.isActive(),
                    Timestamp.valueOf(entry.getUpdatedAt()), entry.getId());
            return entry;
        } catch (DataAccessException e) {
            throw new IllegalStateException("API 키 수정에 실패했습니다. DB 연결 및 api_key_entry 테이블 상태를 확인하세요.", e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            jdbc.update("DELETE FROM api_key_entry WHERE id = ?", id);
        } catch (DataAccessException e) {
            throw new IllegalStateException("API 키 삭제에 실패했습니다. DB 연결 및 api_key_entry 테이블 상태를 확인하세요.", e);
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? throwable.getClass().getSimpleName() : cursor.getMessage();
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
