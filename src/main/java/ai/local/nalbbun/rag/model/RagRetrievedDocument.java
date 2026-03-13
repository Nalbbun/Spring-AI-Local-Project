package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Builder;

@Builder
public record RagRetrievedDocument(
        String id,
        String title,
        String source,
        String version,
        ChatCategory category,
        String text,
        Double score
) {
}
