package ai.local.nalbbun.admin.model.llm;

import lombok.Data;

@Data
public class VllmChatTestRequest {
    private String mode;
    private String model;
    private String systemPrompt;
    private String userPrompt;
}
