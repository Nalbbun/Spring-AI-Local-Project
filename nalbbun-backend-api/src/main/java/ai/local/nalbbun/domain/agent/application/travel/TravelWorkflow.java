package ai.local.nalbbun.domain.agent.application.travel;

import ai.local.nalbbun.domain.agent.executor.travel.TravelBudgetAgent;
import ai.local.nalbbun.domain.agent.executor.travel.TravelPlanAgent;
import ai.local.nalbbun.domain.category.travel.model.DaySchedule;
import ai.local.nalbbun.domain.category.travel.model.Plan;
import ai.local.nalbbun.domain.category.travel.model.ScheduleItem;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.agent.application.travel.TravelInfoCollector;
import ai.local.nalbbun.domain.agent.application.travel.TravelReplanner;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.common.sse.AgentEventPublisher;
import ai.local.nalbbun.common.sse.SseEmitterHelper;
import ai.local.nalbbun.common.sse.SseEventNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Travel Workflow — 에이전트 파이프라인 오케스트레이션.
 *
 * 수정 이력:
 * - buildSummary() 단순 텍스트 → TOKEN SSE 스트리밍으로 교체
 *   이유: TravelPlanAgent.execute()는 callEntity()로 JSON Plan 반환 후
 *         TOKEN 이벤트를 한 번도 전송하지 않아 채팅 화면에 아무것도 표시되지 않았음.
 * - 해결: renderPlanAsMarkdown()으로 Plan을 마크다운으로 포맷팅 후
 *         streamTokens()로 줄 단위 TOKEN 이벤트 전송
 */
@Component
@RequiredArgsConstructor
public class TravelWorkflow {

    private final TravelInfoCollector travelInfoCollector;
    private final TravelPlanAgent     travelPlanAgent;
    private final TravelBudgetAgent   travelBudgetAgent;
    private final TravelReplanner     travelReplanner;
    private final AgentEventPublisher agentEventPublisher;
    private final SseEmitterHelper    sseEmitterHelper;

    public String execute(ConversationState state, TravelContext context, SseEmitter emitter) {

        // 1. 요청 파싱 결과 로그
        agentEventPublisher.send(emitter, "TravelRequestParser", "complete",
            String.format("mode=%s | 목적지=%s, 일수=%d, 예산=%,d원",
                context.getParserMode(), context.getDestination(),
                context.getDays(), context.getMaxBudget()));

        // 2. 정보 수집 (관광지, 맛집, 숙소 — 병렬)
        travelInfoCollector.collect(context, emitter);

        // 3. 일정 생성 (LLM callEntity)
        boolean planGenerated = false;
        agentEventPublisher.send(emitter, "ModelTrace-TRAVEL_PLAN", "info",
            "plan=" + travelPlanAgent.describeModel());
        agentEventPublisher.send(emitter, "TravelPlanAgent", "running", "초기 일정 생성 중...");
        try {
            travelPlanAgent.execute(context);
            planGenerated = context.getPlan() != null;
            agentEventPublisher.send(emitter, "TravelPlanAgent", planGenerated ? "complete" : "warn",
                planGenerated ? "초기 일정 생성 완료" : "일정 구조 생성값이 비어 있어 기본 추천 형식으로 응답합니다.");
        } catch (Exception e) {
            agentEventPublisher.send(emitter, "TravelPlanAgent", "error",
                "일정 생성 실패로 기본 추천 형식으로 전환합니다: " + safeMessage(e));
        }

        // 4. 예산 분석
        if (context.getPlan() != null) {
            try {
                agentEventPublisher.send(emitter, "TravelBudgetAgent", "running", "예산 분석 중...");
                travelBudgetAgent.execute(context);
                agentEventPublisher.send(emitter, "TravelBudgetAgent", "complete",
                    context.getBudgetAnalysis() != null
                        ? context.getBudgetAnalysis().getMessage() : "예산 분석 완료");
            } catch (Exception e) {
                agentEventPublisher.send(emitter, "TravelBudgetAgent", "warn",
                    "예산 분석을 건너뜁니다: " + safeMessage(e));
            }
        } else {
            agentEventPublisher.send(emitter, "TravelBudgetAgent", "info", "일정 데이터가 없어 예산 분석을 건너뜁니다.");
        }

        // 5. 예산 초과 시 재계획
        if (context.getPlan() != null && context.getBudgetAnalysis() != null && context.getBudgetAnalysis().isExceeded()) {
            travelReplanner.replan(context, emitter);
        }

        // 6. 최종 응답 렌더링 → TOKEN SSE 스트리밍
        String fullResponse = renderPlanAsMarkdown(context);
        streamTokens(emitter, fullResponse);

        return fullResponse;
    }

