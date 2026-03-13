package ai.local.nalbbun.model.common;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ConversationState {

    private String conversationId;
    private String userQuery;

    private ChatCategory requestedCategory;
    private ChatCategory resolvedCategory;

    private CategoryContext categoryContext;

    private String finalResponse;
    private String errorMessage;

    private Map<String, Object> attributes = new HashMap<>();
}