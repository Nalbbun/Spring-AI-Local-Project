package ai.local.nalbbun.rag.model;

import java.util.List;

import lombok.Builder;

/**
 * RagSourceReindexResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source reindex result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param category 대상 카테고리 정보
 * @param source source 값
 * @param version version 값
 * @param targetVersion targetVersion 값
 * @param totalCandidates totalCandidates 값
 * @param successCount successCount 값
 * @param results results 값
 */
@Builder
public record RagSourceReindexResult(
        String category,
        String source,
        String version,
        String targetVersion,
        int totalCandidates,
        int successCount,
        List<RagSourceReindexItemResult> results
) {
}
