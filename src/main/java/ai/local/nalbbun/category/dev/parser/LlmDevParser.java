package ai.local.nalbbun.category.dev.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.common.ConversationState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmDevParser implements CategoryParsingStrategy<DevContext> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmDevParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public DevContext parse(ConversationState state, DevContext context) {
        String prompt = String.format("""
            다음 개발 관련 사용자 질문을 구조화하세요.

            질문:
            "%s"

            JSON 형식:
            {
              "taskType": "refactoring|troubleshooting|setup|implementation",
              "topic": "핵심 주제",
              "stackKeywords": ["spring","java","docker"]
            }

            규칙:
            1) JSON만 반환
            2) 모르면 null 또는 빈 배열
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            String json = cleanJson(raw);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("taskType") && !node.get("taskType").isNull()) {
                context.setTaskType(node.get("taskType").asText());
            }
            if (node.has("topic") && !node.get("topic").isNull()) {
                context.setTopic(node.get("topic").asText());
            }
            if (node.has("stackKeywords") && node.get("stackKeywords").isArray()) {
                context.getStackKeywords().clear();
                node.get("stackKeywords").forEach(n -> context.getStackKeywords().add(n.asText()));
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