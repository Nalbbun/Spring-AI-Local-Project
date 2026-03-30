package ai.local.nalbbun.domain.rag.model;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import lombok.Data;

/**
 * Rag Source Manifest 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 category, source, sourceKey, version, versionKey, title, ingestType, storageKind 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
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
