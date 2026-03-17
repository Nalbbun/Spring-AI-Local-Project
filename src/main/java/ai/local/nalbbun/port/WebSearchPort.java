package ai.local.nalbbun.port;

/**
 * WebSearchPort는 구현체가 따라야 할 동작 계약을 정의하는 인터페이스이다.
 * <p>주요 기능: web search port 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public interface WebSearchPort {

    /**
     * 대상 정보를 조회한다.
     *
     * @param query 사용자 입력 또는 질의 내용
     * @return 처리 결과 문자열
     */
    String search(String query);

    /**
     * fetch 기능을 수행한다.
     *
     * @param url 대상 URL
     * @return 처리 결과 문자열
     */
    String fetch(String url);

    /**
     * providerName 기능을 수행한다.
     * @return 처리 결과 문자열
     */
    default String providerName() {
        return "unknown";
    }
}
