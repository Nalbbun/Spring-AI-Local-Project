package ai.local.nalbbun.category.common;

import java.time.Duration;

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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * Llm Category Resolver 타입이다.
 *
 * <p>기능 설명: 입력 조건을 해석해 적절한 선택 결과를 도출한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@Component
public class LlmCategoryResolver {

    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    private final String categoryModel;
    private final String chatKeepAlive;
    private final long ollamaConnectTimeoutMs;
    private final long ollamaRequestTimeoutMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Llm Category Resolver 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public LlmCategoryResolver(
            DebugRuntimeOllamaConnectionService ollamaConnectionService,
            @Value("${app.ollama.default-general-model:${spring.ai.ollama.chat.options.model:gemma2:9b}}") String categoryModel,
            @Value("${spring.ai.ollama.chat.options.keep-alive:300s}") String chatKeepAlive,
            @Value("${app.ollama.connect-timeout-ms:5000}") long ollamaConnectTimeoutMs,
            @Value("${app.ollama.request-timeout-ms:300000}") long ollamaRequestTimeoutMs
    ) {
        this.ollamaConnectionService = ollamaConnectionService;
        this.categoryModel = categoryModel;
        this.chatKeepAlive = (chatKeepAlive == null || chatKeepAlive.isBlank()) ? "300s" : chatKeepAlive.trim();
        this.ollamaConnectTimeoutMs = Math.max(1000, ollamaConnectTimeoutMs);
        this.ollamaRequestTimeoutMs = Math.max(this.ollamaConnectTimeoutMs, ollamaRequestTimeoutMs);
    }

    /**
     * resolve 결과를 계산한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
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

    /**
     * runtime Chat Client 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private ChatClient runtimeChatClient() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaConnectionService.getBaseUrl())
                .restClientBuilder(runtimeRestClientBuilder())
                .webClientBuilder(runtimeWebClientBuilder())
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(categoryModel)
                        .keepAlive(chatKeepAlive)
                        .build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    /**
     * runtime Rest Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) ollamaConnectTimeoutMs);
        requestFactory.setReadTimeout((int) ollamaRequestTimeoutMs);
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * runtime Web Client Builder 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private WebClient.Builder runtimeWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * mode 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String mode() {
        return "LLM";
    }
}
