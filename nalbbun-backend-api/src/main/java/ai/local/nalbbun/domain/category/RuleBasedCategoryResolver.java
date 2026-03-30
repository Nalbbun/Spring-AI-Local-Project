package ai.local.nalbbun.domain.category;

import ai.local.nalbbun.domain.category.model.CategoryResolution;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import org.springframework.stereotype.Component;

/**
 * Rule Based Category Resolver 타입이다.
 *
 * <p>기능 설명: 입력 조건을 해석해 적절한 선택 결과를 도출한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class RuleBasedCategoryResolver   {
 
    /**
     * resolve 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String mode() {
        return "RULE";
    }

    /**
     * contains Any 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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