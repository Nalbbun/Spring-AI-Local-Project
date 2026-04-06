package ai.local.nalbbun.domain.category.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Conversation State 타입이다.
 *
 * <p>기능 설명: 계층 간에 전달되는 도메인 데이터와 상태를 표현한다. 클래스 단위 책임이 명확하도록 관련 기능을 응집해 제공한다. 주요 속성 예시는 conversationId, userQuery, requestedCategory, resolvedCategory, requestedExecutionMode, resolvedExecutionMode, categoryContext, finalResponse, errorMessage 이다.</p>
 * <p>입력: 상위 계층에서 전달한 속성 값</p>
 * <p>출력: 직렬화/역직렬화 가능한 데이터 객체</p>
 */
@Data
public class ConversationState {

    private String conversationId;
    private String userQuery;

    /** 채팅 요청 시 전달된 프롬프트 ID (null이면 기본 프롬프트 사용) */
    private String promptId;

    private ChatCategory requestedCategory;
    private ChatCategory resolvedCategory;
    private ExecutionMode requestedExecutionMode;
    private ExecutionMode resolvedExecutionMode;

    private CategoryContext categoryContext;

    private String finalResponse;
    private String errorMessage;

    private Map<String, Object> attributes = new HashMap<>();
}
