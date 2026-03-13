package ai.local.nalbbun.category.travel.agent;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.travel.model.BudgetAnalysis;
import ai.local.nalbbun.category.travel.model.DaySchedule;
import ai.local.nalbbun.category.travel.model.Plan;
import ai.local.nalbbun.category.travel.model.ScheduleItem;
import ai.local.nalbbun.category.travel.model.TravelContext;

@Component
public class TravelBudgetAgent {

    public TravelBudgetAgent() {
    }

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

    public String describeModel() {
        return "INTERNAL:rule-based-budget-analysis";
    }
}
