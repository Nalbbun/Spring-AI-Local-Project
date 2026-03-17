package ai.local.nalbbun.model.common;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ConversationState는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: conversation state 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
public class ConversationState {

    /** conversationId 값을 보관한다. */
    private String conversationId;
    /** userQuery 값을 보관한다. */
    private String userQuery;

    /** requestedCategory 값을 보관한다. */
    private ChatCategory requestedCategory;
    /** resolvedCategory 값을 보관한다. */
    private ChatCategory resolvedCategory;

    /** categoryContext 값을 보관한다. */
    private CategoryContext categoryContext;

    /** finalResponse 값을 보관한다. */
    private String finalResponse;
    /** errorMessage 값을 보관한다. */
    private String errorMessage;

    /** attributes 값을 보관한다. */
    private Map<String, Object> attributes = new HashMap<>();
}