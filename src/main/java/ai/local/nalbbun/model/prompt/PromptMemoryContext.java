package ai.local.nalbbun.model.prompt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptMemoryContext {
    private String categorySummary;
    private String importantNotesBlock;
    private String recentConversationBlock;
}