package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LlmCategoryResolver {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmCategoryResolver(@Qualifier("ollamaBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public CategoryResolution resolve(String userQuery) {
        String prompt = String.format("""
            다음 사용자 질문을 정확히 하나의 카테고리로 분류하세요.

            카테고리:
            - GENERAL
            - TRAVEL
            - DEV
            - MICE

            규칙:
            1) JSON 객체만 반환하세요.
            2) code block, 설명문, 마크다운 금지
            3) 응답 형식:
               {"category":"DEV","confidence":92,"reason":"..."}
            4) category는 반드시 GENERAL/TRAVEL/DEV/MICE 중 하나여야 합니다.
            5) confidence는 0~100 정수입니다.

            사용자 질문:
            "%s"
            """, userQuery);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            ChatCategory category = ChatCategory.valueOf(node.path("category").asText("GENERAL").trim().toUpperCase());
            int confidence = Math.max(0, Math.min(100, node.path("confidence").asInt(70)));
            String reason = node.path("reason").asText("");

            return new CategoryResolution(category, confidence, mode(), reason);
        } catch (Exception e) {
            return new CategoryResolution(ChatCategory.GENERAL, 50, mode(), "llm classification failed");
        }
    }

    public String mode() {
        return "LLM";
    }
}
