package ai.local.nalbbun.category.travel.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RuleBasedTravelParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: rule based travel parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class RuleBasedTravelParser implements CategoryParsingStrategy<TravelContext> {

    /** NIGHTS_DAYS 값을 보관한다. */
    private static final Pattern NIGHTS_DAYS = Pattern.compile("(\\d+)박\\s*(\\d+)일");
    /** DAYS_ONLY 값을 보관한다. */
    private static final Pattern DAYS_ONLY = Pattern.compile("(\\d+)일");
    /** MAN_WON 값을 보관한다. */
    private static final Pattern MAN_WON = Pattern.compile("(\\d+)\\s*만원");
    /** WON 값을 보관한다. */
    private static final Pattern WON = Pattern.compile("(\\d{5,})\\s*원?");

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return TravelContext 타입의 처리 결과
     */
    @Override
    public TravelContext parse(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery();
        context.setUserQuery(userQuery);

        if (context.getDestination() == null) {
            context.setDestination(extractDestination(userQuery));
        }
        if (context.getDays() == null) {
            context.setDays(extractDays(userQuery));
        }
        if (context.getMaxBudget() == null) {
            context.setMaxBudget(extractBudget(userQuery));
        }

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
     * extractDestination 기능을 수행한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    private String extractDestination(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            return "제주도";
        }

        String q = userQuery.toLowerCase();
        if (q.contains("제주")) return "제주도";
        if (q.contains("부산")) return "부산";
        if (q.contains("서울")) return "서울";
        if (q.contains("강릉")) return "강릉";
        if (q.contains("여수")) return "여수";
        if (q.contains("속초")) return "속초";
        if (q.contains("전주")) return "전주";
        return "제주도";
    }

    /**
     * extractDays 기능을 수행한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return int 타입의 처리 결과
     */
    private int extractDays(String userQuery) {
        if (userQuery == null) {
            return 2;
        }

        Matcher nightsDays = NIGHTS_DAYS.matcher(userQuery);
        if (nightsDays.find()) {
            return Integer.parseInt(nightsDays.group(2));
        }

        Matcher daysOnly = DAYS_ONLY.matcher(userQuery);
        if (daysOnly.find()) {
            return Integer.parseInt(daysOnly.group(1));
        }

        return 2;
    }

    /**
     * extractBudget 기능을 수행한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return int 타입의 처리 결과
     */
    private int extractBudget(String userQuery) {
        if (userQuery == null) {
            return 500000;
        }

        String cleaned = userQuery.replace(",", "");

        Matcher manWon = MAN_WON.matcher(cleaned);
        if (manWon.find()) {
            return Integer.parseInt(manWon.group(1)) * 10_000;
        }

        Matcher won = WON.matcher(cleaned);
        if (won.find()) {
            return Integer.parseInt(won.group(1));
        }

        return 500000;
    }
}