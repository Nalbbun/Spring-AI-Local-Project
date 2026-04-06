package ai.local.nalbbun.domain.prompt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PromptSeedInitializer {

    private final PromptService promptService;

    @Bean
    ApplicationRunner promptSeedApplicationRunner() {
        return args -> {
            try {
                promptService.seedDefaultsIfEmpty();
            } catch (Exception e) {
                log.warn("기본 프롬프트 초기 시드 실패. reason={}", e.getMessage());
            }
        };
    }
}
