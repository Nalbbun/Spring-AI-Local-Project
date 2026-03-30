package ai.local.nalbbun.domain.search.port;

/**
 * Web Search Port 인터페이스이다.
 *
 * <p>기능 설명: 외부 연동 계약을 정의한다. 구현체가 따라야 할 계약을 정의한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
public interface WebSearchPort {

    String search(String query);

    String fetch(String url);

    /**
     * provider Name 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    default String providerName() {
        return "unknown";
    }
}
