package ai.local.nalbbun.category.travel;

import ai.local.nalbbun.category.travel.agent.TravelBudgetAgent;
import ai.local.nalbbun.category.travel.agent.TravelPlanAgent;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.category.travel.workflow.TravelInfoCollector;
import ai.local.nalbbun.category.travel.workflow.TravelReplanner;
import ai.local.nalbbun.category.model.ConversationState;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Travel Workflow 타입이다.
 *
 * <p>기능 설명: 다단계 처리 순서를 오케스트레이션한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
@RequiredArgsConstructor
public class TravelWorkflow {

    private final TravelInfoCollector travelInfoCollector;
    private final TravelPlanAgent travelPlanAgent;
    private final TravelBudgetAgent travelBudgetAgent;
    private final TravelReplanner travelReplanner;
    private final AgentEventPublisher agentEventPublisher;

    /**
     * execute 로직을 실행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String execute(ConversationState state, TravelContext context, SseEmitter emitter) {
        agentEventPublisher.send(
                emitter,
                "TravelRequestParser",
                "complete",
                String.format(
                        "mode=%s | 목적지=%s, 일수=%d, 예산=%,d원",
                        context.getParserMode(),
                        context.getDestination(),
                        context.getDays(),
                        context.getMaxBudget()
                )
        );

        travelInfoCollector.collect(context, emitter);

        agentEventPublisher.send(
                emitter,
                "ModelTrace-TRAVEL_PLAN",
                "info",
                "plan=" + travelPlanAgent.describeModel()
        );

        agentEventPublisher.send(emitter, "TravelPlanAgent", "running", "초기 일정 생성 중...");
        travelPlanAgent.execute(context);
        agentEventPublisher.send(emitter, "TravelPlanAgent", "complete", "초기 일정 생성 완료");

        agentEventPublisher.send(emitter, "TravelBudgetAgent", "running", "예산 분석 중...");
        travelBudgetAgent.execute(context);
        agentEventPublisher.send(
                emitter,
                "TravelBudgetAgent",
                "complete",
                context.getBudgetAnalysis() != null ? context.getBudgetAnalysis().getMessage() : "예산 분석 완료"
        );

        if (context.getBudgetAnalysis() != null && context.getBudgetAnalysis().isExceeded()) {
            travelReplanner.replan(context, emitter);
        }

        return buildSummary(context);
    }

    /**
     * build Summary 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String buildSummary(TravelContext context) {
        int totalCost = context.getPlan() != null && context.getPlan().getTotalCost() != null
                ? context.getPlan().getTotalCost()
                : 0;

        return String.format(
                """
                [TRAVEL]
                parser: %s
                목적지: %s
                일정: %d일
                예산: %,d원
                총 비용: %,d원
                상태: %s
                """,
                context.getParserMode(),
                context.getDestination(),
                context.getDays(),
                context.getMaxBudget(),
                totalCost,
                context.getBudgetAnalysis() != null && context.getBudgetAnalysis().isExceeded()
                        ? "예산 초과"
                        : "예산 적합"
        ).trim();
    }
}