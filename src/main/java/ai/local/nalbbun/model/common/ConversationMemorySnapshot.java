package ai.local.nalbbun.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemorySnapshot {
    private String conversationId;
    private List<MemoryMessage> recentMessages;
    private Map<String, MemorySummary> categorySummaries;
    private List<ImportantNote> importantNotes;
}