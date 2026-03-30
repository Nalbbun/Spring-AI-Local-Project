package ai.local.nalbbun.domain.prompt.model;

/**
 * 실제 응답 생성에 적용된 프롬프트 선택 결과를 표현한다.
 */
public record PromptSelection(
        String source,
        Long promptTemplateId,
        String promptName,
        String systemPrompt
) {

    public static PromptSelection builtin(String systemPrompt) {
        return new PromptSelection("BUILT_IN", null, "내장 기본 프롬프트", systemPrompt);
    }

    public String debugMessage() {
        if (promptTemplateId == null) {
            return "source=%s, name=%s".formatted(source, promptName);
        }
        return "source=%s, templateId=%d, name=%s".formatted(source, promptTemplateId, promptName);
    }
}
