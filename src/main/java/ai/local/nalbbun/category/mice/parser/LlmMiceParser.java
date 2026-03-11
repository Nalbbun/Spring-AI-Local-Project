package ai.local.nalbbun.category.mice.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmMiceParser implements CategoryParsingStrategy<MiceContext> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmMiceParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public MiceContext parse(ConversationState state, MiceContext context) {
        String prompt = String.format("""
            다음 MICE/행사 관련 사용자 질문을 구조화하세요.

            질문:
            "%s"

            JSON 형식:
            {
              "eventType": "forum|conference|exhibition|festival|mice-event",
              "deliverableType": "proposal|operations|program|branding|strategy",
              "targetRegion": "korea|thailand|vietnam|malaysia|bhutan|global"
            }

            규칙:
            1) JSON만 반환
            2) 불확실하면 null 허용
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            String json = cleanJson(raw);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("eventType") && !node.get("eventType").isNull()) {
                context.setEventType(node.get("eventType").asText());
            }
            if (node.has("deliverableType") && !node.get("deliverableType").isNull()) {
                context.setDeliverableType(node.get("deliverableType").asText());
            }
            if (node.has("targetRegion") && !node.get("targetRegion").isNull()) {
                context.setTargetRegion(node.get("targetRegion").asText());
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
        if (raw == null) return "{}";
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("```\\s*$", "");
        }
        return text.trim();
    }
}