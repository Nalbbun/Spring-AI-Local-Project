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
 * LlmCategoryResolver는 조건에 따라 적절한 대상이나 값을 해석하는 리졸버이다.
 * <p>주요 기능: llm category resolver 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Component
public class LlmCategoryResolver {

    /** ollamaConnectionService 값을 보관한다. */
    private final DebugRuntimeOllamaConnectionService ollamaConnectionService;
    /** categoryModel 값을 보관한다. */
    private final String categoryModel;
    /** chatKeepAlive 값을 보관한다. */
    private final String chatKeepAlive;
    /** ollamaConnectTimeoutMs 값을 보관한다. */
    private final long ollamaConnectTimeoutMs;
    /** ollamaRequestTimeoutMs 값을 보관한다. */
    private final long ollamaRequestTimeoutMs;
    /** objectMapper 값을 보관한다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param ollamaConnectionService ollamaConnectionService 값
     * @param categoryModel categoryModel 값
     * @param chatKeepAlive chatKeepAlive 값
     * @param ollamaConnectTimeoutMs ollamaConnectTimeoutMs 값
     * @param ollamaRequestTimeoutMs ollamaRequestTimeoutMs 값
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
     * 입력 정보를 해석하여 결과를 결정한다.
     *
     * @param userQuery 사용자 입력 또는 질의 내용
     * @return CategoryResolution 타입의 처리 결과
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
     * 핵심 처리 로직을 실행한다.
     * @return ChatClient 타입의 처리 결과
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
     * 핵심 처리 로직을 실행한다.
     * @return RestClient.Builder 타입의 처리 결과
     */
    private RestClient.Builder runtimeRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) ollamaConnectTimeoutMs);
        requestFactory.setReadTimeout((int) ollamaRequestTimeoutMs);
        return RestClient.builder().requestFactory(requestFactory);
    }

    /**
     * 핵심 처리 로직을 실행한다.
     * @return WebClient.Builder 타입의 처리 결과
     */
    private WebClient.Builder runtimeWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(ollamaRequestTimeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    /**
     * mode 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String mode() {
        return "LLM";
    }
}
