package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

/**
 * RagSourceReindexCommand는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source reindex command 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class RagSourceReindexCommand {
    /** category 값을 보관한다. */
    private ChatCategory category;
    /** source 값을 보관한다. */
    private String source;
    /** version 값을 보관한다. */
    private String version;
    /** targetVersion 값을 보관한다. */
    private String targetVersion;
    /** purgeBeforeReindex 값을 보관한다. */
    private boolean purgeBeforeReindex = true;
    /** copyToNewVersion 값을 보관한다. */
    private boolean copyToNewVersion = false;
}
