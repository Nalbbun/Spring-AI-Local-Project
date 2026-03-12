package ai.local.nalbbun.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutionConfig {

    @Bean(name = "chatTaskExecutor")
    public Executor chatTaskExecutor(
            @Value("${app.executor.chat.core-pool-size:4}") int corePoolSize,
            @Value("${app.executor.chat.max-pool-size:8}") int maxPoolSize,
            @Value("${app.executor.chat.queue-capacity:100}") int queueCapacity
    ) {
        return build("chat-", corePoolSize, maxPoolSize, queueCapacity);
    }

    @Bean(name = "travelTaskExecutor")
    public Executor travelTaskExecutor(
            @Value("${app.executor.travel.core-pool-size:6}") int corePoolSize,
            @Value("${app.executor.travel.max-pool-size:12}") int maxPoolSize,
            @Value("${app.executor.travel.queue-capacity:100}") int queueCapacity
    ) {
        return build("travel-", corePoolSize, maxPoolSize, queueCapacity);
    }

    @Bean(name = "llmTaskExecutor")
    public Executor llmTaskExecutor(
            @Value("${app.executor.llm.core-pool-size:4}") int corePoolSize,
            @Value("${app.executor.llm.max-pool-size:8}") int maxPoolSize,
            @Value("${app.executor.llm.queue-capacity:50}") int queueCapacity
    ) {
        return build("llm-", corePoolSize, maxPoolSize, queueCapacity);
    }

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
