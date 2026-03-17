package ai.local.nalbbun.debug.model.llm;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * DebugOllamaWarmupResult는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: debug ollama warmup result 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DebugOllamaWarmupResult {
    /** baseUrl 값을 보관한다. */
    private String baseUrl;
    /** keepAlive 값을 보관한다. */
    private String keepAlive;
    /** applied 값을 보관한다. */
    private boolean applied;
    /** message 값을 보관한다. */
    private String message;
    /** requestedModels 값을 보관한다. */
    private List<String> requestedModels = new ArrayList<>();
    /** warmedModels 값을 보관한다. */
    private List<String> warmedModels = new ArrayList<>();
    /** failedModels 값을 보관한다. */
    private List<String> failedModels = new ArrayList<>();
}
