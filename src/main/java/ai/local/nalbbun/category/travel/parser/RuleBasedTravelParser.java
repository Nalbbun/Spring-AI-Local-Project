package ai.local.nalbbun.category.travel.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.common.ConversationState;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule Based Travel Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class RuleBasedTravelParser implements CategoryParsingStrategy<TravelContext> {

    private static final Pattern NIGHTS_DAYS = Pattern.compile("(\\d+)박\\s*(\\d+)일");
    private static final Pattern DAYS_ONLY = Pattern.compile("(\\d+)일");
    private static final Pattern MAN_WON = Pattern.compile("(\\d+)\\s*만원");
    private static final Pattern WON = Pattern.compile("(\\d{5,})\\s*원?");

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String mode() {
        return "RULE";
    }

    /**
     * extract Destination 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * extract Days 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     * extract Budget 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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