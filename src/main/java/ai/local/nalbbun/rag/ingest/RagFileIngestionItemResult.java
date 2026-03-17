package ai.local.nalbbun.rag.ingest;

/**
 * Rag File Ingestion Item Result 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 불변 데이터 전달 객체로 사용된다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
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
     * Rag File Ingestion Item Result 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
