package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.dto.runtime.ModelPriorityDto;
import ai.local.nalbbun.domain.runtime.service.CategoryModelPriorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/model-priority")
public class ModelPriorityController {

    private final CategoryModelPriorityService priorityService;

    @GetMapping
    public ApiResponse<ModelPriorityDto> getAll() {
        return ApiResponse.ok(new ModelPriorityDto(priorityService.getAll()));
    }

    @PostMapping
    public ApiResponse<ModelPriorityDto> updateAll(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(new ModelPriorityDto(priorityService.updateAll(body)));
    }

    @PostMapping("/reset")
    public ApiResponse<ModelPriorityDto> reset() {
        return ApiResponse.ok(new ModelPriorityDto(priorityService.reset()));
    }
}
