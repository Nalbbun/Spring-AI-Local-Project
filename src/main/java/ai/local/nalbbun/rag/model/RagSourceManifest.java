package ai.local.nalbbun.rag.model;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

/**
 * RagSourceManifest는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source manifest 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class RagSourceManifest {
    /** category 값을 보관한다. */
    private ChatCategory category;
    /** source 값을 보관한다. */
    private String source;
    /** sourceKey 값을 보관한다. */
    private String sourceKey;
    /** version 값을 보관한다. */
    private String version;
    /** versionKey 값을 보관한다. */
    private String versionKey;
    /** title 값을 보관한다. */
    private String title;
    /** ingestType 값을 보관한다. */
    private String ingestType;
    /** storageKind 값을 보관한다. */
    private String storageKind;
    /** storagePath 값을 보관한다. */
    private String storagePath;
    /** originalFilename 값을 보관한다. */
    private String originalFilename;
    /** contentType 값을 보관한다. */
    private String contentType;
    /** url 값을 보관한다. */
    private String url;
    /** ingestedAt 값을 보관한다. */
    private String ingestedAt;
    /** lastIndexedAt 값을 보관한다. */
    private String lastIndexedAt;
    /** chunkCount 값을 보관한다. */
    private int chunkCount;
    /** fileCount 값을 보관한다. */
    private int fileCount;
    /** metadata 값을 보관한다. */
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
