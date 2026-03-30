package ai.local.nalbbun.domain.prompt.service;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        if (promptId != null && !promptId.isBlank()) {
            return promptRepository.findById(promptId)
                    .filter(PromptEntry::isActive)
                    .map(PromptEntry::getSystemPrompt);
        }
        return promptRepository.findDefault(category)
                .map(PromptEntry::getSystemPrompt);
    }

    // ── CUD ───────────────────────────────────────────────
    public PromptEntry create(PromptEntry entry) {
        validate(entry);
        return promptRepository.save(entry);
    }

    public PromptEntry update(String id, PromptEntry entry) {
        validate(entry);
        entry.setId(id);
        PromptEntry existing = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
        entry.setCreatedAt(existing.getCreatedAt());
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
        if (!promptRepository.findAll().isEmpty()) return;

        for (ChatCategory cat : ChatCategory.values()) {
            PromptEntry e = PromptEntry.builder()
                    .name("[기본] " + cat.name() + " 프롬프트")
                    .category(cat)
                    .systemPrompt(defaultSystemPrompt(cat))
                    .description("카테고리별 내장 기본 프롬프트")
                    .isDefault(true)
                    .active(true)
                    .build();
            promptRepository.save(e);
        }
        log.info("기본 프롬프트 시드 완료");
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
