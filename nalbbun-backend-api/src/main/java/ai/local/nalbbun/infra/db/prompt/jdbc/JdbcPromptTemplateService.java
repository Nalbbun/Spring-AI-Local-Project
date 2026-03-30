package ai.local.nalbbun.infra.db.prompt.jdbc;
import ai.local.nalbbun.domain.prompt.service.PromptTemplateService;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptPageScope;
import ai.local.nalbbun.domain.prompt.model.PromptSelection;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateRecord;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateUpsertRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL/JDBC 기반 프롬프트 템플릿 저장소 구현체다.
 */
@Service
@ConditionalOnBean(DataSource.class)
public class JdbcPromptTemplateService implements PromptTemplateService {

    private final DataSource dataSource;

    public JdbcPromptTemplateService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<PromptTemplateRecord> findAll(PromptPageScope pageScope, ChatCategory category, boolean activeOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, description, page_scope, category, system_prompt, active, default_prompt, created_at, updated_at
                FROM prompt_template
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (pageScope != null) {
            sql.append(" AND (page_scope = ? OR page_scope = 'ALL')");
            params.add(pageScope.name());
        }
        if (category != null) {
            sql.append(" AND (category = ? OR category IS NULL)");
            params.add(category.name());
        }
        if (activeOnly) {
            sql.append(" AND active = TRUE");
        }
        sql.append(" ORDER BY default_prompt DESC, updated_at DESC, id DESC");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<PromptTemplateRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("프롬프트 목록 조회에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<PromptTemplateRecord> findById(Long id) {
        String sql = """
                SELECT id, name, description, page_scope, category, system_prompt, active, default_prompt, created_at, updated_at
                FROM prompt_template
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("프롬프트 상세 조회에 실패했습니다.", e);
        }
    }

    @Override
    public PromptTemplateRecord create(PromptTemplateUpsertRequest request) {
        PromptTemplateUpsertRequest normalized = normalize(request);
        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO prompt_template(name, description, page_scope, category, system_prompt, active, default_prompt, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, normalized, now, now);
            ps.executeUpdate();
            Long id;
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new IllegalStateException("프롬프트 ID 생성값을 읽지 못했습니다.");
                }
                id = rs.getLong(1);
            }
            if (Boolean.TRUE.equals(normalized.defaultPrompt())) {
                clearOtherDefaults(id, normalized.pageScope(), normalized.category());
            }
            return findById(id).orElseThrow(() -> new IllegalStateException("저장된 프롬프트를 다시 읽지 못했습니다."));
        } catch (SQLException e) {
            throw new IllegalStateException("프롬프트 저장에 실패했습니다.", e);
        }
    }

    @Override
    public PromptTemplateRecord update(Long id, PromptTemplateUpsertRequest request) {
        PromptTemplateUpsertRequest normalized = normalize(request);
        String sql = """
                UPDATE prompt_template
                SET name = ?, description = ?, page_scope = ?, category = ?, system_prompt = ?, active = ?, default_prompt = ?, updated_at = ?
                WHERE id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bindUpdate(ps, normalized, LocalDateTime.now());
            ps.setLong(9, id);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("존재하지 않는 프롬프트 ID 입니다: " + id);
            }
            if (Boolean.TRUE.equals(normalized.defaultPrompt())) {
                clearOtherDefaults(id, normalized.pageScope(), normalized.category());
            }
            return findById(id).orElseThrow(() -> new IllegalStateException("수정된 프롬프트를 다시 읽지 못했습니다."));
        } catch (SQLException e) {
            throw new IllegalStateException("프롬프트 수정에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM prompt_template WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("프롬프트 삭제에 실패했습니다.", e);
        }
    }

    @Override
    public PromptSelection resolveSelection(Long promptTemplateId,
                                            PromptPageScope pageScope,
                                            ChatCategory category,
                                            String fallbackSystemPrompt) {
        if (promptTemplateId != null) {
            PromptTemplateRecord record = findById(promptTemplateId)
                    .filter(PromptTemplateRecord::active)
                    .orElseThrow(() -> new IllegalArgumentException("선택한 프롬프트를 찾을 수 없거나 비활성 상태입니다."));
            if (!isPageCompatible(record.pageScope(), pageScope)) {
                throw new IllegalArgumentException("선택한 프롬프트는 현재 테스트 페이지에서 사용할 수 없습니다.");
            }
            return new PromptSelection("REQUEST_TEMPLATE", record.id(), record.name(), record.systemPrompt());
        }

        Optional<PromptTemplateRecord> defaultRecord = findDefault(pageScope, category);
        if (defaultRecord.isPresent()) {
            PromptTemplateRecord record = defaultRecord.get();
            return new PromptSelection("DEFAULT_TEMPLATE", record.id(), record.name(), record.systemPrompt());
        }
        return PromptSelection.builtin(fallbackSystemPrompt);
    }

