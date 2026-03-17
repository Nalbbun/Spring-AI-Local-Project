package ai.local.nalbbun.rag.ingest;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

/**
 * RagUrlIngestCommand는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag url ingest command 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class RagUrlIngestCommand {
    /** category 값을 보관한다. */
    private ChatCategory category;
    /** url 값을 보관한다. */
    private String url;
    /** source 값을 보관한다. */
    private String source;
    /** version 값을 보관한다. */
    private String version;
    /** title 값을 보관한다. */
    private String title;
    /** metadata 값을 보관한다. */
    private Map<String, Object> metadata = new LinkedHashMap<>();
}
