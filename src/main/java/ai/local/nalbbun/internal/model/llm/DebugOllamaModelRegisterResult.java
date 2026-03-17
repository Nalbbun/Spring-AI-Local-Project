package ai.local.nalbbun.internal.model.llm;

import lombok.Data;

/**
 * Debug Ollama Model Register Result 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 baseUrl, model, keepAlive, requestedPull, requestedWarmup, installAttempted, installSuccess, warmupAttempted 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
@Data
public class DebugOllamaModelRegisterResult {

    private String baseUrl;
    private String model;
    private String keepAlive;

    private boolean requestedPull;
    private boolean requestedWarmup;

    private boolean installAttempted;
    private boolean installSuccess;
    private boolean warmupAttempted;
    private boolean warmupSuccess;

    private Integer installedCount;
    private Integer runningCount;

    private String status;
    private String message;
}
