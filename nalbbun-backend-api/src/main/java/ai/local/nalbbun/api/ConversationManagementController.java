package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.conversation.*;
import ai.local.nalbbun.api.mapper.ConversationDtoMapper;
import ai.local.nalbbun.domain.memory.model.ConversationMemorySnapshot;
import ai.local.nalbbun.domain.memory.service.ConversationMemoryService;
import ai.local.nalbbun.domain.memory.service.RoutingConversationMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memory")
public class ConversationManagementController {

    private final ConversationMemoryService conversationMemoryService;

    @GetMapping("/conversations")
    public ApiResponse<ConversationListDto> listConversations() {
        try {
            List<String> ids = conversationMemoryService.listConversationIds();
            List<ConversationListItemDto> conversations = ids.stream()
                    .map(conversationMemoryService::snapshot)
                    .map(ConversationDtoMapper::toListItemDto)
                    .sorted(Comparator
                            .comparing(ConversationListItemDto::lastUpdated,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(ConversationListItemDto::conversationId))
                    .toList();
            List<String> orderedIds = conversations.stream()
                    .map(ConversationListItemDto::conversationId)
                    .toList();
            return ApiResponse.ok(new ConversationListDto(orderedIds, orderedIds.size(), conversations));
        } catch (Exception e) {
            log.warn("대화 목록 조회 실패. 빈 목록으로 대체합니다. reason={}", e.getMessage());
            return ApiResponse.ok(new ConversationListDto(List.of(), 0, List.of()));
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationSnapshotDto> getSnapshot(@PathVariable("conversationId") String conversationId) {
        try {
            ConversationMemorySnapshot snapshot = conversationMemoryService.snapshot(conversationId);
            return ApiResponse.ok(ConversationDtoMapper.toDto(snapshot));
        } catch (Exception e) {
            log.warn("대화 스냅샷 조회 실패. 빈 스냅샷으로 대체합니다. conversationId={}, reason={}", conversationId, e.getMessage());
            return ApiResponse.ok(ConversationDtoMapper.toDto(new ConversationMemorySnapshot(conversationId, List.of(), java.util.Map.of(), List.of())));
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ApiResponse<ConversationDeleteResultDto> deleteConversation(@PathVariable("conversationId") String conversationId) {
        conversationMemoryService.clear(conversationId);
        return ApiResponse.ok(new ConversationDeleteResultDto(conversationId, true));
    }

    @GetMapping("/summary")
    public ApiResponse<ConversationStoreSummaryDto> summary() {
        try {
            List<String> ids = conversationMemoryService.listConversationIds();
            return ApiResponse.ok(new ConversationStoreSummaryDto(
                    storeType(),
                    ids.size(),
                    ids
            ));
        } catch (Exception e) {
            log.warn("대화 저장소 요약 조회 실패. 빈 요약으로 대체합니다. reason={}", e.getMessage());
            return ApiResponse.ok(new ConversationStoreSummaryDto(storeType(), 0, List.of()));
        }
    }

    private String storeType() {
        if (conversationMemoryService instanceof RoutingConversationMemoryService routingConversationMemoryService) {
            return routingConversationMemoryService.getActiveStore() + " (" + routingConversationMemoryService.getActiveServiceType() + ")";
        }
        return conversationMemoryService.getClass().getSimpleName();
    }
}
