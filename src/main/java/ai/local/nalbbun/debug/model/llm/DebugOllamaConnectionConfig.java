package ai.local.nalbbun.debug.model.llm;

import lombok.Data;

/**
 * DebugOllamaConnectionConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: debug ollama connection config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class DebugOllamaConnectionConfig {

    /** baseUrl 값을 보관한다. */
    private String baseUrl;
}
