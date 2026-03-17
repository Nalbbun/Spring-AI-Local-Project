package ai.local.nalbbun.web.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.local.nalbbun.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.memory.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;

/**
 * 대화 관리 REST API.
 * 저장소(in-memory / JDBC / Redis) 구현에 무관하게 동작합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memory")
public class ConversationManagementController {

    private final ConversationMemoryService conversationMemoryService;

    /** 저장된 conversationId 목록 조회 */
    @GetMapping("/conversations")
    public List<String> listConversations() {
        return conversationMemoryService.listConversationIds();
    }

    /** 특정 conversationId 스냅샷 조회 */
    @GetMapping("/conversations/{conversationId}")
    public ConversationMemorySnapshot getSnapshot(@PathVariable("conversationId") String conversationId) {
        return conversationMemoryService.snapshot(conversationId);
    }

    /** 특정 conversationId 삭제 */
    @DeleteMapping("/conversations/{conversationId}")
    public Map<String, Object> deleteConversation(@PathVariable("conversationId") String conversationId) {
        conversationMemoryService.clear(conversationId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversationId", conversationId);
        result.put("deleted", true);
        return result;
    }

    /** 전체 대화 수 및 저장소 타입 요약 */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<String> ids = conversationMemoryService.listConversationIds();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("storeType", conversationMemoryService.getClass().getSimpleName());
        result.put("conversationCount", ids.size());
        result.put("conversationIds", ids);
        return result;
    }
}