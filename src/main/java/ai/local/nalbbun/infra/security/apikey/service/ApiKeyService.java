package ai.local.nalbbun.infra.security.apikey.service;

import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyProvider;
import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API 키 CRUD 서비스.
 * 저장 시 AES 암호화, 조회 시 마스킹, 뷰 요청 시만 복호화합니다.
 * 활성 키 변경 시 OpenAI / Tavily 런타임 설정에 즉시 반영합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final ApiKeyCrypto     crypto;

    // OpenAI API 런타임 반영 (Spring AI OpenAiApi 빈)
    private final OpenAiApi openAiApi;

    // ── 조회 ──────────────────────────────────────────────
    /** 전체 목록 — keyValue 는 마스킹 */
    public List<Map<String, Object>> listMasked() {
        return repository.findAll().stream()
                .map(this::toMasked)
                .toList();
    }

    /** provider 별 목록 — keyValue 마스킹 */
    public List<Map<String, Object>> listMaskedByProvider(String provider) {
        return repository.findByProvider(provider).stream()
                .map(this::toMasked)
                .toList();
    }

    /** 단건 조회 — keyValue 마스킹 */
    public Optional<Map<String, Object>> findMasked(String id) {
        return repository.findById(id).map(this::toMasked);
    }

    /** 단건 복호화 조회 — 뷰 전용 */
    public Optional<String> revealKey(String id) {
        return repository.findById(id)
                .map(e -> crypto.decrypt(e.getKeyValue()));
    }

    /** 프로바이더 목록 */
    public List<Map<String, Object>> listProviders() {
        return List.of(
            providerInfo(ApiKeyProvider.OPENAI),
            providerInfo(ApiKeyProvider.TAVILY),
            providerInfo(ApiKeyProvider.ANTHROPIC),
            providerInfo(ApiKeyProvider.CUSTOM)
        );
    }

    // ── CUD ───────────────────────────────────────────────
    public Map<String, Object> create(String provider, String label,
                                       String description, String plainKey, boolean active) {
        validate(provider, label, plainKey);
        ApiKeyEntry entry = ApiKeyEntry.builder()
                .provider(provider.toUpperCase())
                .label(label)
                .description(description)
                .keyValue(crypto.encrypt(plainKey))
                .active(active)
                .build();
        ApiKeyEntry saved = repository.save(entry);
        if (active) applyToRuntime(saved.getProvider(), plainKey);
        return toMasked(saved);
    }

    public Map<String, Object> update(String id, String provider, String label,
                                       String description, String plainKey, boolean active) {
        ApiKeyEntry existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
        existing.setProvider(provider != null ? provider.toUpperCase() : existing.getProvider());
        existing.setLabel(label != null ? label : existing.getLabel());
        existing.setDescription(description);
        existing.setActive(active);
        if (plainKey != null && !plainKey.isBlank()) {
            existing.setKeyValue(crypto.encrypt(plainKey));
        }
        ApiKeyEntry updated = repository.update(existing);
        if (active) {
            String key = (plainKey != null && !plainKey.isBlank())
                    ? plainKey : crypto.decrypt(existing.getKeyValue());
            applyToRuntime(updated.getProvider(), key);
        }
        return toMasked(updated);
    }

    public void delete(String id) {
        repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
        repository.delete(id);
    }

    /** 특정 키를 런타임에 적용 (활성화 토글) */
    public Map<String, Object> activate(String id) {
        ApiKeyEntry entry = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API 키를 찾을 수 없습니다: " + id));
        entry.setActive(true);
        repository.update(entry);
        applyToRuntime(entry.getProvider(), crypto.decrypt(entry.getKeyValue()));
        return toMasked(entry);
    }

    /** 현재 런타임에 적용된 키 요약 */
    public Map<String, Object> runtimeStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (ApiKeyProvider p : ApiKeyProvider.values()) {
            Optional<ApiKeyEntry> active = repository.findActiveByProvider(p.name());
            result.put(p.name().toLowerCase(),
                active.map(e -> "활성 (" + e.maskedKey() + ")").orElse("미설정"));
        }
        return result;
    }

    // ── 런타임 반영 ───────────────────────────────────────
    private void applyToRuntime(String provider, String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return;
        try {
            switch (provider.toUpperCase()) {
                case "OPENAI" -> {
                    // Spring AI OpenAiApi — 리플렉션으로 apiKey 필드 변경
                    var field = openAiApi.getClass().getDeclaredField("apiKey");
                    field.setAccessible(true);
                    field.set(openAiApi, plainKey);
                    log.info("OpenAI API 키 런타임 적용 완료");
                }
                case "TAVILY" -> {
                    // TavilyWebSearchService는 @Value로 주입 — 환경변수로 재설정 안내
                    System.setProperty("TAVILY_API_KEY", plainKey);
                    log.info("Tavily API 키 시스템 프로퍼티 설정 완료 (재시작 시 반영)");
                }
                default -> log.info("런타임 반영 미지원 프로바이더: {}", provider);
            }
        } catch (Exception e) {
            log.warn("API 키 런타임 반영 실패 (provider={}, reason={})", provider, e.getMessage());
        }
    }

    // ── 내부 유틸 ─────────────────────────────────────────
    private Map<String, Object> toMasked(ApiKeyEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          e.getId());
        m.put("provider",    e.getProvider());
        m.put("label",       e.getLabel());
        m.put("description", e.getDescription());
        m.put("maskedKey",   e.maskedKey());
        m.put("active",      e.isActive());
        m.put("createdAt",   e.getCreatedAt());
        m.put("updatedAt",   e.getUpdatedAt());
        return m;
    }

    private Map<String, Object> providerInfo(ApiKeyProvider p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider",     p.name());
        m.put("displayName",  p.displayName);
        m.put("description",  p.description);
        m.put("keyIssueUrl",  p.keyIssueUrl);
        Optional<ApiKeyEntry> active = repository.findActiveByProvider(p.name());
        m.put("hasActiveKey", active.isPresent());
        m.put("maskedKey",    active.map(ApiKeyEntry::maskedKey).orElse(null));
        return m;
    }

    private void validate(String provider, String label, String plainKey) {
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("프로바이더는 필수입니다.");
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("레이블은 필수입니다.");
        if (plainKey == null || plainKey.isBlank())
            throw new IllegalArgumentException("API 키 값은 필수입니다.");
    }
}
