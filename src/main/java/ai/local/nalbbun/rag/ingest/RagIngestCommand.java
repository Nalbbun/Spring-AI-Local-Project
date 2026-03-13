package ai.local.nalbbun.rag.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagIngestCommand {
    private ChatCategory category;
    private String source;
    private String version;
    private String title;
    private String text;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
