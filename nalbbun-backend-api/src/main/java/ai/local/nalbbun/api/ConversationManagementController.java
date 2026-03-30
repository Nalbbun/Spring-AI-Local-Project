package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.conversation.*;
import ai.local.nalbbun.api.mapper.ConversationDtoMapper;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memory")
public class ConversationManagementController {

    private final ConversationMemoryService conversationMemoryService;

    @GetMapping("/conversations")
    public ApiResponse<ConversationListDto> listConversations() {
        List<String> ids = conversationMemoryService.listConversationIds();
        return ApiResponse.ok(new ConversationListDto(ids, ids.size()));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationSnapshotDto> getSnapshot(@PathVariable("conversationId") String conversationId) {
        ConversationMemorySnapshot snapshot = conversationMemoryService.snapshot(conversationId);
        return ApiResponse.ok(ConversationDtoMapper.toDto(snapshot));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationDeleteResultDto> deleteConversation(@PathVariable("conversationId") String conversationId) {
        conversationMemoryService.clear(conversationId);
        return ApiResponse.ok(new ConversationDeleteResultDto(conversationId, true));
    }

    @GetMapping("/summary")
    public ApiResponse<ConversationStoreSummaryDto> summary() {
        List<String> ids = conversationMemoryService.listConversationIds();
        return ApiResponse.ok(new ConversationStoreSummaryDto(
                conversationMemoryService.getClass().getSimpleName(),
                ids.size(),
                ids
        ));
    }
}
