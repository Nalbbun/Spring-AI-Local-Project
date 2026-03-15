package ai.local.nalbbun.category.mice.parser;

import java.util.Set;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmMiceParser implements CategoryParsingStrategy<MiceContext> {

    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of("forum", "conference", "exhibition", "festival", "mice-event");
    private static final Set<String> ALLOWED_DELIVERABLE_TYPES = Set.of("proposal", "operations", "program", "branding", "strategy");

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
            1) JSON 객체만 반환
            2) code block, 설명문, 마크다운 금지
            3) 불확실하면 null 허용
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            if (node.has("eventType") && !node.get("eventType").isNull()) {
                String value = node.get("eventType").asText("").trim().toLowerCase();
                if (ALLOWED_EVENT_TYPES.contains(value)) {
                    context.setEventType(value);
                }
            }
            if (node.has("deliverableType") && !node.get("deliverableType").isNull()) {
                String value = node.get("deliverableType").asText("").trim().toLowerCase();
                if (ALLOWED_DELIVERABLE_TYPES.contains(value)) {
                    context.setDeliverableType(value);
                }
            }
            if (node.has("targetRegion") && !node.get("targetRegion").isNull()) {
                String value = node.get("targetRegion").asText("").trim().toLowerCase();
                if (!value.isBlank()) {
                    context.setTargetRegion(value);
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
}
