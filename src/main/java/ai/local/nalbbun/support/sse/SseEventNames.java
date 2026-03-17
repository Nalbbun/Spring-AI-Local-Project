package ai.local.nalbbun.support.sse;

/**
 * SseEventNames는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: sse event names 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public final class SseEventNames {

    /** AGENT 값을 보관한다. */
    public static final String AGENT = "agent";
    /** MESSAGE 값을 보관한다. */
    public static final String MESSAGE = "message";
    /** COMPLETE 값을 보관한다. */
    public static final String COMPLETE = "complete";
    /** ERROR 값을 보관한다. */
    public static final String ERROR = "error";

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     */
    private SseEventNames() {
    }
}