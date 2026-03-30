package ai.local.nalbbun.api;

import ai.local.nalbbun.infra.security.apikey.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API 키 관리 REST 컨트롤러.
 * /api/api-keys/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /** 프로바이더 목록 (키 발급 URL 포함) */
    @GetMapping("/providers")
    public List<Map<String, Object>> providers() {
        return apiKeyService.listProviders();
    }

    /** 현재 런타임 키 상태 요약 */
    @GetMapping("/runtime-status")
    public Map<String, Object> runtimeStatus() {
        return apiKeyService.runtimeStatus();
    }

    /** 전체 목록 (마스킹) */
    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(name = "provider", required = false) String provider) {
        return provider != null && !provider.isBlank()
                ? apiKeyService.listMaskedByProvider(provider)
                : apiKeyService.listMasked();
    }

    /** 단건 조회 (마스킹) */
    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable("id") String id) {
        return apiKeyService.findMasked(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
    }

    /** 복호화 키 조회 — 뷰 버튼 전용 */
    @GetMapping("/{id}/reveal")
    public Map<String, Object> reveal(@PathVariable("id") String id) {
        String plain = apiKeyService.revealKey(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
        return Map.of("id", id, "keyValue", plain);
    }

    /** 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return apiKeyService.create(
                str(body, "provider"),
                str(body, "label"),
                str(body, "description"),
                str(body, "keyValue"),
                bool(body, "active", true)
        );
    }

    /** 수정 */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable("id") String id,
                                       @RequestBody Map<String, Object> body) {
        return apiKeyService.update(
                id,
                str(body, "provider"),
                str(body, "label"),
                str(body, "description"),
                str(body, "keyValue"),
                bool(body, "active", true)
        );
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") String id) {
        apiKeyService.delete(id);
        return Map.of("id", id, "deleted", true);
    }

    /** 활성화 (런타임 즉시 반영) */
    @PostMapping("/{id}/activate")
    public Map<String, Object> activate(@PathVariable("id") String id) {
        return apiKeyService.activate(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
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
