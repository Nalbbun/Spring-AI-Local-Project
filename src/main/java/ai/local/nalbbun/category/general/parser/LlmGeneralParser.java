package ai.local.nalbbun.category.general.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.general.model.GeneralContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
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
            1) JSON 객체만 반환
            2) code block, 설명문, 마크다운 금지
            3) intent는 짧게 작성
            """, state.getUserQuery());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            if (node.has("intent") && !node.get("intent").isNull()) {
                context.setIntent(node.get("intent").asText("general_qa").trim());
            }
        } catch (Exception ignored) {
        }

        if (context.getIntent() == null || context.getIntent().isBlank()) {
            context.setIntent("general_qa");
        }
        return context;
    }

    @Override
    public String mode() {
        return "LLM";
    }
}
