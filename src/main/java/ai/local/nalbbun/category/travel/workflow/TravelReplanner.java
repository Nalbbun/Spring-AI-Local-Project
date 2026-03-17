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

/**
 * TravelReplanner는 순차 처리 흐름을 조합하고 실행하는 워크플로이다.
 * <p>주요 기능: travel replanner 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class TravelReplanner {

    /** travelPlanAgent 값을 보관한다. */
    private final TravelPlanAgent travelPlanAgent;
    /** travelBudgetAgent 값을 보관한다. */
    private final TravelBudgetAgent travelBudgetAgent;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;

    /**
     * replan 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param emitter SSE 이벤트 전송 객체
     */
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