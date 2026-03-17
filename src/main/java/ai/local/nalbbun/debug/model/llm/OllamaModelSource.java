package ai.local.nalbbun.debug.model.llm;

/**
 * OllamaModelSource는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: ollama model source 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public enum OllamaModelSource {
    RUNNING,
    INSTALLED,
    ALL
}
