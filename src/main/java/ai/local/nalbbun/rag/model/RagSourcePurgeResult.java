package ai.local.nalbbun.rag.model;

import lombok.Builder;

/**
 * RagSourcePurgeResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source purge result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param category 대상 카테고리 정보
 * @param source source 값
 * @param version version 값
 * @param estimatedVectorRowsBefore estimatedVectorRowsBefore 값
 * @param estimatedVectorRowsAfter estimatedVectorRowsAfter 값
 * @param registryEntriesRemoved registryEntriesRemoved 값
 * @param registryDeleted registryDeleted 값
 */
@Builder
public record RagSourcePurgeResult(
        String category,
        String source,
        String version,
        int estimatedVectorRowsBefore,
        int estimatedVectorRowsAfter,
        int registryEntriesRemoved,
        boolean registryDeleted
) {
}
