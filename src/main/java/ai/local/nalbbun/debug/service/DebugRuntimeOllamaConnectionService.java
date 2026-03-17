package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * DebugRuntimeOllamaConnectionService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: debug runtime ollama connection service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Service
public class DebugRuntimeOllamaConnectionService {

    /** defaultBaseUrl 값을 보관한다. */
    private final String defaultBaseUrl;
    /** baseUrl 값을 보관한다. */
    private final AtomicReference<String> baseUrl;

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param defaultBaseUrl defaultBaseUrl 값
     */
    public DebugRuntimeOllamaConnectionService(
            @Value("${spring.ai.ollama.base-url:http://127.0.0.1:11434}") String defaultBaseUrl
    ) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.baseUrl = new AtomicReference<>(this.defaultBaseUrl);
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getBaseUrl() {
        return baseUrl.get();
    }

    /**
     * 지정된 정보를 조회한다.
     * @return 처리 결과 문자열
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * 대상 값을 갱신한다.
     *
     * @param requestedBaseUrl requestedBaseUrl 값
     * @return 처리 결과 문자열
     */
    public String update(String requestedBaseUrl) {
        String normalized = normalize(requestedBaseUrl);
        if (normalized.isBlank()) {
            normalized = defaultBaseUrl;
        }
        baseUrl.set(normalized);
        return normalized;
    }

    /**
     * reset 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String reset() {
        baseUrl.set(defaultBaseUrl);
        return defaultBaseUrl;
    }

    /**
     * normalize 기능을 수행한다.
     *
     * @param value value 값
     * @return 처리 결과 문자열
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
