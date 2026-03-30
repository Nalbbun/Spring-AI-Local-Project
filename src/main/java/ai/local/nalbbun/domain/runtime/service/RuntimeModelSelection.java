package ai.local.nalbbun.domain.runtime.service;

/**
 * Runtime Model Selection 타입이다.
 *
 * <p>기능 설명: 비즈니스 규칙과 처리 흐름을 수행한다. 불변 데이터 전달 객체로 사용된다.</p>
 * <p>입력: 도메인 요청 데이터, 주입된 의존성, 설정값</p>
 * <p>출력: 처리 결과 데이터, 상태 변경, 외부 연동 결과</p>
 */
public record RuntimeModelSelection(
        boolean ollama,
        String modelName,
        boolean fallbackApplied,
        String reason
) {
    /**
     * describe 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    public String describe() {
        String provider = ollama ? "OLLAMA:" + modelName : "OPENAI:default";
        if (!fallbackApplied) {
            return provider;
        }
        return provider + " (fallback: " + reason + ")";
    }
}
