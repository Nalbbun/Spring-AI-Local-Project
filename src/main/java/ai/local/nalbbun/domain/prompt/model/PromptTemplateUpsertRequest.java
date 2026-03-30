package ai.local.nalbbun.domain.prompt.model;

import ai.local.nalbbun.domain.category.model.ChatCategory;

/**
 * 프롬프트 생성/수정 요청을 표현한다.
 */
public record PromptTemplateUpsertRequest(
        String name,
        String description,
        PromptPageScope pageScope,
        ChatCategory category,
        String systemPrompt,
        Boolean active,
        Boolean defaultPrompt
) {
}
