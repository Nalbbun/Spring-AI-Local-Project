package ai.local.nalbbun.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AsyncExecutionConfig는 애플리케이션 설정과 빈 구성을 담당하는 설정 타입이다.
 * <p>주요 기능: async execution config 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Configuration
public class AsyncExecutionConfig {

    /**
     * chatTaskExecutor 기능을 수행한다.
     *
     * @param corePoolSize corePoolSize 값
     * @param maxPoolSize maxPoolSize 값
     * @param queueCapacity queueCapacity 값
     * @return Executor 타입의 처리 결과
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
     * travelTaskExecutor 기능을 수행한다.
     *
     * @param corePoolSize corePoolSize 값
     * @param maxPoolSize maxPoolSize 값
     * @param queueCapacity queueCapacity 값
     * @return Executor 타입의 처리 결과
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
     * llmTaskExecutor 기능을 수행한다.
     *
     * @param corePoolSize corePoolSize 값
     * @param maxPoolSize maxPoolSize 값
     * @param queueCapacity queueCapacity 값
     * @return Executor 타입의 처리 결과
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
     * 필요한 결과 객체를 구성한다.
     *
     * @param prefix prefix 값
     * @param corePoolSize corePoolSize 값
     * @param maxPoolSize maxPoolSize 값
     * @param queueCapacity queueCapacity 값
     * @return Executor 타입의 처리 결과
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
