package ai.local.nalbbun.rag.reader;

import org.springframework.stereotype.Component;

/**
 * RagFileTypeDetector는 RAG 관련 처리와 관리 기능을 담당하는 컴포넌트이다.
 * <p>주요 기능: rag file type detector 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RagFileTypeDetector {

    /**
     * detect 기능을 수행한다.
     *
     * @param filename filename 값
     * @return RagFileType 타입의 처리 결과
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
