package ai.local.nalbbun.api;

import ai.local.nalbbun.api.dto.apikey.*;
import ai.local.nalbbun.api.dto.common.ApiResponse;
import ai.local.nalbbun.api.mapper.ApiKeyDtoMapper;
import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping("/providers")
    public ApiResponse<List<ApiKeyProviderDto>> providers() {
        return ApiResponse.ok(apiKeyService.listProviders().stream().map(ApiKeyDtoMapper::toProviderDto).toList());
    }

    @GetMapping("/runtime-status")
    public ApiResponse<ApiKeyRuntimeStatusDto> runtimeStatus() {
        return ApiResponse.ok(new ApiKeyRuntimeStatusDto(apiKeyService.runtimeStatus()));
    }

    @GetMapping
    public ApiResponse<List<ApiKeyEntryDto>> list(@RequestParam(name = "provider", required = false) String provider) {
        List<Map<String, Object>> rows = provider != null && !provider.isBlank()
                ? apiKeyService.listMaskedByProvider(provider)
                : apiKeyService.listMasked();
        return ApiResponse.ok(rows.stream().map(ApiKeyDtoMapper::toEntryDto).toList(), Map.of("count", rows.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiKeyEntryDto> getOne(@PathVariable("id") String id) {
        return ApiResponse.ok(apiKeyService.findMasked(id).map(ApiKeyDtoMapper::toEntryDto)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id)));
    }

    @GetMapping("/{id}/reveal")
    public ApiResponse<ApiKeyRevealDto> reveal(@PathVariable("id") String id) {
        String plain = apiKeyService.revealKey(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
        return ApiResponse.ok(new ApiKeyRevealDto(id, plain));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApiKeyEntryDto> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(ApiKeyDtoMapper.toEntryDto(apiKeyService.create(
                str(body, "provider"),
                str(body, "label"),
                str(body, "description"),
                str(body, "keyValue"),
                bool(body, "active", true)
        )));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiKeyEntryDto> update(@PathVariable("id") String id,
                                       @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(ApiKeyDtoMapper.toEntryDto(apiKeyService.update(
                id,
                str(body, "provider"),
                str(body, "label"),
                str(body, "description"),
                str(body, "keyValue"),
                bool(body, "active", true)
        )));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable("id") String id) {
        apiKeyService.delete(id);
        return ApiResponse.ok(Map.of("id", id, "deleted", true));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<ApiKeyEntryDto> activate(@PathVariable("id") String id) {
        return ApiResponse.ok(ApiKeyDtoMapper.toEntryDto(apiKeyService.activate(id)));
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString().trim();
    }
    private boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v == null ? def : Boolean.parseBoolean(v.toString());
    }
}
