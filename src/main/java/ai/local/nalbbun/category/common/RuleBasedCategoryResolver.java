package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

/**
 * RuleBasedCategoryResolver는 조건에 따라 적절한 대상이나 값을 해석하는 리졸버이다.
 * <p>주요 기능: rule based category resolver 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuleBasedCategoryResolver   {
 
    /**
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return CategoryResolution 타입의 처리 결과
     */
    public CategoryResolution resolve(String userQuery) {
        String q = userQuery == null ? "" : userQuery.toLowerCase();

        if (containsAny(q, "여행", "관광", "숙소", "호텔", "펜션", "맛집", "일정", "2박3일", "가볼만한")) {
            return new CategoryResolution(ChatCategory.TRAVEL, 95, mode(), "travel keyword matched");
        }

        if (containsAny(q, "자바", "스프링", "spring", "kubernetes", "쿠버네티스", "docker", "도커",
                "git", "jenkins", "helm", "yaml", "gradle", "리팩토링", "코드", "개발")) {
            return new CategoryResolution(ChatCategory.DEV, 95, mode(), "dev keyword matched");
        }

        if (containsAny(q, "행사", "컨퍼런스", "포럼", "전시", "박람회", "제안서", "운영", "의전", "프로그램",
                "mice", "연사", "참가자", "현장 운영")) {
            return new CategoryResolution(ChatCategory.MICE, 95, mode(), "mice keyword matched");
        }

        return new CategoryResolution(ChatCategory.GENERAL, 60, mode(), "no strong keyword, fallback general");
    }
 
    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String mode() {
        return "RULE";
    }

    /**
     * containsAny 기능을 수행한다.
     *
     * @param text 본문 또는 텍스트 내용
     * @param keywords keywords 값
     * @return 처리 가능 여부 또는 조건 충족 여부
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}