package ai.local.nalbbun.prompt.service;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.prompt.model.PromptPageScope;
import ai.local.nalbbun.prompt.model.PromptSelection;
import ai.local.nalbbun.prompt.model.PromptTemplateRecord;
import ai.local.nalbbun.prompt.model.PromptTemplateUpsertRequest;

import java.util.List;
import java.util.Optional;

/**
 * 프롬프트 템플릿 저장/조회/적용을 담당한다.
 */
public interface PromptTemplateService {

    List<PromptTemplateRecord> findAll(PromptPageScope pageScope, ChatCategory category, boolean activeOnly);

    Optional<PromptTemplateRecord> findById(Long id);

    PromptTemplateRecord create(PromptTemplateUpsertRequest request);

    PromptTemplateRecord update(Long id, PromptTemplateUpsertRequest request);

    void delete(Long id);

    PromptSelection resolveSelection(Long promptTemplateId,
                                     PromptPageScope pageScope,
                                     ChatCategory category,
                                     String fallbackSystemPrompt);
}
