package ai.local.nalbbun.api.dto.apikey;

public record ApiKeyProviderDto(
        String provider,
        String displayName,
        String description,
        String keyIssueUrl,
        boolean hasActiveKey,
        String maskedKey
) {}
