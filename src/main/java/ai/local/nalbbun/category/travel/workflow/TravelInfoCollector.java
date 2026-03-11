package ai.local.nalbbun.category.travel.workflow;

import ai.local.nalbbun.category.travel.agent.TravelAccommodationAgent;
import ai.local.nalbbun.category.travel.agent.TravelAttractionAgent;
import ai.local.nalbbun.category.travel.agent.TravelRestaurantAgent;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class TravelInfoCollector {

    private final TravelAttractionAgent travelAttractionAgent;
    private final TravelRestaurantAgent travelRestaurantAgent;
    private final TravelAccommodationAgent travelAccommodationAgent;
    private final AgentEventPublisher agentEventPublisher;

    public void collect(TravelContext context, SseEmitter emitter) {
        agentEventPublisher.send(
                emitter,
                "ModelTrace-TRAVEL_SEARCH",
                "info",
                "search-agents=" + travelAttractionAgent.describeModel()
        );

        CompletableFuture<Void> attractionTask = CompletableFuture.runAsync(() -> {
            try {
                agentEventPublisher.send(emitter, "TravelAttractionAgent", "running", "관광지 검색 중...");
                travelAttractionAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelAttractionAgent", "complete", "관광지 검색 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelAttractionAgent", "error", "관광지 검색 실패: " + e.getMessage());
            }
        });

        CompletableFuture<Void> restaurantTask = CompletableFuture.runAsync(() -> {
            try {
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "running", "맛집 검색 중...");
                travelRestaurantAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "complete", "맛집 검색 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "error", "맛집 검색 실패: " + e.getMessage());
            }
        });

        CompletableFuture<Void> accommodationTask = CompletableFuture.runAsync(() -> {
            try {
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "running", "숙소 검색 중...");
                travelAccommodationAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "complete", "숙소 검색 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "error", "숙소 검색 실패: " + e.getMessage());
            }
        });

        CompletableFuture.allOf(attractionTask, restaurantTask, accommodationTask).join();
    }
}