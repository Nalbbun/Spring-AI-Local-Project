package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.prompt.PromptTemplateRecordDto;
import ai.local.nalbbun.api.mapper.PromptTemplateDtoMapper;
import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptPageScope;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateUpsertRequest;
import ai.local.nalbbun.domain.prompt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompts")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public ApiResponse<List<PromptTemplateRecordDto>> list(
            @RequestParam(name = "pageScope", required = false) PromptPageScope pageScope,
            @RequestParam(name = "category", required = false) ChatCategory category,
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        var items = promptTemplateService.findAll(pageScope, category, activeOnly).stream().map(PromptTemplateDtoMapper::toDto).toList();
        return ApiResponse.ok(items, Map.of("count", items.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromptTemplateRecordDto> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(promptTemplateService.findById(id)
                .map(PromptTemplateDtoMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프롬프트 ID 입니다: " + id)));
    }

    @PostMapping
    public ApiResponse<PromptTemplateRecordDto> create(@RequestBody PromptTemplateUpsertRequest request) {
        return ApiResponse.ok(PromptTemplateDtoMapper.toDto(promptTemplateService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplateRecordDto> update(@PathVariable("id") Long id,
                                       @RequestBody PromptTemplateUpsertRequest request) {
        return ApiResponse.ok(PromptTemplateDtoMapper.toDto(promptTemplateService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("id") Long id) {
        promptTemplateService.delete(id);
        return ApiResponse.ok(Map.of("id", id, "deleted", true));
    }
}
