package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagSourcePurgeCommand {
    private ChatCategory category;
    private String source;
    private String version;
    private boolean deleteRegistry = false;
}
