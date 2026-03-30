package ai.local.nalbbun.api.dto.runtime;

public record OllamaConnectionInfoDto(
        String baseUrl,
        boolean reachable,
        String status,
        String message,
        int runningCount,
        int installedCount
) {}
