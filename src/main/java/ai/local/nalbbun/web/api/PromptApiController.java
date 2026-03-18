package ai.local.nalbbun.web.api;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.prompt.model.PromptEntry;
import ai.local.nalbbun.prompt.model.PromptProperties;
import ai.local.nalbbun.prompt.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 프롬프트 CRUD REST API.
 * /api/prompts/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prompt-entries")
public class PromptApiController {

    private final PromptService promptService;
    private final PromptProperties promptProperties;

    /** 저장소 타입 및 전체 목록 요약 */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<PromptEntry> all = promptService.listAll();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("store", promptProperties.getStore());
        result.put("total", all.size());
        result.put("activeCount", all.stream().filter(PromptEntry::isActive).count());
        return result;
    }

    /** 전체 목록 조회 (category 필터 선택) */
    @GetMapping
    public List<PromptEntry> list(
            @RequestParam(name = "category", required = false) ChatCategory category) {
        return category != null
                ? promptService.listByCategory(category)
                : promptService.listAll();
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public PromptEntry getOne(@PathVariable("id") String id) {
        return promptService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프롬프트를 찾을 수 없습니다: " + id));
    }

    /** 카테고리 기본 프롬프트 조회 */
    @GetMapping("/default")
    public Map<String, Object> getDefault(
            @RequestParam(name = "category", required = false) ChatCategory category) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category", category == null ? "ALL" : category.name());
        result.put("prompt", promptService.resolveSystemPrompt(null, category).orElse(null));
        return result;
    }

    /** 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromptEntry create(@RequestBody PromptEntry entry) {
        return promptService.create(entry);
    }

    /** 수정 */
    @PutMapping("/{id}")
    public PromptEntry update(@PathVariable("id") String id,
                               @RequestBody PromptEntry entry) {
        return promptService.update(id, entry);
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") String id) {
        promptService.delete(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    /** 기본 프롬프트 지정 */
    @PostMapping("/{id}/default")
    public PromptEntry setDefault(@PathVariable("id") String id) {
        return promptService.setDefault(id);
    }

    /** 초기 기본 프롬프트 시드 */
    @PostMapping("/seed")
    public Map<String, Object> seed() {
        int before = promptService.listAll().size();
        promptService.seedDefaultsIfEmpty();
        int after = promptService.listAll().size();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seeded", after - before);
        result.put("total", after);
        return result;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
