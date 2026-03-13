package ai.local.nalbbun.rag.model;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

@Data
public class RagSourceFileEntry {
    private ChatCategory category;
    private String source;
    private String sourceKey;
    private String version;
    private String versionKey;
    private String fileId;
    private String fileName;
    private String originalFileName;
    private String title;
    private String ingestType;
    private String storageKind;
    private String storagePath;
    private String contentType;
    private String url;
    private String ingestedAt;
    private String lastIndexedAt;
    private int chunkCount;
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
