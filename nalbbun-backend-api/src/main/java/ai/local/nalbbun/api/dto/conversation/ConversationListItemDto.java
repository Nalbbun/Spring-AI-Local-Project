package ai.local.nalbbun.api.dto.conversation;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationListItemDto(
        String conversationId,
        List<String> categories,
        LocalDateTime lastUpdated,
        int messageCount
) {}
