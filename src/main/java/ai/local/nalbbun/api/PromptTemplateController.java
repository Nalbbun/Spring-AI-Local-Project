package ai.local.nalbbun.api;

import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptPageScope;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateRecord;
import ai.local.nalbbun.domain.prompt.model.PromptTemplateUpsertRequest;
import ai.local.nalbbun.domain.prompt.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프롬프트 템플릿 관리 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompts")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public List<PromptTemplateRecord> list(
            @RequestParam(name = "pageScope", required = false) PromptPageScope pageScope,
            @RequestParam(name = "category", required = false) ChatCategory category,
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly
    ) {
        return promptTemplateService.findAll(pageScope, category, activeOnly);
    }

    @GetMapping("/{id}")
    public PromptTemplateRecord detail(@PathVariable("id") Long id) {
        return promptTemplateService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프롬프트 ID 입니다: " + id));
    }

    @PostMapping
    public PromptTemplateRecord create(@RequestBody PromptTemplateUpsertRequest request) {
        return promptTemplateService.create(request);
    }

    @PutMapping("/{id}")
    public PromptTemplateRecord update(@PathVariable("id") Long id,
                                       @RequestBody PromptTemplateUpsertRequest request) {
        return promptTemplateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        promptTemplateService.delete(id);
    }
}
