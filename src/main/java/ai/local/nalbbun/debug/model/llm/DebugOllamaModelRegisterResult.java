package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

/**
 * DebugOllamaModelRegisterResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: debug ollama model register result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DebugOllamaModelRegisterResult {

    /** baseUrl 값을 보관한다. */
    private String baseUrl;
    /** model 값을 보관한다. */
    private String model;
    /** keepAlive 값을 보관한다. */
    private String keepAlive;

    /** requestedPull 값을 보관한다. */
    private boolean requestedPull;
    /** requestedWarmup 값을 보관한다. */
    private boolean requestedWarmup;

    /** installAttempted 값을 보관한다. */
    private boolean installAttempted;
    /** installSuccess 값을 보관한다. */
    private boolean installSuccess;
    /** warmupAttempted 값을 보관한다. */
    private boolean warmupAttempted;
    /** warmupSuccess 값을 보관한다. */
    private boolean warmupSuccess;

    /** installedCount 값을 보관한다. */
    private Integer installedCount;
    /** runningCount 값을 보관한다. */
    private Integer runningCount;

    /** status 값을 보관한다. */
    private String status;
    /** message 값을 보관한다. */
    private String message;
}