    // ── 마크다운 렌더링 ────────────────────────────────────────────
    /**
     * Plan 객체를 마크다운 형태로 렌더링합니다.
     * LLM 재호출 없이 로컬에서 직접 포맷팅합니다.
     */
    private String renderPlanAsMarkdown(TravelContext context) {
        StringBuilder sb = new StringBuilder();
        Plan plan = context.getPlan();
        int totalCost = (plan != null && plan.getTotalCost() != null) ? plan.getTotalCost() : 0;
        boolean exceeded = context.getBudgetAnalysis() != null
                && context.getBudgetAnalysis().isExceeded();

        // 헤더
        sb.append("## 🗺 ").append(safe(context.getDestination()))
          .append(" ").append(context.getDays()).append("일 여행 가이드\n\n");

        // 여행 요약 표
        sb.append("### 📋 여행 요약\n");
        sb.append("| 항목 | 내용 |\n|------|------|\n");
        sb.append("| 📍 목적지 | ").append(safe(context.getDestination())).append(" |\n");
        sb.append("| 📅 기간 | ").append(context.getDays()).append("일 (")
          .append(Math.max(0, context.getDays() - 1)).append("박) |\n");
        sb.append("| 💰 총 예산 | ").append(String.format("%,d원", context.getMaxBudget())).append(" |\n");
        sb.append("| 💳 예상 총 비용 | **").append(String.format("%,d원", totalCost)).append("**");
        if (exceeded) sb.append(" ⚠️ (예산 초과)");
        else if (totalCost > 0) sb.append(" ✅ (예산 내)");
        sb.append(" |\n\n");

        // 일정 데이터 없을 때
        if (plan == null || plan.getDays() == null || plan.getDays().isEmpty()) {
            sb.append("> ⚠️ 자동 일정 생성 모델 응답이 불안정하여 기본 추천 형식으로 안내합니다.\n\n");
            sb.append("### 추천 관광지\n");
            if (context.getAttractions() != null && !context.getAttractions().isEmpty()) {
                context.getAttractions().stream().limit(5).forEach(item ->
                    sb.append("- ").append(safe(item.getName())).append(" · ").append(safe(item.getAddress())).append("\n"));
            } else {
                sb.append("- 추천 관광지 정보를 불러오지 못했습니다.\n");
            }
            sb.append("\n### 추천 맛집\n");
            if (context.getRestaurants() != null && !context.getRestaurants().isEmpty()) {
                context.getRestaurants().stream().limit(5).forEach(item ->
                    sb.append("- ").append(safe(item.getName())).append(" · ").append(safe(item.getAddress())).append("\n"));
            } else {
                sb.append("- 추천 맛집 정보를 불러오지 못했습니다.\n");
            }
            sb.append("\n### 추천 숙소\n");
            if (context.getAccommodations() != null && !context.getAccommodations().isEmpty()) {
                context.getAccommodations().stream().limit(3).forEach(item ->
                    sb.append("- ").append(safe(item.getName())).append(" · ").append(safe(item.getAddress())).append("\n"));
            } else {
                sb.append("- 추천 숙소 정보를 불러오지 못했습니다.\n");
            }
            sb.append("\n### 기본 일정 가이드\n");
            for (int day = 1; day <= Math.max(1, context.getDays()); day++) {
                sb.append("- Day ").append(day).append(": 오전 관광 / 점심 / 오후 관광 / 저녁 / 휴식\n");
            }
            return sb.toString().trim();
        }

        // 일별 일정
        for (DaySchedule day : plan.getDays()) {
            sb.append("---\n### 📅 Day ").append(day.getDayNumber()).append("\n\n");

            if (day.getSchedule() == null || day.getSchedule().isEmpty()) {
                sb.append("_일정 정보 없음_\n\n");
                continue;
            }

            for (ScheduleItem item : day.getSchedule()) {
                String icon = switch (safe(item.getType()).toLowerCase()) {
                    case "attraction"    -> "🏛";
                    case "meal"          -> "🍽";
                    case "accommodation" -> "🏨";
                    default              -> "📌";
                };
                sb.append("**").append(icon).append(" ")
                  .append(safe(item.getTime())).append(" — ")
                  .append(safe(item.getName())).append("**\n");
                if (!safe(item.getAddress()).isBlank())
                    sb.append("📍 ").append(item.getAddress()).append("\n");
                if (!safe(item.getDescription()).isBlank())
                    sb.append("　").append(item.getDescription()).append("\n");
                if (item.getCost() > 0)
                    sb.append("　💰 ").append(String.format("%,d원", item.getCost())).append("\n");
                sb.append("\n");
            }
        }

        // 비용 내역 표
        if (plan.getMeals() != null || plan.getAccommodation() != null || plan.getAttractions() != null) {
            sb.append("---\n### 💳 비용 내역\n\n");
            sb.append("| 분류 | 비용 |\n|------|------|\n");
            if (plan.getAttractions() != null)
                sb.append("| 🏛 관광지 입장료 | ").append(String.format("%,d원", plan.getAttractions())).append(" |\n");
            if (plan.getMeals() != null)
                sb.append("| 🍽 식사비 | ").append(String.format("%,d원", plan.getMeals())).append(" |\n");
            if (plan.getAccommodation() != null)
                sb.append("| 🏨 숙박비 | ").append(String.format("%,d원", plan.getAccommodation())).append(" |\n");
            sb.append("| **합계** | **").append(String.format("%,d원", totalCost)).append("** |\n");
            int remain = context.getMaxBudget() - totalCost;
            sb.append("| 예산 잔액 | ").append(String.format("%,d원", remain))
              .append(exceeded ? " ⚠️ 초과" : " ✅").append(" |\n\n");
        }

        // 예산 초과 경고
        if (exceeded && context.getBudgetAnalysis() != null) {
            sb.append("> ⚠️ **").append(context.getBudgetAnalysis().getMessage()).append("**\n");
            sb.append("> 예산 범위에 맞게 일부 항목을 조정하는 것을 권장합니다.\n\n");
        }

        return sb.toString().trim();
    }

    /**
     * 완성된 문자열을 줄 단위로 잘라 TOKEN SSE 이벤트로 전송합니다.
     * 스트리밍 효과를 위해 줄 단위로 분리합니다.
     */
    private void streamTokens(SseEmitter emitter, String text) {
        if (text == null || text.isBlank()) return;
        // 줄바꿈 기준으로 split (줄바꿈 문자 포함해서 분리)
        String[] lines = text.split("(?<=\\n)");
        for (String line : lines) {
            sseEmitterHelper.send(emitter, SseEventNames.TOKEN, line);
        }
        // 마지막 줄이 줄바꿈으로 끝나지 않는 경우 처리
        if (!text.endsWith("\n")) {
            // 이미 전송됨
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "unknown";
        }
        return e.getMessage();
    }
}
