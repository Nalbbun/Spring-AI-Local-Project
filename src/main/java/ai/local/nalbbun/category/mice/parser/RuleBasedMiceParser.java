package ai.local.nalbbun.category.mice.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

/**
 * RuleBasedMiceParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: rule based mice parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuleBasedMiceParser implements CategoryParsingStrategy<MiceContext> {

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return MiceContext 타입의 처리 결과
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
     * @return 처리 결과 문자열
     */
    @Override
    public String mode() {
        return "RULE";
    }

    /**
     * inferEventType 기능을 수행한다.
     *
     * @param q q 값
     * @return 처리 결과 문자열
     */
    private String inferEventType(String q) {
        if (containsAny(q, "포럼")) return "forum";
        if (containsAny(q, "컨퍼런스", "conference")) return "conference";
        if (containsAny(q, "전시", "박람회", "expo")) return "exhibition";
        if (containsAny(q, "공연", "페스티벌")) return "festival";
        return "mice-event";
    }

    /**
     * inferDeliverable 기능을 수행한다.
     *
     * @param q q 값
     * @return 처리 결과 문자열
     */
    private String inferDeliverable(String q) {
        if (containsAny(q, "제안서", "기획안")) return "proposal";
        if (containsAny(q, "운영안", "운영")) return "operations";
        if (containsAny(q, "프로그램", "세션")) return "program";
        if (containsAny(q, "슬로건", "메시지", "브랜딩")) return "branding";
        return "strategy";
    }

    /**
     * inferRegion 기능을 수행한다.
     *
     * @param q q 값
     * @return 처리 결과 문자열
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