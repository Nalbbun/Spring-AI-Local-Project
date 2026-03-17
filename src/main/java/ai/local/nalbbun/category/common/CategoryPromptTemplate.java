package ai.local.nalbbun.category.common;

import lombok.Builder;
import lombok.Data;

/**
 * CategoryPromptTemplate는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: category prompt template 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
@Builder
public class CategoryPromptTemplate {
    /** systemPrompt 값을 보관한다. */
    private String systemPrompt;
    /** parsedSummary 값을 보관한다. */
    private String parsedSummary;
    /** categorySummary 값을 보관한다. */
    private String categorySummary;
    /** importantNotes 값을 보관한다. */
    private String importantNotes;
    /** recentConversation 값을 보관한다. */
    private String recentConversation;
    /** currentUserQuery 값을 보관한다. */
    private String currentUserQuery;

    /**
     * 현재 상태를 다른 표현 형태로 변환한다.
     * @return 처리 결과 문자열
     */
    public String toUserPrompt() {
        return String.format("""
            [파싱 요약]
            %s

            [카테고리 요약]
            %s

            [중요 메모]
            %s

            [최근 대화]
            %s

            [현재 사용자 질문]
            %s
            """,
            blankToDefault(parsedSummary, "(파싱 정보 없음)"),
            blankToDefault(categorySummary, "(카테고리 요약 없음)"),
            blankToDefault(importantNotes, "(중요 메모 없음)"),
            blankToDefault(recentConversation, "(최근 대화 없음)"),
            blankToDefault(currentUserQuery, "")
        );
    }

    /**
     * 값이 비어 있을 때 기본값으로 대체한다.
     *
     * @param value value 값
     * @param defaultValue defaultValue 값
     * @return 처리 결과 문자열
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}