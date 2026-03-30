package ai.local.nalbbun.domain.category.travel.workflow;

import ai.local.nalbbun.domain.category.travel.model.Accommodation;
import ai.local.nalbbun.domain.category.travel.model.Attraction;
import ai.local.nalbbun.domain.category.travel.model.BudgetAnalysis;
import ai.local.nalbbun.domain.category.travel.model.DaySchedule;
import ai.local.nalbbun.domain.category.travel.model.Plan;
import ai.local.nalbbun.domain.category.travel.model.Restaurant;
import ai.local.nalbbun.domain.category.travel.model.ScheduleItem;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.common.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Travel Replanner — 예산 초과 시 LLM 재호출 없이 로컬에서 비용 절감 재계획을 수행합니다.
 *
 * 기존 방식: LLM을 다시 호출해서 새 Plan 생성 → 타임아웃 2배 위험
 * 변경 방식: 기존 Plan의 ScheduleItem 비용을 로컬에서 조정
 *   1. 숙소를 가장 저렴한 것으로 교체
 *   2. 식사를 가장 저렴한 맛집으로 교체
 *   3. 입장료가 없는 관광지 우선 배치
 *   4. 조정 후 BudgetAnalysis 재계산
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelReplanner {

    private final AgentEventPublisher agentEventPublisher;

    public void replan(TravelContext context, SseEmitter emitter) {
        agentEventPublisher.send(emitter, "TravelReplanner", "warning",
            "예산 초과 - 로컬 비용 절감 재계획 수행 중 (LLM 재호출 없음)");

        if (context.getPlan() == null || context.getPlan().getDays() == null) {
            agentEventPublisher.send(emitter, "TravelReplanner", "skipped", "일정 데이터 없음, 재계획 불가");
            return;
        }

        context.setReplan(true);
        int previousTotal = context.getPlan().getTotalCost() != null
                ? context.getPlan().getTotalCost() : 0;
        context.setPreviousTotalCost(previousTotal);

        // 저렴한 대안 목록 준비
        List<Accommodation> cheapAccom = sortedAccom(context);
        List<Restaurant>    cheapRest  = sortedRest(context);
        List<Attraction>    freeAttr   = freeAttractions(context);

        // 기존 Plan ScheduleItem 비용 조정
        adjustPlanCosts(context.getPlan(), cheapAccom, cheapRest, freeAttr, context.getMaxBudget());

        // BudgetAnalysis 재계산
        int newTotal = recalcTotal(context.getPlan());
        context.getPlan().setTotalCost(newTotal);
        boolean stillExceeded = newTotal > context.getMaxBudget();
        int saved = previousTotal - newTotal;

        BudgetAnalysis newAnalysis = new BudgetAnalysis(
            context.getMaxBudget(),
            newTotal,
            stillExceeded,
            String.format("총 비용: %,d원 | 예산: %,d원 | 절감: %,d원 | %s",
                newTotal, context.getMaxBudget(), saved,
                stillExceeded ? "⚠️ 여전히 초과" : "✅ 예산 내 조정 완료")
        );
        context.setBudgetAnalysis(newAnalysis);

        agentEventPublisher.send(emitter, "TravelReplanner", "complete", newAnalysis.getMessage());
        log.info("TravelReplanner 완료. previousTotal={}, newTotal={}, saved={}", previousTotal, newTotal, saved);
    }

    // ── 비용 조정 로직 ──────────────────────────────────────────────────

    private void adjustPlanCosts(Plan plan,
                                  List<Accommodation> cheapAccom,
                                  List<Restaurant>    cheapRest,
                                  List<Attraction>    freeAttr,
                                  int maxBudget) {
        int accomIdx = 0;
        int restIdx  = 0;

        for (DaySchedule day : plan.getDays()) {
            if (day.getSchedule() == null) continue;

            for (ScheduleItem item : day.getSchedule()) {
                String type = item.getType() == null ? "" : item.getType().toLowerCase();

                switch (type) {
                    case "accommodation" -> {
                        // 가장 저렴한 숙소로 교체
                        if (accomIdx < cheapAccom.size()) {
                            Accommodation a = cheapAccom.get(accomIdx++);
                            item.setName(a.getName());
                            item.setAddress(a.getAddress() != null ? a.getAddress() : item.getAddress());
                            item.setDescription(a.getDescription() != null ? a.getDescription() : "저비용 숙소");
                            item.setCost(a.getPricePerNight());
                        } else if (item.getCost() > 0) {
                            // 대안 없으면 30% 할인
                            item.setCost((int)(item.getCost() * 0.7));
                        }
                    }
                    case "meal" -> {
                        // 가장 저렴한 맛집으로 교체
                        if (restIdx < cheapRest.size()) {
                            Restaurant r = cheapRest.get(restIdx++ % cheapRest.size());
                            item.setName(item.getName() != null && item.getName().contains("점심")
                                ? "점심 - " + r.getName() : "저녁 - " + r.getName());
                            item.setCost(r.getPrice());
                        } else if (item.getCost() > 0) {
                            item.setCost((int)(item.getCost() * 0.7));
                        }
                    }
                    case "attraction" -> {
                        // 무료 관광지로 교체 가능하면 교체
                        if (!freeAttr.isEmpty() && item.getCost() > 0) {
                            Attraction fa = freeAttr.get(0);
                            item.setName(fa.getName());
                            item.setAddress(fa.getAddress() != null ? fa.getAddress() : item.getAddress());
                            item.setDescription(fa.getDescription() != null ? fa.getDescription() : "무료 관광지");
                            item.setCost(0);
                            freeAttr.remove(0);
                        } else if (item.getCost() > 0) {
                            item.setCost((int)(item.getCost() * 0.6));
                        }
                    }
                }
            }
        }
    }

    private int recalcTotal(Plan plan) {
        int total = 0;
        for (DaySchedule day : plan.getDays()) {
            if (day.getSchedule() == null) continue;
            for (ScheduleItem item : day.getSchedule()) {
                total += item.getCost();
            }
        }
        return total;
    }

    // ── 정렬 유틸 ────────────────────────────────────────────────────────

    private List<Accommodation> sortedAccom(TravelContext ctx) {
        if (ctx.getAccommodations() == null) return List.of();
        return new ArrayList<>(ctx.getAccommodations()).stream()
                .sorted(Comparator.comparingInt(Accommodation::getPricePerNight))
                .toList();
    }

    private List<Restaurant> sortedRest(TravelContext ctx) {
        if (ctx.getRestaurants() == null) return List.of();
        return new ArrayList<>(ctx.getRestaurants()).stream()
                .sorted(Comparator.comparingInt(Restaurant::getPrice))
                .toList();
    }

    private List<Attraction> freeAttractions(TravelContext ctx) {
        if (ctx.getAttractions() == null) return new ArrayList<>();
        return new ArrayList<>(ctx.getAttractions()).stream()
                .filter(a -> a.getEntranceFee() == 0)
                .sorted(Comparator.comparing(Attraction::getName))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
