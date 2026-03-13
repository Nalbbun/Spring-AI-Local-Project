package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagSourceReindexCommand {
    private ChatCategory category;
    private String source;
    private String version;
    private String targetVersion;
    private boolean purgeBeforeReindex = true;
    private boolean copyToNewVersion = false;
}
