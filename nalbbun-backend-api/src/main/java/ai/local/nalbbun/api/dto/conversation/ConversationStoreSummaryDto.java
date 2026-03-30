package ai.local.nalbbun.api.dto.conversation;

import java.util.List;

public record ConversationStoreSummaryDto(String storeType, int conversationCount, List<String> conversationIds) {}
