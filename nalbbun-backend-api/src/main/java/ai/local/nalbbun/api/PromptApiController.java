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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompt-entries")
public class PromptApiController {

    private final PromptService promptService;
    private final PromptProperties promptProperties;

    @GetMapping("/summary")
    public ApiResponse<PromptSummaryDto> summary() {
        List<PromptEntry> all = promptService.listAll();
        return ApiResponse.ok(new PromptSummaryDto(
                promptProperties.getStore(),
                all.size(),
                all.stream().filter(PromptEntry::isActive).count()
        ));
    }

    @GetMapping
    public ApiResponse<List<PromptEntryDto>> list(@RequestParam(name = "category", required = false) ChatCategory category) {
        List<PromptEntry> entries = category != null ? promptService.listByCategory(category) : promptService.listAll();
        return ApiResponse.ok(entries.stream().map(PromptDtoMapper::toDto).toList(), Map.of("count", entries.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptEntryDto> getOne(@PathVariable("id") String id) {
        return ApiResponse.ok(promptService.findById(id)
                .map(PromptDtoMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id)));
    }

    @GetMapping("/default")
    public ApiResponse<PromptDefaultDto> getDefault(@RequestParam(name = "category", required = false) ChatCategory category) {
        return ApiResponse.ok(new PromptDefaultDto(
                category == null ? "ALL" : category.name(),
                promptService.resolveSystemPrompt(null, category).orElse(null)
        ));
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
        int before = promptService.listAll().size();
        promptService.seedDefaultsIfEmpty();
        int after = promptService.listAll().size();
        return ApiResponse.ok(new PromptSeedResultDto(after - before, after));
    }
}
