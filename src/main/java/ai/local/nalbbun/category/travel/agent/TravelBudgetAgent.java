package ai.local.nalbbun.category.travel.agent;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.travel.model.BudgetAnalysis;
import ai.local.nalbbun.category.travel.model.DaySchedule;
import ai.local.nalbbun.category.travel.model.Plan;
import ai.local.nalbbun.category.travel.model.ScheduleItem;
import ai.local.nalbbun.category.travel.model.TravelContext;

/**
 * TravelBudgetAgent는 세부 업무를 분리하여 수행하는 에이전트이다.
 * <p>주요 기능: travel budget agent 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelBudgetAgent {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     */
    public TravelBudgetAgent() {
    }

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     */
    public void execute(TravelContext context) {
        Integer maxBudget = context.getMaxBudget();
        Plan plan = context.getPlan();

        if (plan == null) {
            context.setBudgetAnalysis(new BudgetAnalysis(
                    maxBudget,
                    0,
                    false,
                    "생성된 여행 계획이 없어 예산 분석을 건너뜁니다."
            ));
            return;
        }

        calculateAndUpdateCategoryCosts(plan);

        int actualTotalCost = plan.getTotalCost() == null ? 0 : plan.getTotalCost();
        int budget = maxBudget == null ? 0 : maxBudget;
        boolean exceeded = actualTotalCost > budget;

        String message = String.format(
                "총 비용: %,d원 | 예산: %,d원 | %s",
                actualTotalCost,
                budget,
                exceeded ? "⚠️ 초과" : "✅ 정상"
        );

        context.setBudgetAnalysis(new BudgetAnalysis(
                budget,
                actualTotalCost,
                exceeded,
                message
        ));
    }

    /**
     * calculateAndUpdateCategoryCosts 기능을 수행한다.
     *
     * @param plan plan 값
     */
    private void calculateAndUpdateCategoryCosts(Plan plan) {
        int mealsCost = 0;
        int accommodationCost = 0;
        int attractionsCost = 0;

        if (plan.getDays() != null) {
            for (DaySchedule day : plan.getDays()) {
                if (day.getSchedule() != null) {
                    for (ScheduleItem item : day.getSchedule()) {
                        String type = item.getType();
                        int cost = item.getCost();

                        if ("meal".equals(type)) {
                            mealsCost += cost;
                        } else if ("accommodation".equals(type)) {
                            accommodationCost += cost;
                        } else if ("attraction".equals(type)) {
                            attractionsCost += cost;
                        }
                    }
                }
            }
        }

        plan.setMeals(mealsCost);
        plan.setAccommodation(accommodationCost);
        plan.setAttractions(attractionsCost);
        plan.setTotalCost(mealsCost + accommodationCost + attractionsCost);
    }

    /**
     * describeModel 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String describeModel() {
        return "INTERNAL:rule-based-budget-analysis";
    }
}
