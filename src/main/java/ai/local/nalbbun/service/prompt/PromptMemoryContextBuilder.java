package ai.local.nalbbun.service.prompt;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.prompt.PromptMemoryContext;
import ai.local.nalbbun.service.memory.ConversationMemoryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptMemoryContextBuilder {

    private final ConversationMemoryService memoryService;

    public PromptMemoryContextBuilder(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public PromptMemoryContext build(String conversationId, ChatCategory category) {
        String summary = safe(memoryService.getCategorySummary(conversationId, category));
        List<String> notes = memoryService.getImportantNotes(conversationId, category);
        String recentConversation = safe(memoryService.formatRecentConversation(conversationId, 10));

        String importantNotesBlock = notes == null || notes.isEmpty()
                ? "(중요 메모 없음)"
                : notes.stream()
                    .map(note -> "- " + note)
                    .collect(Collectors.joining("\n"));

        if (summary.isBlank()) {
            summary = "(카테고리 요약 없음)";
        }

        if (recentConversation.isBlank()) {
            recentConversation = "(최근 대화 없음)";
        }

        return new PromptMemoryContext(
                summary,
                importantNotesBlock,
                recentConversation
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}