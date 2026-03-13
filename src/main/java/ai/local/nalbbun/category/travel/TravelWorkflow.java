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

@Component
@RequiredArgsConstructor
public class TravelWorkflow {

    private final TravelInfoCollector travelInfoCollector;
    private final TravelPlanAgent travelPlanAgent;
    private final TravelBudgetAgent travelBudgetAgent;
    private final TravelReplanner travelReplanner;
    private final AgentEventPublisher agentEventPublisher;

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