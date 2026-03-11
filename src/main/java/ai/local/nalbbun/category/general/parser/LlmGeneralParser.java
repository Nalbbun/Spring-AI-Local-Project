package ai.local.nalbbun.category.general.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.common.ConversationState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmGeneralParser implements CategoryParsingStrategy<GeneralContext> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmGeneralParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public GeneralContext parse(ConversationState state, GeneralContext context) {
        String prompt = String.format("""
            다음 일반 질문의 의도를 JSON으로 간단히 분류하세요.

            질문:
            "%s"

            형식:
            {"intent":"general_qa"}

            규칙:
            1) JSON만 반환
            2) intent는 짧게 작성
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            String json = cleanJson(raw);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("intent") && !node.get("intent").isNull()) {
                context.setIntent(node.get("intent").asText());
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