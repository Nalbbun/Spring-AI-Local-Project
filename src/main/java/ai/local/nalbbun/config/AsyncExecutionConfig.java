package ai.local.nalbbun.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async Execution Config 타입이다.
 *
 * <p>기능 설명: 스프링 빈과 런타임 설정을 구성한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다.</p>
 * <p>입력: 프로퍼티 값, 환경 변수, 스프링 컨텍스트 정보</p>
 * <p>출력: 빈 등록 결과 또는 런타임 설정 상태</p>
 */
@Configuration
public class AsyncExecutionConfig {

    /**
     * chat Task Executor 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Bean(name = "chatTaskExecutor")
    public Executor chatTaskExecutor(
            @Value("${app.executor.chat.core-pool-size:4}") int corePoolSize,
            @Value("${app.executor.chat.max-pool-size:8}") int maxPoolSize,
            @Value("${app.executor.chat.queue-capacity:100}") int queueCapacity
    ) {
        return build("chat-", corePoolSize, maxPoolSize, queueCapacity);
    }

    /**
     * travel Task Executor 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Bean(name = "travelTaskExecutor")
    public Executor travelTaskExecutor(
            @Value("${app.executor.travel.core-pool-size:6}") int corePoolSize,
            @Value("${app.executor.travel.max-pool-size:12}") int maxPoolSize,
            @Value("${app.executor.travel.queue-capacity:100}") int queueCapacity
    ) {
        return build("travel-", corePoolSize, maxPoolSize, queueCapacity);
    }

    /**
     * llm Task Executor 기능을 수행한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    @Bean(name = "llmTaskExecutor")
    public Executor llmTaskExecutor(
            @Value("${app.executor.llm.core-pool-size:4}") int corePoolSize,
            @Value("${app.executor.llm.max-pool-size:8}") int maxPoolSize,
            @Value("${app.executor.llm.queue-capacity:50}") int queueCapacity
    ) {
        return build("llm-", corePoolSize, maxPoolSize, queueCapacity);
    }

    /**
     * build 결과를 구성한다.
     *
     * <p>입력: 메서드 파라미터, 주입된 상태값, 내부 계산에 필요한 문맥 정보</p>
     * <p>출력: 반환값, 상태 변경 또는 후속 처리용 결과</p>
     */
    private Executor build(String prefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
