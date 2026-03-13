package ai.local.nalbbun.rag.reader;

import org.springframework.stereotype.Component;

@Component
public class RagFileTypeDetector {

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
