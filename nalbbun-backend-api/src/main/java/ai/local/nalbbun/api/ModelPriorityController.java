package ai.local.nalbbun.api;

import ai.local.nalbbun.domain.runtime.service.CategoryModelPriorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 카테고리별 모델 우선순위 REST API.
 * /api/model-priority/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/model-priority")
public class ModelPriorityController {

    private final CategoryModelPriorityService priorityService;

    /** 전체 우선순위 조회 */
    @GetMapping
    public Map<String, Object> getAll() {
        return priorityService.getAll();
    }

    /** 일괄 변경 { "GENERAL": "OLLAMA_FIRST", "DEV": "OPENAI_ONLY", ... } */
    @PostMapping
    public Map<String, Object> updateAll(@RequestBody Map<String, String> body) {
        return priorityService.updateAll(body);
    }

    /** 전체 초기화 (OLLAMA_FIRST) */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        return priorityService.reset();
    }
}
