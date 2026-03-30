package ai.local.nalbbun.domain.rag.service;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.config.rag.RagProperties;
import ai.local.nalbbun.domain.rag.model.RagRetrievedDocument;
import lombok.RequiredArgsConstructor;

/**
 * Rag Prompt Composer 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Component
@RequiredArgsConstructor
public class RagPromptComposer {

    private final RagProperties ragProperties;

    /**
     * compose 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * blank To Dash 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * limit 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
