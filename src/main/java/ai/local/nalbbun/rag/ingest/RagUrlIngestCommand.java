package ai.local.nalbbun.rag.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagUrlIngestCommand {
    private ChatCategory category;
    private String url;
    private String source;
    private String version;
    private String title;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
