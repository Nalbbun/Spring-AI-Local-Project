package ai.local.nalbbun.debug.model.llm;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Debug Ollama Connection Info 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 baseUrl, reachable, status, message, runningCount, installedCount 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
@Data
public class DebugOllamaConnectionInfo {
    private String baseUrl;
    private boolean reachable;
    private String status;
    private String message;
    private int runningCount;
    private int installedCount;
    private List<String> runningModels = new ArrayList<>();
}
