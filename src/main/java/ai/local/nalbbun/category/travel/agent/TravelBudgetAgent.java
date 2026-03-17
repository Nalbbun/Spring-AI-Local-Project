package ai.local.nalbbun.category.travel.agent;

import org.springframework.stereotype.Component;

import ai.local.nalbbun.category.travel.model.BudgetAnalysis;
import ai.local.nalbbun.category.travel.model.DaySchedule;
import ai.local.nalbbun.category.travel.model.Plan;
import ai.local.nalbbun.category.travel.model.ScheduleItem;
import ai.local.nalbbun.category.travel.model.TravelContext;

/**
 * Travel Budget Agent 타입이다.
 *
 * <p>기능 설명: 특정 작업 목적에 맞는 세부 처리 단위를 담당한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelBudgetAgent {

    /**
     * Travel Budget Agent 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public TravelBudgetAgent() {
    }

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * calculate And Update Category Costs 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * describe Model 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describeModel() {
        return "INTERNAL:rule-based-budget-analysis";
    }
}
