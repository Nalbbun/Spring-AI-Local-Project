package ai.local.nalbbun.domain.category.general.parser;

import ai.local.nalbbun.domain.category.parser.CategoryParsingStrategy;
import ai.local.nalbbun.domain.category.general.model.GeneralContext;
import ai.local.nalbbun.domain.category.model.ConversationState;
import ai.local.nalbbun.domain.runtime.service.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Llm General Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class LlmGeneralParser implements CategoryParsingStrategy<GeneralContext> {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Llm General Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public LlmGeneralParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Override
    public String mode() {
        return "LLM";
    }
}
