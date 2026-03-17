package ai.local.nalbbun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * NalbbunAiLocalApplication는 애플리케이션 기능을 구성하는 타입이다.
 * <p>주요 기능: nalbbun ai local application 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class NalbbunAiLocalApplication {

    /**
     * main 기능을 수행한다.
     *
     * @param args args 값
     */
    public static void main(String[] args) {
        SpringApplication.run(NalbbunAiLocalApplication.class, args);
    }
}
