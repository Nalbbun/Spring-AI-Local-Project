package ai.local.nalbbun.service.llm;

/**
 * RuntimeModelResolutionException는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: runtime model resolution exception 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public class RuntimeModelResolutionException extends RuntimeException {

    /**
     * 필수 의존성을 주입하여 객체를 생성한다.
     *
     * @param message 사용자 입력 또는 질의 내용
     */
    public RuntimeModelResolutionException(String message) {
        super(message);
    }
}
