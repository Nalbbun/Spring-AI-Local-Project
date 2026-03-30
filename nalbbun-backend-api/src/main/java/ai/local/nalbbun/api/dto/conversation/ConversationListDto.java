package ai.local.nalbbun.api.dto.conversation;

import java.util.List;

public record ConversationListDto(
        List<String> conversationIds,
        int total,
        List<ConversationListItemDto> conversations
) {}
