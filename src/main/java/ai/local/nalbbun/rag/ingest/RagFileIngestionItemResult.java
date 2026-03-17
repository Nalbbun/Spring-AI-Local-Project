package ai.local.nalbbun.rag.ingest;

/**
 * RagFileIngestionItemResult는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag file ingestion item result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param fileId fileId 식별자 값
 * @param fileName fileName 값
 * @param originalFileName originalFileName 값
 * @param contentType contentType 값
 * @param source source 값
 * @param version version 값
 * @param title title 값
 * @param chunkCount chunkCount 값
 * @param stored stored 값
 * @param message 사용자 입력 또는 질의 내용
 */
public record RagFileIngestionItemResult(
        String fileId,
        String fileName,
        String originalFileName,
        String contentType,
        String source,
        String version,
        String title,
        int chunkCount,
        boolean stored,
        String message
) {
    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param fileName fileName 값
     * @param source source 값
     * @param version version 값
     * @param title title 값
     * @param chunkCount chunkCount 값
     * @param stored stored 값
     * @param message 사용자 입력 또는 질의 내용
     */
    public RagFileIngestionItemResult(String fileName,
                                      String source,
                                      String version,
                                      String title,
                                      int chunkCount,
                                      boolean stored,
                                      String message) {
        this(null, fileName, fileName, null, source, version, title, chunkCount, stored, message);
    }
}
