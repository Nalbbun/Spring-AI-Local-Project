package ai.local.nalbbun.category.common;

import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            1) JSON만 반환하세요.
            2) 응답 형식:
               {"category":"DEV","confidence":92,"reason":"..."}
            3) category는 반드시 GENERAL/TRAVEL/DEV/MICE 중 하나여야 합니다.
            4) confidence는 0~100 정수입니다.

            사용자 질문:
            "%s"
            """, userQuery);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String clean = cleanJson(raw);
            JsonNode node = objectMapper.readTree(clean);

            ChatCategory category = ChatCategory.valueOf(node.get("category").asText().trim().toUpperCase());
            int confidence = node.has("confidence") ? node.get("confidence").asInt(70) : 70;
            String reason = node.has("reason") ? node.get("reason").asText("") : "";

            return new CategoryResolution(category, confidence, mode(), reason);
        } catch (Exception e) {
            return new CategoryResolution(ChatCategory.GENERAL, 50, mode(), "llm classification failed");
        }
    }
 
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