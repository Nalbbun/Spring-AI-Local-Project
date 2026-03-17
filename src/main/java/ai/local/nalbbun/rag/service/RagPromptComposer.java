package ai.local.nalbbun.rag.service;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import lombok.RequiredArgsConstructor;

/**
 * RagPromptComposer는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: rag prompt composer 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class RagPromptComposer {

    /** ragProperties 값을 보관한다. */
    private final RagProperties ragProperties;

    /**
     * compose 기능을 수행한다.
     *
     * @param documents documents 목록 정보
     * @return 처리 결과 문자열
     */
    public String compose(List<RagRetrievedDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[검색된 참고 문서]");

        for (int i = 0; i < documents.size(); i++) {
            RagRetrievedDocument document = documents.get(i);
            builder.append('[').append(i + 1).append("] ")
                    .append("title=").append(blankToDash(document.title()))
                    .append(", source=").append(blankToDash(document.source()))
                    .append(", category=").append(document.category())
                    .append(", score=").append(document.score() == null ? "-" : String.format("%.3f", document.score()))
                    .append("\n")
                    .append(limit(document.text(), 1200))
                    .append("\n\n");
        }

        builder.append("[RAG 답변 원칙]\n")
                .append("- 위 참고 문서에 직접 근거가 있으면 그 내용을 우선 반영하세요.\n")
                .append("- 문서에 없는 내용은 추정이라고 명시하세요.\n");

        if (ragProperties.isIncludeCitations()) {
            builder.append("- 답변에 반영한 근거는 가능하면 [1], [2] 형식으로 표시하세요.\n");
        }

        return builder.toString().trim();
    }

    /**
     * blankToDash 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * limit 기능을 수행한다.
     *
     * @param value value 값
     * @param maxLength maxLength 값
     * @return 처리 결과 문자열
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "(본문 없음)";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + " ...";
    }
}
