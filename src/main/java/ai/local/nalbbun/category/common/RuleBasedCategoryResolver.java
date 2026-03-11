package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedCategoryResolver   {
 
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
 
    public String mode() {
        return "RULE";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}