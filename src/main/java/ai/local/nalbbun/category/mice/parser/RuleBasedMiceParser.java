package ai.local.nalbbun.category.mice.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

/**
 * Rule Based Mice Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class RuleBasedMiceParser implements CategoryParsingStrategy<MiceContext> {

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public MiceContext parse(ConversationState state, MiceContext context) {
        String q = state.getUserQuery() == null ? "" : state.getUserQuery().toLowerCase();

        context.setEventType(inferEventType(q));
        context.setDeliverableType(inferDeliverable(q));
        context.setTargetRegion(inferRegion(q));
        return context;
    }

    /**
     * mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String mode() {
        return "RULE";
    }

    /**
     * infer Event Type 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String inferEventType(String q) {
        if (containsAny(q, "포럼")) return "forum";
        if (containsAny(q, "컨퍼런스", "conference")) return "conference";
        if (containsAny(q, "전시", "박람회", "expo")) return "exhibition";
        if (containsAny(q, "공연", "페스티벌")) return "festival";
        return "mice-event";
    }

    /**
     * infer Deliverable 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String inferDeliverable(String q) {
        if (containsAny(q, "제안서", "기획안")) return "proposal";
        if (containsAny(q, "운영안", "운영")) return "operations";
        if (containsAny(q, "프로그램", "세션")) return "program";
        if (containsAny(q, "슬로건", "메시지", "브랜딩")) return "branding";
        return "strategy";
    }

    /**
     * infer Region 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String inferRegion(String q) {
        if (containsAny(q, "태국", "방콕")) return "thailand";
        if (containsAny(q, "베트남", "호치민", "하노이")) return "vietnam";
        if (containsAny(q, "말레이시아", "쿠알라룸푸르")) return "malaysia";
        if (containsAny(q, "부탄")) return "bhutan";
        if (containsAny(q, "한국", "서울", "부산", "제주")) return "korea";
        return "global";
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