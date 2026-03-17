package ai.local.nalbbun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * Nalbbun Ai Local Application 타입이다.
 *
 * <p>기능 설명: 애플리케이션 부트스트랩과 실행 진입점을 제공한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 호출 계층에서 전달되는 입력값과 주입된 의존성</p>
 * <p>출력: 처리 결과 객체, 상태 변경 또는 후속 처리에 필요한 데이터</p>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class NalbbunAiLocalApplication {

    /**
     * main 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 상태 변경, 이벤트 전송 또는 내부 처리 완료 상태</p>
     */
    public static void main(String[] args) {
        SpringApplication.run(NalbbunAiLocalApplication.class, args);
    }
}
