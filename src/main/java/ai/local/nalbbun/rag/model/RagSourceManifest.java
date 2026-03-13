package ai.local.nalbbun.rag.model;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagSourceManifest {
    private ChatCategory category;
    private String source;
    private String sourceKey;
    private String version;
    private String versionKey;
    private String title;
    private String ingestType;
    private String storageKind;
    private String storagePath;
    private String originalFilename;
    private String contentType;
    private String url;
    private String ingestedAt;
    private String lastIndexedAt;
    private int chunkCount;
    private int fileCount;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
