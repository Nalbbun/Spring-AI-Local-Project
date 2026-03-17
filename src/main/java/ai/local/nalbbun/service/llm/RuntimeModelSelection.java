package ai.local.nalbbun.service.llm;

/**
 * RuntimeModelSelection는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: runtime model selection 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 * @param ollama ollama 값
 * @param modelName modelName 값
 * @param fallbackApplied fallbackApplied 값
 * @param reason reason 값
 */
public record RuntimeModelSelection(
        boolean ollama,
        String modelName,
        boolean fallbackApplied,
        String reason
) {
    /**
     * describe 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    public String describe() {
        String provider = ollama ? "OLLAMA:" + modelName : "OPENAI:default";
        if (!fallbackApplied) {
            return provider;
        }
        return provider + " (fallback: " + reason + ")";
    }
}
