package ai.local.nalbbun.category.travel.workflow;

import ai.local.nalbbun.category.travel.agent.TravelBudgetAgent;
import ai.local.nalbbun.category.travel.agent.TravelPlanAgent;
import ai.local.nalbbun.category.travel.model.Accommodation;
import ai.local.nalbbun.category.travel.model.Attraction;
import ai.local.nalbbun.category.travel.model.Restaurant;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TravelReplanner {

    private final TravelPlanAgent travelPlanAgent;
    private final TravelBudgetAgent travelBudgetAgent;
    private final AgentEventPublisher agentEventPublisher;

    public void replan(TravelContext context, SseEmitter emitter) {
        agentEventPublisher.send(emitter, "TravelReplanner", "warning", "예산 초과 - 저비용 기준으로 재계획합니다.");

        context.setReplan(true);
        if (context.getPlan() != null) {
            context.setPreviousTotalCost(context.getPlan().getTotalCost());
        }

        List<Attraction> cheapAttractions = context.getAttractions().stream()
                .sorted(Comparator.comparingInt(Attraction::getEntranceFee))
                .toList();

        List<Restaurant> cheapRestaurants = context.getRestaurants().stream()
                .sorted(Comparator.comparingInt(Restaurant::getPrice))
                .toList();

        List<Accommodation> cheapAccommodations = context.getAccommodations().stream()
                .sorted(Comparator.comparingInt(Accommodation::getPricePerNight))
                .toList();

  
        context.setAttractions(cheapAttractions);
        context.setRestaurants(cheapRestaurants);
        context.setAccommodations(cheapAccommodations);
        
        agentEventPublisher.send(
                emitter,
                "ModelTrace-TRAVEL_PLAN",
                "info",
                "replan=" + travelPlanAgent.describeModel()
        );

        agentEventPublisher.send(emitter, "TravelPlanAgent", "running", "재계획 일정 생성 중...");
        
        travelPlanAgent.execute(context);
        
        agentEventPublisher.send(emitter, "TravelPlanAgent", "complete", "재계획 일정 생성 완료");

        
        
        agentEventPublisher.send(
                emitter,
                "ModelTrace-TRAVEL_Budget",
                "info",
                "replan=" + travelBudgetAgent.describeModel()
        );
        
        agentEventPublisher.send(emitter, "TravelBudgetAgent", "running", "재계획 예산 분석 중...");
        
        travelBudgetAgent.execute(context);
        
        agentEventPublisher.send(
                emitter,
                "TravelBudgetAgent",
                "complete",
                context.getBudgetAnalysis() != null ? context.getBudgetAnalysis().getMessage() : "재계획 예산 분석 완료"
        );
    }
}