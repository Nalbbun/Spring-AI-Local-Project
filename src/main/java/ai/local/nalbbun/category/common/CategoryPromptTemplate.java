package ai.local.nalbbun.category.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryPromptTemplate {
    private String systemPrompt;
    private String parsedSummary;
    private String categorySummary;
    private String importantNotes;
    private String recentConversation;
    private String currentUserQuery;

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

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}