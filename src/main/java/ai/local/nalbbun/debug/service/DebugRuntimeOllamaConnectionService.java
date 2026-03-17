package ai.local.nalbbun.debug.service;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Debug Runtime Ollama Connection Service 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
@Service
public class DebugRuntimeOllamaConnectionService {

    private final String defaultBaseUrl;
    private final AtomicReference<String> baseUrl;

    /**
     * Debug Runtime Ollama Connection Service 인스턴스를 초기화한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public DebugRuntimeOllamaConnectionService(
            @Value("${spring.ai.ollama.base-url:http://127.0.0.1:11434}") String defaultBaseUrl
    ) {
        this.defaultBaseUrl = normalize(defaultBaseUrl);
        this.baseUrl = new AtomicReference<>(this.defaultBaseUrl);
    }

    /**
     * Base Url 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getBaseUrl() {
        return baseUrl.get();
    }

    /**
     * Default Base Url 값을 반환한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * update 작업을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
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
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String reset() {
        baseUrl.set(defaultBaseUrl);
        return defaultBaseUrl;
    }

    /**
     * normalize 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
