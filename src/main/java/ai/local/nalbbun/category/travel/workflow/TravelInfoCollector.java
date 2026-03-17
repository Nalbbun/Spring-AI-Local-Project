package ai.local.nalbbun.category.travel.workflow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.category.travel.agent.TravelAccommodationAgent;
import ai.local.nalbbun.category.travel.agent.TravelAttractionAgent;
import ai.local.nalbbun.category.travel.agent.TravelRestaurantAgent;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.support.sse.AgentEventPublisher;

/**
 * TravelInfoCollector는 순차 처리 흐름을 조합하고 실행하는 워크플로이다.
 * <p>주요 기능: travel info collector 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class TravelInfoCollector {

    /** travelAttractionAgent 값을 보관한다. */
    private final TravelAttractionAgent travelAttractionAgent;
    /** travelRestaurantAgent 값을 보관한다. */
    private final TravelRestaurantAgent travelRestaurantAgent;
    /** travelAccommodationAgent 값을 보관한다. */
    private final TravelAccommodationAgent travelAccommodationAgent;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;
    /** travelTaskExecutor 값을 보관한다. */
    private final Executor travelTaskExecutor;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param travelAttractionAgent travelAttractionAgent 값
     * @param travelRestaurantAgent travelRestaurantAgent 값
     * @param travelAccommodationAgent travelAccommodationAgent 값
     * @param agentEventPublisher agentEventPublisher 값
     * @param travelTaskExecutor travelTaskExecutor 값
     */
    public TravelInfoCollector(
            TravelAttractionAgent travelAttractionAgent,
            TravelRestaurantAgent travelRestaurantAgent,
            TravelAccommodationAgent travelAccommodationAgent,
            AgentEventPublisher agentEventPublisher,
            @Qualifier("travelTaskExecutor") Executor travelTaskExecutor
    ) {
        this.travelAttractionAgent = travelAttractionAgent;
        this.travelRestaurantAgent = travelRestaurantAgent;
        this.travelAccommodationAgent = travelAccommodationAgent;
        this.agentEventPublisher = agentEventPublisher;
        this.travelTaskExecutor = travelTaskExecutor;
    }

    /**
     * collect 기능을 수행한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @param emitter SSE 이벤트 전송 객체
     */
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
        }, travelTaskExecutor);

        CompletableFuture<Void> restaurantTask = CompletableFuture.runAsync(() -> {
            try {
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "running", "맛집 검색 중...");
                travelRestaurantAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "complete", "맛집 검색 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelRestaurantAgent", "error", "맛집 검색 실패: " + e.getMessage());
            }
        }, travelTaskExecutor);

        CompletableFuture<Void> accommodationTask = CompletableFuture.runAsync(() -> {
            try {
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "running", "숙소 검색 중...");
                travelAccommodationAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "complete", "숙소 검색 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelAccommodationAgent", "error", "숙소 검색 실패: " + e.getMessage());
            }
        }, travelTaskExecutor);

        CompletableFuture.allOf(attractionTask, restaurantTask, accommodationTask).join();
    }
}
