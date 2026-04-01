package ai.local.nalbbun.api.dto.runtime;

import java.util.List;

public record RuntimeMetaDto(
        boolean debugEnabled,
        boolean adminConsoleEnabled,
        boolean crossOriginSessionSupported,
        String conversationTransport,
        List<String> activeProfiles
) {}
