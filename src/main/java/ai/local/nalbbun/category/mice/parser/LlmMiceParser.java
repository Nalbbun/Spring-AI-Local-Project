package ai.local.nalbbun.category.mice.parser;

import java.util.Set;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.mice.model.MiceContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LlmMiceParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: llm mice parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class LlmMiceParser implements CategoryParsingStrategy<MiceContext> {

    /** ALLOWED_EVENT_TYPES 값을 보관한다. */
    private static final Set<String> ALLOWED_EVENT_TYPES = Set.of("forum", "conference", "exhibition", "festival", "mice-event");
    /** ALLOWED_DELIVERABLE_TYPES 값을 보관한다. */
    private static final Set<String> ALLOWED_DELIVERABLE_TYPES = Set.of("proposal", "operations", "program", "branding", "strategy");

    /** chatClient 값을 보관한다. */
    private final ChatClient chatClient;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param chatClientBuilder chatClientBuilder 값
     */
    public LlmMiceParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return MiceContext 타입의 처리 결과
     */
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

    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    @Override
    public String mode() {
        return "LLM";
    }
}
