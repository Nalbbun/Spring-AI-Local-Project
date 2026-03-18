package ai.local.nalbbun.prompt.service;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.prompt.model.PromptPageScope;
import ai.local.nalbbun.prompt.model.PromptSelection;
import ai.local.nalbbun.prompt.model.PromptTemplateRecord;
import ai.local.nalbbun.prompt.model.PromptTemplateUpsertRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * DataSource가 없는 환경을 위한 no-op 프롬프트 서비스다.
 */
@Service
@ConditionalOnMissingBean(PromptTemplateService.class)
public class NoopPromptTemplateService implements PromptTemplateService {

    @Override
    public List<PromptTemplateRecord> findAll(PromptPageScope pageScope, ChatCategory category, boolean activeOnly) {
        return List.of();
    }

    @Override
    public Optional<PromptTemplateRecord> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public PromptTemplateRecord create(PromptTemplateUpsertRequest request) {
        throw new IllegalStateException("DataSource가 없어 프롬프트 저장을 사용할 수 없습니다.");
    }

    @Override
    public PromptTemplateRecord update(Long id, PromptTemplateUpsertRequest request) {
        throw new IllegalStateException("DataSource가 없어 프롬프트 수정을 사용할 수 없습니다.");
    }

    @Override
    public void delete(Long id) {
        throw new IllegalStateException("DataSource가 없어 프롬프트 삭제를 사용할 수 없습니다.");
    }

    @Override
    public PromptSelection resolveSelection(Long promptTemplateId,
                                            PromptPageScope pageScope,
                                            ChatCategory category,
                                            String fallbackSystemPrompt) {
        return PromptSelection.builtin(fallbackSystemPrompt);
    }
}
