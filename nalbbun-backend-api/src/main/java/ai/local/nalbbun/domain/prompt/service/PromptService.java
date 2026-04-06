package ai.local.nalbbun.domain.prompt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프롬프트 CRUD 서비스.
 * CategoryHandler에서 시스템 프롬프트 오버라이드 시 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;

    // ── 조회 ──────────────────────────────────────────────
    public List<PromptEntry> listAll() {
        return promptRepository.findAll();
    }

    public List<PromptEntry> listByCategory(ChatCategory category) {
        return promptRepository.findByCategory(category);
    }

    public Optional<PromptEntry> findById(String id) {
        return promptRepository.findById(id);
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

    // ── CUD ───────────────────────────────────────────────
    public PromptEntry create(PromptEntry entry) {
        validate(entry);
        if (entry.getVersionNo() <= 0) entry.setVersionNo(1);
        entry.setPreviousVersionId(null);
        return promptRepository.save(entry);
    }

    public PromptEntry update(String id, PromptEntry entry) {
        validate(entry);
        entry.setId(id);
        PromptEntry existing = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        entry.setCreatedAt(existing.getCreatedAt());
        entry.setVersionNo(Math.max(existing.getVersionNo() + 1, 1));
        entry.setPreviousVersionId(existing.getId());
        return promptRepository.update(entry);
    }

    public void delete(String id) {
        promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        promptRepository.delete(id);
    }

    /** 기본 프롬프트 지정 */
    public PromptEntry setDefault(String id) {
        PromptEntry entry = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        entry.setDefault(true);
        return promptRepository.update(entry);
    }

    // ── 초기 기본 프롬프트 시드 ────────────────────────────
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
}
