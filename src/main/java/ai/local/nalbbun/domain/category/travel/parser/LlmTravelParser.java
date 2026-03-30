package ai.local.nalbbun.domain.category.travel.parser;

import ai.local.nalbbun.domain.category.parser.CategoryParsingStrategy;
import ai.local.nalbbun.domain.category.travel.model.TravelContext;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.runtime.service.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LLM 기반 여행 파서.
 * RuleBasedTravelParser가 추출하지 못한 값을 LLM으로 보완합니다.
 * 파싱 실패 시 context를 수정하지 않고 원본 반환.
 */
@Slf4j
@Component
public class LlmTravelParser implements CategoryParsingStrategy<TravelContext> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmTravelParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public TravelContext parse(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery();

        // 이미 모든 필드가 채워져 있으면 LLM 호출 불필요
        if (context.getDestination() != null && !context.getDestination().isBlank()
                && context.getDays() != null && context.getDays() > 0
                && context.getMaxBudget() != null && context.getMaxBudget() > 0) {
            return context;
        }

        String prompt = String.format("""
            아래 사용자 질문에서 여행 정보를 추출해 JSON으로만 반환하세요.

            사용자 질문: "%s"

            추출 규칙:
            - destination: 질문에 언급된 여행지명 (반드시 질문에 있는 지명 그대로, 임의로 변경 금지)
            - days: 여행 전체 일수 (숫자만, "2박3일" → 3)
            - maxBudget: 총 예산 원 단위 (숫자만, "20만원" → 200000)
            - 값이 불명확하면 null
            - JSON 외 텍스트, 마크다운 코드블록 금지

            예시:
            {"destination":"강원도","days":7,"maxBudget":null}
            """, userQuery);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            // destination: 기존 값이 없을 때만 LLM 값으로 채움
            if ((context.getDestination() == null || context.getDestination().isBlank())
                    && node.has("destination") && !node.get("destination").isNull()) {
                String dest = node.get("destination").asText("").trim();
                if (!dest.isBlank() && !dest.equalsIgnoreCase("null")) {
                    context.setDestination(dest);
                }
            }

            if ((context.getDays() == null || context.getDays() <= 0)
                    && node.has("days") && !node.get("days").isNull()) {
                int days = node.get("days").asInt(0);
                if (days >= 1 && days <= 30) context.setDays(days);
            }

            if ((context.getMaxBudget() == null || context.getMaxBudget() <= 0)
                    && node.has("maxBudget") && !node.get("maxBudget").isNull()) {
                int budget = node.get("maxBudget").asInt(0);
                if (budget > 0) context.setMaxBudget(budget);
            }

            log.debug("LlmTravelParser 결과: destination={}, days={}, budget={}",
                    context.getDestination(), context.getDays(), context.getMaxBudget());

        } catch (Exception e) {
            log.warn("LlmTravelParser 파싱 실패 (원문 그대로 유지): {}", e.getMessage());
        }

        return context;
    }

    @Override
    public String mode() { return "LLM"; }
}
