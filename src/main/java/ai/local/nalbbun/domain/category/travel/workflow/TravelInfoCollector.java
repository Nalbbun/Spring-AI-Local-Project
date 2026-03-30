package ai.local.nalbbun.domain.category.travel.workflow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.local.nalbbun.domain.category.travel.agent.TravelAccommodationAgent;
import ai.local.nalbbun.domain.category.travel.agent.TravelAttractionAgent;
import ai.local.nalbbun.domain.category.travel.agent.TravelRestaurantAgent;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.common.sse.AgentEventPublisher;

/**
 * Travel Info Collector 타입이다.
 *
 * <p>기능 설명: 애플리케이션 기능을 이루는 재사용 가능한 구성 요소다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class TravelInfoCollector {

    private final TravelAttractionAgent travelAttractionAgent;
    private final TravelRestaurantAgent travelRestaurantAgent;
    private final TravelAccommodationAgent travelAccommodationAgent;
    private final AgentEventPublisher agentEventPublisher;
    private final Executor travelTaskExecutor;

    /**
     * Travel Info Collector 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
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
