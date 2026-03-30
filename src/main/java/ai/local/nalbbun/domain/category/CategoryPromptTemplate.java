package ai.local.nalbbun.domain.category;

import lombok.Builder;
import lombok.Data;

/**
 * Category Prompt Template 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Data
@Builder
public class CategoryPromptTemplate {
    private String systemPrompt;
    private String parsedSummary;
    private String categorySummary;
    private String importantNotes;
    private String recentConversation;
    private String currentUserQuery;

    /**
     * to User Prompt 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * blank To Default 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}