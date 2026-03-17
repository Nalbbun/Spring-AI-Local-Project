package ai.local.nalbbun.category.dev.parser;

import java.util.Set;

import ai.local.nalbbun.category.common.parser.CategoryParsingStrategy;
import ai.local.nalbbun.category.dev.model.DevContext;
import ai.local.nalbbun.category.model.ConversationState;
import ai.local.nalbbun.llm.service.LlmJsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Llm Dev Parser 타입이다.
 *
 * <p>기능 설명: 원시 입력을 구조화된 데이터로 변환한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class LlmDevParser implements CategoryParsingStrategy<DevContext> {

    private static final Set<String> ALLOWED_TASK_TYPES = Set.of(
            "refactoring", "troubleshooting", "setup", "implementation"
    );

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Llm Dev Parser 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public LlmDevParser(@Qualifier("openaiBuilder") ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * parse 처리를 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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
