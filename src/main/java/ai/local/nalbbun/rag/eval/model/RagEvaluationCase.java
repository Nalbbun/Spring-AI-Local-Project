package ai.local.nalbbun.rag.eval.model;

import java.util.List;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

/**
 * RagEvaluationCase는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag evaluation case 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class RagEvaluationCase {
    /** id 값을 보관한다. */
    private String id;
    /** category 값을 보관한다. */
    private ChatCategory category;
    /** query 값을 보관한다. */
    private String query;
    /** source 값을 보관한다. */
    private String source;
    /** version 값을 보관한다. */
    private String version;
    /** minHits 값을 보관한다. */
    private int minHits = 1;
    /** expectedSources 값을 보관한다. */
    private List<String> expectedSources;
    /** expectedVersions 값을 보관한다. */
    private List<String> expectedVersions;
}
