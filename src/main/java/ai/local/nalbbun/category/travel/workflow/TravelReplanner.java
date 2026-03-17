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
 * Travel Replanner 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class TravelReplanner {

    private final TravelPlanAgent travelPlanAgent;
    private final TravelBudgetAgent travelBudgetAgent;
    private final AgentEventPublisher agentEventPublisher;

    /**
     * replan 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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