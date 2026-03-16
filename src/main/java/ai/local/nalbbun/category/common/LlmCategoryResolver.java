package ai.local.nalbbun.category.common;

import ai.local.nalbbun.debug.service.DebugRuntimeOllamaConnectionService;
import ai.local.nalbbun.model.category.CategoryResolution;
import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmCategoryResolver {

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final String categoryModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmCategoryResolver(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            @Value("${app.ollama.default-general-model:${spring.ai.ollama.chat.options.model:gemma2:9b}}") String categoryModel
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.categoryModel = categoryModel;
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
            String raw = runtimeChatClient().prompt()
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

    private ChatClient runtimeChatClient() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(categoryModel)
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    public String mode() {
        return "LLM";
    }
}
