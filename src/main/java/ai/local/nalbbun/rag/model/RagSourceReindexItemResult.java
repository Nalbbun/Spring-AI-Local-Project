package ai.local.nalbbun.rag.model;

import ai.local.nalbbun.rag.ingest.RagIngestionResult;
import lombok.Builder;

/**
 * RagSourceReindexItemResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: rag source reindex item result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param source source 값
 * @param version version 값
 * @param targetVersion targetVersion 값
 * @param title title 값
 * @param storageKind storageKind 값
 * @param success success 값
 * @param detail detail 값
 * @param ingestionResult ingestionResult 값
 */
@Builder
public record RagSourceReindexItemResult(
        String source,
        String version,
        String targetVersion,
        String title,
        String storageKind,
        boolean success,
        String detail,
        RagIngestionResult ingestionResult
) {
}
