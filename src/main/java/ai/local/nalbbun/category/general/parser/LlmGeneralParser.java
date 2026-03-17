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

/**
 * LlmGeneralParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: llm general parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class LlmGeneralParser implements CategoryParsingStrategy<GeneralContext> {

    /** chatClient 값을 보관한다. */
    private final ChatClient chatClient;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param chatClientBuilder chatClientBuilder 값
     */
    public LlmGeneralParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return GeneralContext 타입의 처리 결과
     */
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

    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String mode() {
        return "LLM";
    }
}
