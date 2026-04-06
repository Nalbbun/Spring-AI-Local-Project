package ai.local.nalbbun.domain.prompt.service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.model.PromptEntryHistoryRecord;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 프롬프트 CRUD 서비스.
 * CategoryHandler에서 시스템 프롬프트 오버라이드 시 사용합니다.
 */
@Slf4j
@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final JdbcTemplate apiJdbcTemplate;

    public PromptService(
            PromptRepository promptRepository,
            @Qualifier("apiJdbcTemplate") ObjectProvider<JdbcTemplate> apiJdbcTemplateProvider
    ) {
        this.promptRepository = promptRepository;
        this.apiJdbcTemplate = apiJdbcTemplateProvider.getIfAvailable();
    }

    public List<PromptEntry> listAll() {
        return promptRepository.findAll();
    }

    public List<PromptEntry> listByCategory(ChatCategory category) {
        return promptRepository.findByCategory(category);
    }

    public Optional<PromptEntry> findById(String id) {
        return promptRepository.findById(id);
    }

    public List<PromptEntryHistoryRecord> history(String id) {
        if (apiJdbcTemplate == null) {
            return List.of();
        }
        return apiJdbcTemplate.query(
                """
                SELECT history_id, prompt_id, action, name, category, system_prompt, description,
                       is_default, active, version_no, previous_version_id, captured_at
                  FROM prompt_entry_history
                 WHERE prompt_id = ?
                 ORDER BY captured_at DESC, history_id DESC
                """,
                (rs, rowNum) -> mapHistory(rs),
                id);
    }

    /**
     * 채팅 시 적용할 시스템 프롬프트 결정.
     * promptId가 있으면 해당 프롬프트 → 없으면 카테고리 기본 프롬프트 → 없으면 null(핸들러 내장 프롬프트 사용)
     */
    public Optional<String> resolveSystemPrompt(String promptId, ChatCategory category) {
        String commonPrompt = promptRepository.findDefault(null)
                .filter(PromptEntry::isActive)
                .map(PromptEntry::getSystemPrompt)
                .orElse(null);

        String selectedPrompt = null;
        if (promptId != null && !promptId.isBlank()) {
            selectedPrompt = promptRepository.findById(promptId)
                    .filter(PromptEntry::isActive)
                    .map(PromptEntry::getSystemPrompt)
                    .orElse(null);
        } else if (category != null) {
            selectedPrompt = promptRepository.findDefault(category)
                    .filter(PromptEntry::isActive)
                    .map(PromptEntry::getSystemPrompt)
                    .orElse(null);
        }

        String combined = joinPromptBlocks(commonPrompt, selectedPrompt);
        return combined == null || combined.isBlank() ? Optional.empty() : Optional.of(combined);
    }

    public PromptEntry create(PromptEntry entry) {
        validate(entry);
        if (entry.getVersionNo() <= 0) entry.setVersionNo(1);
        entry.setPreviousVersionId(null);
        PromptEntry saved = promptRepository.save(entry);
        captureHistory(saved, "CREATED");
        return saved;
    }

    public PromptEntry update(String id, PromptEntry entry) {
        validate(entry);
        PromptEntry existing = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        captureHistory(existing, "BEFORE_UPDATE");
        entry.setId(id);
        entry.setCreatedAt(existing.getCreatedAt());
        entry.setVersionNo(Math.max(existing.getVersionNo() + 1, 1));
        entry.setPreviousVersionId(existing.getId());
        PromptEntry saved = promptRepository.update(entry);
        captureHistory(saved, "UPDATED");
        return saved;
    }

    public void delete(String id) {
        PromptEntry existing = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        captureHistory(existing, "DELETED");
        promptRepository.delete(id);
    }

    /** 기본 프롬프트 지정 */
    public PromptEntry setDefault(String id) {
        PromptEntry entry = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        captureHistory(entry, "BEFORE_SET_DEFAULT");
        entry.setDefault(true);
        entry.setVersionNo(Math.max(entry.getVersionNo() + 1, 1));
        entry.setPreviousVersionId(entry.getId());
        PromptEntry saved = promptRepository.update(entry);
        captureHistory(saved, "SET_DEFAULT");
        return saved;
    }

    public PromptEntry rollbackToHistory(String id, Long historyId) {
        PromptEntry existing = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        PromptEntryHistoryRecord history = history(id).stream()
                .filter(item -> Objects.equals(item.historyId(), historyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("롤백 대상을 찾을 수 없습니다: " + historyId));

        captureHistory(existing, "BEFORE_ROLLBACK");

        PromptEntry rollback = PromptEntry.builder()
                .id(existing.getId())
                .name(history.name())
                .category(history.category())
                .systemPrompt(history.systemPrompt())
                .description(history.description())
                .isDefault(history.isDefault())
                .active(history.active())
                .versionNo(Math.max(existing.getVersionNo() + 1, history.versionNo() + 1))
                .previousVersionId(existing.getId())
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        PromptEntry saved = promptRepository.update(rollback);
        captureHistory(saved, "ROLLED_BACK");
        return saved;
    }

    public void seedDefaultsIfEmpty() {
        List<PromptEntry> existing = promptRepository.findAll();

        ensureDefaultPrompt(existing, null,
                "[기본] 공통 운영 프롬프트",
                commonSystemPrompt(),
                "모든 카테고리에 공통으로 적용되는 기본 프롬프트");

        for (ChatCategory cat : ChatCategory.values()) {
            ensureDefaultPrompt(existing,
                    cat,
                    "[기본] " + cat.name() + " 프롬프트",
                    defaultSystemPrompt(cat),
                    "카테고리별 기본 프롬프트");
        }
        log.info("기본 프롬프트 점검/시드 완료");
    }

    private void ensureDefaultPrompt(List<PromptEntry> existing,
                                     ChatCategory category,
                                     String name,
                                     String systemPrompt,
                                     String description) {
        boolean exists = existing.stream()
                .filter(PromptEntry::isDefault)
                .filter(PromptEntry::isActive)
                .anyMatch(entry -> Objects.equals(entry.getCategory(), category));
        if (exists) {
            return;
        }

        PromptEntry entry = PromptEntry.builder()
                .name(name)
                .category(category)
                .systemPrompt(systemPrompt)
                .description(description)
                .isDefault(true)
                .active(true)
                .build();
        PromptEntry saved = promptRepository.save(entry);
        captureHistory(saved, "SEEDED_DEFAULT");
        existing.add(saved);
    }

    private String joinPromptBlocks(String commonPrompt, String specificPrompt) {
        List<String> blocks = new ArrayList<>();
        if (commonPrompt != null && !commonPrompt.isBlank()) {
            blocks.add(commonPrompt.trim());
        }
        if (specificPrompt != null && !specificPrompt.isBlank()) {
            blocks.add(specificPrompt.trim());
        }
        return String.join("\n\n", blocks);
    }

    private String commonSystemPrompt() {
        return """
                당신은 Nalbbun AI Local Assistant입니다.
                사용자의 현재 질문에 바로 도움이 되는 답을 우선 제시하세요.
                답변은 지나치게 장황하지 않게 정리하되, 필요한 경우 단계와 우선순위를 분명히 제시하세요.
                확실하지 않은 내용은 추정이라고 명확히 밝히고, 확인이 필요한 부분은 분리해서 안내하세요.
                이전 대화 맥락이 있으면 이어받되, 현재 질문과 직접 관련된 내용만 반영하세요.
                """;
    }

    private void validate(PromptEntry entry) {
        if (entry.getName() == null || entry.getName().isBlank())
            throw new IllegalArgumentException("프롬프트 이름은 필수입니다.");
        if (entry.getSystemPrompt() == null || entry.getSystemPrompt().isBlank())
            throw new IllegalArgumentException("시스템 프롬프트 내용은 필수입니다.");
    }

    private String defaultSystemPrompt(ChatCategory category) {
        return switch (category) {
            case GENERAL -> """
                당신은 일반 질의응답 어시스턴트입니다.
                최근 대화와 중요 메모를 참고하되, 현재 질문에 직접적으로 답변하세요.
                불필요하게 과거 내용을 반복하지 말고 자연스럽게 이어서 답변하세요.
                """;
            case DEV -> """
                당신은 실무 중심의 개발/인프라/리팩토링 기술 어시스턴트입니다.
                응답은 우선순위와 단계 순서가 보이도록 작성하세요.
                최근 대화의 연속성과 이전 구조 결정을 반영하세요.
                필요하면 선택지보다 권장안을 먼저 제시하세요.
                """;
            case MICE -> """
                당신은 MICE/행사기획 전문 어시스턴트입니다.
                답변은 배경-목표-방향-구성 순으로 정리하고,
                이전 대화에서 정리된 메시지, 구조, 방향성을 이어받아 일관성 있게 작성하세요.
                필요시 슬로건, 기획 의도, 운영 포인트를 구조적으로 제시하세요.
                """;
            case TRAVEL -> """
                당신은 여행 계획 전문 어시스턴트입니다.
                목적지, 일정, 예산을 고려하여 실용적인 여행 계획을 제시하세요.
                """;
        };
    }

    private void captureHistory(PromptEntry entry, String action) {
        if (apiJdbcTemplate == null || entry == null || entry.getId() == null || entry.getId().isBlank()) {
            return;
        }
        try {
            apiJdbcTemplate.update(
                    """
                    INSERT INTO prompt_entry_history(
                        prompt_id, action, name, category, system_prompt, description,
                        is_default, active, version_no, previous_version_id, captured_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    entry.getId(),
                    action,
                    entry.getName(),
                    entry.getCategory() == null ? null : entry.getCategory().name(),
                    entry.getSystemPrompt(),
                    entry.getDescription(),
                    entry.isDefault(),
                    entry.isActive(),
                    entry.getVersionNo(),
                    entry.getPreviousVersionId(),
                    Timestamp.valueOf(LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("프롬프트 히스토리 저장 실패. promptId={}, action={}, reason={}", entry.getId(), action, e.getMessage());
        }
    }

    private PromptEntryHistoryRecord mapHistory(ResultSet rs) throws java.sql.SQLException {
        String category = rs.getString("category");
        Timestamp capturedAt = rs.getTimestamp("captured_at");
        return new PromptEntryHistoryRecord(
                rs.getLong("history_id"),
                rs.getString("prompt_id"),
                rs.getString("action"),
                rs.getString("name"),
                category == null ? null : ChatCategory.valueOf(category),
                rs.getString("system_prompt"),
                rs.getString("description"),
                rs.getBoolean("is_default"),
                rs.getBoolean("active"),
                rs.getInt("version_no"),
                rs.getString("previous_version_id"),
                capturedAt == null ? null : capturedAt.toLocalDateTime()
        );
    }
}
