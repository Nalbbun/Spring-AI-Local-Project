package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.prompt.*;
import ai.local.nalbbun.api.mapper.PromptDtoMapper;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.model.PromptProperties;
import ai.local.nalbbun.domain.prompt.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompt-entries")
public class PromptApiController {

    private final PromptService promptService;
    private final PromptProperties promptProperties;

    @GetMapping("/summary")
    public ApiResponse<PromptSummaryDto> summary() {
        try {
            List<PromptEntry> all = promptService.listAll();
            return ApiResponse.ok(new PromptSummaryDto(
                    promptProperties.getStore(),
                    all.size(),
                    all.stream().filter(PromptEntry::isActive).count()
            ));
        } catch (Exception e) {
            log.warn("프롬프트 요약 조회 실패. 빈 요약으로 대체합니다. reason={}", e.getMessage());
            return ApiResponse.ok(new PromptSummaryDto(promptProperties.getStore(), 0, 0));
        }
    }

    @GetMapping
    public ApiResponse<List<PromptEntryDto>> list(@RequestParam(name = "category", required = false) ChatCategory category) {
        try {
            List<PromptEntry> entries = category != null ? promptService.listByCategory(category) : promptService.listAll();
            return ApiResponse.ok(entries.stream().map(PromptDtoMapper::toDto).toList(), Map.of("count", entries.size()));
        } catch (Exception e) {
            log.warn("프롬프트 목록 조회 실패. 빈 목록으로 대체합니다. category={}, reason={}", category, e.getMessage());
            return ApiResponse.ok(List.of(), Map.of("count", 0));
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptEntryDto> getOne(@PathVariable("id") String id) {
        return ApiResponse.ok(promptService.findById(id)
                .map(PromptDtoMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id)));
    }

    @GetMapping("/default")
    public ApiResponse<PromptDefaultDto> getDefault(@RequestParam(name = "category", required = false) ChatCategory category) {
        try {
            return ApiResponse.ok(new PromptDefaultDto(
                    category == null ? "ALL" : category.name(),
                    promptService.resolveSystemPrompt(null, category).orElse(null)
            ));
        } catch (Exception e) {
            log.warn("기본 프롬프트 조회 실패. null로 대체합니다. category={}, reason={}", category, e.getMessage());
            return ApiResponse.ok(new PromptDefaultDto(category == null ? "ALL" : category.name(), null));
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PromptEntryDto> create(@RequestBody PromptEntry entry) {
        return ApiResponse.ok(PromptDtoMapper.toDto(promptService.create(entry)));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptEntryDto> update(@PathVariable("id") String id, @RequestBody PromptEntry entry) {
        return ApiResponse.ok(PromptDtoMapper.toDto(promptService.update(id, entry)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("id") String id) {
        promptService.delete(id);
        return ApiResponse.ok(Map.of("id", id, "deleted", true));
    }

    @PostMapping("/{id}/default")
    public ApiResponse<PromptEntryDto> setDefault(@PathVariable("id") String id) {
        return ApiResponse.ok(PromptDtoMapper.toDto(promptService.setDefault(id)));
    }

    @PostMapping("/seed")
    public ApiResponse<PromptSeedResultDto> seed() {
        try {
            int before = promptService.listAll().size();
            promptService.seedDefaultsIfEmpty();
            int after = promptService.listAll().size();
            return ApiResponse.ok(new PromptSeedResultDto(after - before, after));
        } catch (Exception e) {
            log.warn("프롬프트 시드 실패. 0건으로 응답합니다. reason={}", e.getMessage());
            return ApiResponse.ok(new PromptSeedResultDto(0, 0));
        }
    }
}
