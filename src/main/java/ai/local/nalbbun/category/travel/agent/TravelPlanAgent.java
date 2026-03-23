package ai.local.nalbbun.category.travel.agent;

import ai.local.nalbbun.category.travel.model.*;
import ai.local.nalbbun.internal.model.RuntimeModelTarget;
import ai.local.nalbbun.llm.service.RuntimeModelChatService;
import org.springframework.stereotype.Component;

/**
 * Travel Plan Agent 타입이다.
 *
 * <p>기능 설명: 특정 작업 목적에 맞는 세부 처리 단위를 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelPlanAgent {

    private static final String SYSTEM_PROMPT = """
        당신은 여행 일정을 계획하는 전문 에이전트입니다.
        주어진 여행 정보와 장소 목록을 바탕으로 실용적이고 균형잡힌 여행 일정을 작성합니다.
        예산을 고려하면서도 여행자가 충분히 즐길 수 있도록 다양한 장소를 선택합니다.
        """;

    private static final String USER_PROMPT_TEMPLATE = """
        {destination} {days}일 여행 일정을 작성해주세요.

        ## 여행 정보
        - 여행 기간: {days}일 ({nights}박)
        - 총 예산: {budget}원

        ## 추천 관광지 목록
        {attractions}

        ## 추천 맛집 목록
        {restaurants}

        ## 추천 숙소 목록
        {accommodations}

        ## 일정 작성 규칙
        - 매일: 오전 관광지(09:00), 점심(12:00), 오후 관광지(14:00), 저녁(18:00), 숙소체크인(20:00, 마지막날 제외)
        - 숙박: {nights}박, 마지막 날 숙소 불필요
        - 중복 금지: 동일 장소/맛집/숙소 반복 사용 금지
        - 각 항목: time, type(attraction/meal/accommodation), name, address, description, cost 포함
        """;

    private final RuntimeModelChatService runtimeModelChatService;

    /**
     * Travel Plan Agent 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public TravelPlanAgent(RuntimeModelChatService runtimeModelChatService) {
        this.runtimeModelChatService = runtimeModelChatService;
    }

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public void execute(TravelContext context) {
        String prompt = buildTravelPlanPrompt(context);

        Plan plan = runtimeModelChatService.callEntity(
                RuntimeModelTarget.TRAVEL_PLAN,
                SYSTEM_PROMPT,
                prompt,
                Plan.class
        );

        if (plan != null && plan.getMaxBudget() == null) {
            plan.setMaxBudget(context.getMaxBudget());
        }

        context.setPlan(plan);
    }

    /**
     * build Travel Plan Prompt 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String buildTravelPlanPrompt(TravelContext context) {
        StringBuilder attractions = new StringBuilder();
        if (context.getAttractions() != null) {
            for (Attraction attr : context.getAttractions()) {
                attractions.append("- ").append(attr.getName())
                        .append(" (입장료: ").append(String.format("%,d", attr.getEntranceFee())).append("원)\n")
                        .append("  위치: ").append(attr.getAddress()).append("\n")
                        .append("  설명: ").append(attr.getDescription()).append("\n");
            }
        }

        StringBuilder restaurants = new StringBuilder();
        if (context.getRestaurants() != null) {
            for (Restaurant rest : context.getRestaurants()) {
                restaurants.append("- ").append(rest.getName())
                        .append(" (평균 가격: ").append(String.format("%,d", rest.getPrice())).append("원)\n")
                        .append("  위치: ").append(rest.getAddress()).append("\n")
                        .append("  메뉴: ").append(rest.getDescription()).append("\n");
            }
        }

        StringBuilder accommodations = new StringBuilder();
        if (context.getAccommodations() != null) {
            for (Accommodation acc : context.getAccommodations()) {
                accommodations.append("- ").append(acc.getName())
                        .append(" (1박: ").append(String.format("%,d", acc.getPricePerNight())).append("원)\n")
                        .append("  위치: ").append(acc.getAddress()).append("\n")
                        .append("  특징: ").append(acc.getDescription()).append("\n");
            }
        }

        String prompt = USER_PROMPT_TEMPLATE
                .replace("{destination}", safe(context.getDestination()))
                .replace("{days}", String.valueOf(context.getDays()))
                .replace("{nights}", String.valueOf(context.getDays() - 1))
                .replace("{budget}", String.format("%,d", context.getMaxBudget()))
                .replace("{attractions}", attractions.toString())
                .replace("{restaurants}", restaurants.toString())
                .replace("{accommodations}", accommodations.toString());

        if (context.isReplan() && context.getPreviousTotalCost() != null) {
            int exceededAmount = context.getPreviousTotalCost() - context.getMaxBudget();

            String replanWarning = String.format("""
                                
                **예산 재계획 필수**
                이전 일정이 예산을 %,d원 초과했습니다. (이전 총비용: %,d원)
                반드시 더 저렴한 관광지, 맛집, 숙소를 선택하여
                총 예산 %,d원 이내로 일정을 재작성해야 합니다.
                """,
                    exceededAmount,
                    context.getPreviousTotalCost(),
                    context.getMaxBudget());

            prompt = prompt + replanWarning;
        }

        return prompt;
    }

    /**
     * safe 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }
    
    /**
     * describe Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describeModel() {
        return runtimeModelChatService.describeResolvedModel(RuntimeModelTarget.TRAVEL_PLAN, false);
    }
}