package ai.local.nalbbun.rag.ingest;

/**
 * RagIngestionResult는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag ingestion result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param category 대상 카테고리 정보
 * @param source source 값
 * @param version version 값
 * @param title title 값
 * @param chunkCount chunkCount 값
 * @param stored stored 값
 */
public record RagIngestionResult(
        String category,
        String source,
        String version,
        String title,
        int chunkCount,
        boolean stored
) {
}
