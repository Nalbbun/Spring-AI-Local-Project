package ai.local.nalbbun.rag.service;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.rag.config.RagProperties;
import ai.local.nalbbun.rag.model.RagRetrievedDocument;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RagPromptComposer {

    private final RagProperties ragProperties;

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

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

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
