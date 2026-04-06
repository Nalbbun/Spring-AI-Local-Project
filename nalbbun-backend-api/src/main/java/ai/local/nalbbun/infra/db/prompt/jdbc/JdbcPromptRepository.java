package ai.local.nalbbun.infra.db.prompt.jdbc;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.DataAccessException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JdbcPromptRepository implements PromptRepository {

    @Qualifier("apiJdbcTemplate")
    private final JdbcTemplate jdbc;

    @Override
    public List<PromptEntry> findAll() {
        try {
            return jdbc.query("SELECT * FROM prompt_entry ORDER BY COALESCE(category, 'ZZZ'), is_default DESC, updated_at DESC", ROW_MAPPER);
        } catch (DataAccessException e) {
            log.warn("프롬프트 전체 조회 실패 - 빈 목록으로 대체 (reason={})", rootMessage(e));
            return List.of();
        }
    }

    @Override
    public List<PromptEntry> findByCategory(ChatCategory category) {
        try {
            if (category == null) return findAll();
            return jdbc.query("SELECT * FROM prompt_entry WHERE category = ? OR category IS NULL ORDER BY category NULLS FIRST, is_default DESC, updated_at DESC", ROW_MAPPER, category.name());
        } catch (DataAccessException e) {
            log.warn("프롬프트 카테고리 조회 실패 - 빈 목록으로 대체 (category={}, reason={})", category, rootMessage(e));
            return List.of();
        }
    }

    @Override
    public Optional<PromptEntry> findById(String id) {
        try {
            var list = jdbc.query("SELECT * FROM prompt_entry WHERE id = ?", ROW_MAPPER, id);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (DataAccessException e) {
            log.warn("프롬프트 단건 조회 실패 - 빈 결과로 대체 (id={}, reason={})", id, rootMessage(e));
            return Optional.empty();
        }
    }

    @Override
    public Optional<PromptEntry> findDefault(ChatCategory category) {
        try {
            List<PromptEntry> list;
            if (category == null) {
                list = jdbc.query("SELECT * FROM prompt_entry WHERE category IS NULL AND is_default = TRUE AND active = TRUE ORDER BY updated_at DESC FETCH FIRST 1 ROWS ONLY", ROW_MAPPER);
            } else {
                list = jdbc.query("SELECT * FROM prompt_entry WHERE category = ? AND is_default = TRUE AND active = TRUE ORDER BY updated_at DESC FETCH FIRST 1 ROWS ONLY", ROW_MAPPER, category.name());
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (DataAccessException e) {
            log.warn("기본 프롬프트 조회 실패 - 빈 결과로 대체 (category={}, reason={})", category, rootMessage(e));
            return Optional.empty();
        }
    }

    @Override
    public PromptEntry save(PromptEntry entry) {
        if (entry.getId() == null || entry.getId().isBlank()) entry.setId(UUID.randomUUID().toString());
        if (entry.getVersionNo() <= 0) entry.setVersionNo(1);
        LocalDateTime now = LocalDateTime.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        if (entry.isDefault()) clearDefault(entry.getCategory());
        try {
            jdbc.update("""
                INSERT INTO prompt_entry(id, name, category, system_prompt, description, is_default, active, version_no, previous_version_id, created_at, updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """,
                entry.getId(), entry.getName(), entry.getCategory() == null ? null : entry.getCategory().name(),
                entry.getSystemPrompt(), entry.getDescription(), entry.isDefault(), entry.isActive(), entry.getVersionNo(),
                entry.getPreviousVersionId(), Timestamp.valueOf(entry.getCreatedAt()), Timestamp.valueOf(entry.getUpdatedAt()));
            return entry;
        } catch (DataAccessException e) {
            throw new IllegalStateException("프롬프트 저장에 실패했습니다. API DB 또는 prompt_entry 테이블 상태를 확인하세요.", e);
        }
    }

    @Override
    public PromptEntry update(PromptEntry entry) {
        entry.setUpdatedAt(LocalDateTime.now());
        if (entry.isDefault()) clearDefault(entry.getCategory());
        try {
            jdbc.update("""
                UPDATE prompt_entry
                SET name=?, category=?, system_prompt=?, description=?, is_default=?, active=?, version_no=?, previous_version_id=?, updated_at=?
                WHERE id=?
                """,
                entry.getName(), entry.getCategory() == null ? null : entry.getCategory().name(), entry.getSystemPrompt(), entry.getDescription(),
                entry.isDefault(), entry.isActive(), entry.getVersionNo(), entry.getPreviousVersionId(), Timestamp.valueOf(entry.getUpdatedAt()), entry.getId());
            return entry;
        } catch (DataAccessException e) {
            throw new IllegalStateException("프롬프트 수정에 실패했습니다. API DB 또는 prompt_entry 테이블 상태를 확인하세요.", e);
        }
    }

    @Override
    public void delete(String id) {
        try { jdbc.update("DELETE FROM prompt_entry WHERE id = ?", id); }
        catch (DataAccessException e) { throw new IllegalStateException("프롬프트 삭제에 실패했습니다. API DB 또는 prompt_entry 테이블 상태를 확인하세요.", e); }
    }

    @Override
    public void clearDefault(ChatCategory category) {
        try {
            if (category == null) jdbc.update("UPDATE prompt_entry SET is_default = FALSE WHERE category IS NULL");
            else jdbc.update("UPDATE prompt_entry SET is_default = FALSE WHERE category = ?", category.name());
        } catch (DataAccessException e) { throw new IllegalStateException("기본 프롬프트 해제에 실패했습니다. API DB 또는 prompt_entry 테이블 상태를 확인하세요.", e); }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null) cursor = cursor.getCause();
        return cursor.getMessage() == null ? throwable.getClass().getSimpleName() : cursor.getMessage();
    }

    private static final RowMapper<PromptEntry> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        PromptEntry e = new PromptEntry();
        e.setId(rs.getString("id"));
        e.setName(rs.getString("name"));
        String cat = rs.getString("category");
        e.setCategory(cat == null ? null : ChatCategory.valueOf(cat));
        e.setSystemPrompt(rs.getString("system_prompt"));
        e.setDescription(rs.getString("description"));
        e.setDefault(rs.getBoolean("is_default"));
        e.setActive(rs.getBoolean("active"));
        e.setVersionNo(rs.getInt("version_no"));
        e.setPreviousVersionId(rs.getString("previous_version_id"));
        e.setCreatedAt(toLocalDateTime(rs, "created_at"));
        e.setUpdatedAt(toLocalDateTime(rs, "updated_at"));
        return e;
    };

    private static LocalDateTime toLocalDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? null : ts.toLocalDateTime();
    }
}
