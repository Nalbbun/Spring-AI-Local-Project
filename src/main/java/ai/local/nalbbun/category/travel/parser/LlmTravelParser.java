package ai.local.nalbbun.category.travel.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.common.ConversationState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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

        String prompt = String.format("""
            다음 사용자 질문에서 여행 정보를 추출하여 JSON 형식으로 반환하세요.

            사용자 질문: "%s"

            추출할 정보:
            - destination: 여행지명
            - days: 여행 전체 일수
            - maxBudget: 총 예산 (숫자만, 원 단위)

            규칙:
            1) JSON만 반환하세요.
            2) destination을 임의로 특정 지역으로 고정하지 마세요.
            3) 값이 불명확하면 null로 두세요.
            4) "20만원"은 200000으로 변환하세요.
            5) "2박3일"은 days=3 으로 변환하세요.

            예시:
            {"destination":"부산","days":3,"maxBudget":800000}
            """, userQuery);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = cleanJson(raw);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("destination") && !node.get("destination").isNull()) {
                String destination = node.get("destination").asText();
                if (!destination.isBlank()) {
                    context.setDestination(destination);
                }
            }

            if (node.has("days") && !node.get("days").isNull()) {
                int days = node.get("days").asInt();
                if (days > 0) {
                    context.setDays(days);
                }
            }

            if (node.has("maxBudget") && !node.get("maxBudget").isNull()) {
                int budget = node.get("maxBudget").asInt();
                if (budget > 0) {
                    context.setMaxBudget(budget);
                }
            }

        } catch (Exception ignored) {
        }

        return context;
    }

    @Override
    public String mode() {
        return "LLM";
    }

    private String cleanJson(String raw) {
        if (raw == null) {
            return "{}";
        }

        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("```\\s*$", "");
        }
        return text.trim();
    }
}