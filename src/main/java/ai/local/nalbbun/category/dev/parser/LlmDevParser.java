package ai.local.nalbbun.category.dev.parser;

import java.util.Set;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmDevParser implements CategoryParsingStrategy<DevContext> {

    private static final Set<String> ALLOWED_TASK_TYPES = Set.of(
            "refactoring", "troubleshooting", "setup", "implementation"
    );

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
            1) JSON 객체만 반환
            2) code block, 설명문, 마크다운 금지
            3) 모르면 null 또는 빈 배열
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            if (node.has("taskType") && !node.get("taskType").isNull()) {
                String taskType = node.get("taskType").asText("").trim().toLowerCase();
                if (ALLOWED_TASK_TYPES.contains(taskType)) {
                    context.setTaskType(taskType);
                }
            }
            if (node.has("topic") && !node.get("topic").isNull()) {
                context.setTopic(node.get("topic").asText().trim());
            }
            if (node.has("stackKeywords") && node.get("stackKeywords").isArray()) {
                context.getStackKeywords().clear();
                node.get("stackKeywords").forEach(n -> {
                    String keyword = n.asText("").trim();
                    if (!keyword.isBlank()) {
                        context.getStackKeywords().add(keyword);
                    }
                });
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
