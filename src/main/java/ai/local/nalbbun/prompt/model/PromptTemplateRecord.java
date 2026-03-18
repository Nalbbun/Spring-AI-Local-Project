package ai.local.nalbbun.prompt.model;

import ai.local.nalbbun.category.model.ChatCategory;

import java.time.LocalDateTime;

/**
 * 저장된 프롬프트 템플릿 정보를 표현한다.
 */
public record PromptTemplateRecord(
        Long id,
        String name,
        String description,
        PromptPageScope pageScope,
        ChatCategory category,
        String systemPrompt,
        boolean active,
        boolean defaultPrompt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
