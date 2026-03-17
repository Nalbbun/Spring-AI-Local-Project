package ai.local.nalbbun.rag.reader;

import org.springframework.stereotype.Component;

/**
 * Rag File Type Detector 타입이다.
 *
 * <p>기능 설명: 외부 소스 또는 파일에서 데이터를 읽는다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class RagFileTypeDetector {

    /**
     * detect 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public RagFileType detect(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("업로드 파일명은 비어 있을 수 없습니다.");
        }

        String normalized = filename.toLowerCase();
        if (normalized.endsWith(".pdf")) {
            return RagFileType.PDF;
        }
        if (normalized.endsWith(".md") || normalized.endsWith(".markdown")) {
            return RagFileType.MARKDOWN;
        }
        if (normalized.endsWith(".txt") || normalized.endsWith(".log") || normalized.endsWith(".yaml") || normalized.endsWith(".yml") || normalized.endsWith(".json") || normalized.endsWith(".xml") || normalized.endsWith(".csv")) {
            return RagFileType.TEXT;
        }

        throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. pdf, md, markdown, txt, log, yaml, yml, json, xml, csv 만 지원합니다.");
    }
}
