package ai.local.nalbbun.category.travel.parser;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.travel.model.TravelContext;
import ai.local.nalbbun.model.common.ConversationState;
import ai.local.nalbbun.service.llm.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * LlmTravelParser는 입력 데이터를 해석하여 구조화된 결과로 변환하는 파서이다.
 * <p>주요 기능: llm travel parser 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class LlmTravelParser implements CategoryParsingStrategy<TravelContext> {

    /** chatClient 값을 보관한다. */
    private final ChatClient chatClient;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param chatClientBuilder chatClientBuilder 값
     */
    public LlmTravelParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 입력 데이터를 파싱하여 구조화한다.
     *
     * @param state 현재 처리 상태 정보
     * @param context 처리에 필요한 컨텍스트 정보
     * @return TravelContext 타입의 처리 결과
     */
    @Override
    public TravelContext parse(ConversationState state, TravelContext context) {
        String userQuery = state.getUserQuery();

        String prompt = String.format("""
            다음 사용자 질문에서 여행 정보를 추출하여 JSON 객체 형식으로 반환하세요.

            사용자 질문: "%s"

            추출할 정보:
            - destination: 여행지명
            - days: 여행 전체 일수
            - maxBudget: 총 예산 (숫자만, 원 단위)

            규칙:
            1) JSON 객체만 반환하세요.
            2) code block, 설명문, 마크다운 금지
            3) destination을 임의로 특정 지역으로 고정하지 마세요.
            4) 값이 불명확하면 null로 두세요.
            5) "20만원"은 200000으로 변환하세요.
            6) "2박3일"은 days=3 으로 변환하세요.

            예시:
            {"destination":"부산","days":3,"maxBudget":800000}
            """, userQuery);

        try {
            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            JsonNode node = objectMapper.readTree(LlmJsonSupport.extractObject(raw));

            if (node.has("destination") && !node.get("destination").isNull()) {
                String destination = node.get("destination").asText("").trim();
                if (!destination.isBlank()) {
                    context.setDestination(destination);
                }
            }

            if (node.has("days") && !node.get("days").isNull()) {
                int days = node.get("days").asInt();
                if (days > 0 && days <= 30) {
                    context.setDays(days);
                }
            }

            if (node.has("maxBudget") && !node.get("maxBudget").isNull()) {
                int budget = node.get("maxBudget").asInt();
                if (budget > 0) {
                    context.setMaxBudget(budget);
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