    private Optional<PromptTemplateRecord> findDefault(PromptPageScope pageScope, ChatCategory category) {
        if (pageScope == null) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, name, description, page_scope, category, system_prompt, active, default_prompt, created_at, updated_at
                FROM prompt_template
                WHERE active = TRUE
                  AND default_prompt = TRUE
                  AND (page_scope = ? OR page_scope = 'ALL')
                  AND (category = ? OR category IS NULL)
                ORDER BY
                  CASE WHEN page_scope = ? THEN 0 ELSE 1 END,
                  CASE WHEN category = ? THEN 0 ELSE 1 END,
                  updated_at DESC,
                  id DESC
                FETCH FIRST 1 ROWS ONLY
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pageScope.name());
            ps.setString(2, category == null ? null : category.name());
            ps.setString(3, pageScope.name());
            ps.setString(4, category == null ? null : category.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("기본 프롬프트 조회에 실패했습니다.", e);
        }
    }

    private void clearOtherDefaults(Long currentId, PromptPageScope pageScope, ChatCategory category) throws SQLException {
        String sql;
        if (category == null) {
            sql = "UPDATE prompt_template SET default_prompt = FALSE WHERE id <> ? AND page_scope = ? AND category IS NULL";
        } else {
            sql = "UPDATE prompt_template SET default_prompt = FALSE WHERE id <> ? AND page_scope = ? AND category = ?";
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, currentId);
            ps.setString(2, pageScope.name());
            if (category != null) {
                ps.setString(3, category.name());
            }
            ps.executeUpdate();
        }
    }

    private PromptTemplateUpsertRequest normalize(PromptTemplateUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("프롬프트 요청 본문이 비어 있습니다.");
        }
        String name = trim(request.name());
        String systemPrompt = trim(request.systemPrompt());
        if (name.isBlank()) {
            throw new IllegalArgumentException("프롬프트 이름은 필수입니다.");
        }
        if (systemPrompt.isBlank()) {
            throw new IllegalArgumentException("System Prompt 내용은 필수입니다.");
        }
        PromptPageScope pageScope = request.pageScope() == null ? PromptPageScope.ALL : request.pageScope();
        String description = trim(request.description());
        return new PromptTemplateUpsertRequest(
                name,
                description.isBlank() ? null : description,
                pageScope,
                request.category(),
                systemPrompt,
                request.active() == null ? Boolean.TRUE : request.active(),
                request.defaultPrompt() == null ? Boolean.FALSE : request.defaultPrompt()
        );
    }

    private void bindInsert(PreparedStatement ps,
                            PromptTemplateUpsertRequest request,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) throws SQLException {
        ps.setString(1, request.name());
        ps.setString(2, request.description());
        ps.setString(3, request.pageScope().name());
        if (request.category() == null) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, request.category().name());
        }
        ps.setString(5, request.systemPrompt());
        ps.setBoolean(6, Boolean.TRUE.equals(request.active()));
        ps.setBoolean(7, Boolean.TRUE.equals(request.defaultPrompt()));
        ps.setTimestamp(8, Timestamp.valueOf(createdAt));
        ps.setTimestamp(9, Timestamp.valueOf(updatedAt));
    }

    private void bindUpdate(PreparedStatement ps,
                            PromptTemplateUpsertRequest request,
                            LocalDateTime updatedAt) throws SQLException {
        ps.setString(1, request.name());
        ps.setString(2, request.description());
        ps.setString(3, request.pageScope().name());
        if (request.category() == null) {
            ps.setNull(4, Types.VARCHAR);
        } else {
            ps.setString(4, request.category().name());
        }
        ps.setString(5, request.systemPrompt());
        ps.setBoolean(6, Boolean.TRUE.equals(request.active()));
        ps.setBoolean(7, Boolean.TRUE.equals(request.defaultPrompt()));
        ps.setTimestamp(8, Timestamp.valueOf(updatedAt));
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            if (value == null) {
                ps.setNull(i + 1, Types.VARCHAR);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    private PromptTemplateRecord mapRow(ResultSet rs) throws SQLException {
        String categoryValue = rs.getString("category");
        return new PromptTemplateRecord(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                PromptPageScope.valueOf(rs.getString("page_scope")),
                categoryValue == null ? null : ChatCategory.valueOf(categoryValue),
                rs.getString("system_prompt"),
                rs.getBoolean("active"),
                rs.getBoolean("default_prompt"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private boolean isPageCompatible(PromptPageScope promptPageScope, PromptPageScope requestedPageScope) {
        return promptPageScope == PromptPageScope.ALL
                || requestedPageScope == null
                || promptPageScope == requestedPageScope;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
