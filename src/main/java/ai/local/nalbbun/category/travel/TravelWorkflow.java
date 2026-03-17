package ai.local.nalbbun.category.travel;

import ai.local.nalbbun.category.travel.agent.TravelBudgetAgent;
import ai.local.nalbbun.category.travel.agent.TravelPlanAgent;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.category.travel.workflow.TravelInfoCollector;
import ai.local.nalbbun.category.travel.workflow.TravelReplanner;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.support.sse.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TravelWorkflow는 순차 처리 흐름을 조합하고 실행하는 워크플로이다.
 * <p>주요 기능: travel workflow 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
@RequiredArgsConstructor
public class TravelWorkflow {

    /** travelInfoCollector 값을 보관한다. */
    private final TravelInfoCollector travelInfoCollector;
    /** travelPlanAgent 값을 보관한다. */
    private final TravelPlanAgent travelPlanAgent;
    /** travelBudgetAgent 값을 보관한다. */
    private final TravelBudgetAgent travelBudgetAgent;
    /** travelReplanner 값을 보관한다. */
    private final TravelReplanner travelReplanner;
    /** agentEventPublisher 값을 보관한다. */
    private final AgentEventPublisher agentEventPublisher;

    /**
     * 핵심 처리 로직을 실행한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @param emitter SSE 이벤트 전송 객체
     * @return 처리 결과 문자열
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
     * 필요한 결과 객체를 구성한다.
     *
     * @param context 처리에 필요한 컨텍스트 정보
     * @return 처리 결과 문자열
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