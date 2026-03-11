package ai.local.nalbbun.category.travel.agent;

import ai.local.nalbbun.category.travel.model.*;
import ai.local.nalbbun.debug.model.RuntimeModelTarget;
import ai.local.nalbbun.service.llm.RuntimeModelChatService;
import org.springframework.stereotype.Component;

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
        - 여행 기간: {days}일
        - 총 예산: {budget}원

        ## 추천 관광지 목록
        {attractions}

        ## 추천 맛집 목록
        {restaurants}

        ## 추천 숙소 목록
        {accommodations}

        ## 일정 작성 규칙
        1. 매일 포함할 항목:
           - 오전 관광지 1-2곳 (09:00-12:00)
           - 점심 식사 (12:00-13:00) - 맛집에서 선택
           - 오후 관광지 1-2곳 (14:00-18:00)
           - 저녁 식사 (18:00-19:00) - 맛집에서 선택
           - 숙소 체크인 (20:00) - 마지막 날 제외

        2. 숙박 규칙:
           - {days}일 여행 = {nights}박
           - 마지막 날에는 숙소가 필요 없음

        3. 중복 방지 규칙:
           - 같은 관광지는 전체 일정에서 단 1번만 방문
           - 같은 맛집은 전체 일정에서 단 1번만 방문
           - 같은 숙소는 전체 일정에서 단 1번만 사용

        4. 일정 작성 형식:
           - 각 일정 항목에 반드시 포함: time, type, name, address, description, cost
           - type은 정확히: 'attraction', 'meal', 'accommodation'
           - 식사는 '점심 - 식당이름' 또는 '저녁 - 식당이름'
        """;

    private final RuntimeModelChatService runtimeModelChatService;

    public TravelPlanAgent(RuntimeModelChatService runtimeModelChatService) {
        this.runtimeModelChatService = runtimeModelChatService;
    }

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

    private String safe(String s) {
        return s == null ? "" : s;
    }
    
    public String describeModel() {
        return runtimeModelChatService.describeResolvedModel(RuntimeModelTarget.TRAVEL_SEARCH, true);
    }
}