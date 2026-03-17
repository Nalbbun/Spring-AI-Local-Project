package ai.local.nalbbun.service.memory;

import ai.local.nalbbun.model.category.ChatCategory;
import ai.local.nalbbun.model.common.ConversationMemorySnapshot;
import ai.local.nalbbun.model.common.MemoryMessage;

import java.util.List;

/**
 * ConversationMemoryService는 도메인 로직과 운영 지원 기능을 수행하는 서비스이다.
 * <p>주요 기능: conversation memory service 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
public interface ConversationMemoryService {

    /**
     * addUserMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    void addUserMessage(String conversationId, ChatCategory category, String content);

    /**
     * addAssistantMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    void addAssistantMessage(String conversationId, ChatCategory category, String content);

    /**
     * addSystemMessage 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param content 본문 또는 텍스트 내용
     */
    void addSystemMessage(String conversationId, ChatCategory category, String content);

    /**
     * recentMessages 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param limit limit 값
     * @return 조회 또는 생성된 목록
     */
    List<MemoryMessage> recentMessages(String conversationId, int limit);

    /**
     * formatRecentConversation 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param limit limit 값
     * @return 처리 결과 문자열
     */
    String formatRecentConversation(String conversationId, int limit);

    /**
     * 대상 값을 갱신한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param summary summary 값
     */
    void updateCategorySummary(String conversationId, ChatCategory category, String summary);

    /**
     * 지정된 정보를 조회한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @return 처리 결과 문자열
     */
    String getCategorySummary(String conversationId, ChatCategory category);

    /**
     * addImportantNote 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @param note note 값
     */
    void addImportantNote(String conversationId, ChatCategory category, String note);

    /**
     * 지정된 정보를 조회한다.
     *
     * @param conversationId 대화 식별자
     * @param category 대상 카테고리 정보
     * @return 조회 또는 생성된 목록
     */
    List<String> getImportantNotes(String conversationId, ChatCategory category);

    /**
     * snapshot 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     * @return ConversationMemorySnapshot 타입의 처리 결과
     */
    ConversationMemorySnapshot snapshot(String conversationId);

    /**
     * clear 기능을 수행한다.
     *
     * @param conversationId 대화 식별자
     */
    void clear(String conversationId);
}