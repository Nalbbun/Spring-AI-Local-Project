package ai.local.nalbbun.infra.security.apikey.service;

import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyProvider;
import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * API 키 CRUD 서비스.
 * 저장 시 AES 암호화, 조회 시 마스킹, 뷰 요청 시만 복호화합니다.
 * 활성 키 변경 시 OpenAI / Tavily 런타임 설정에 즉시 반영합니다.
 *
 * 기본 provider 목록은 안내용으로 유지하되, 사용자가 임의의 provider 이름을 여러 개 등록할 수 있도록
 * 저장/조회/필터링은 모두 문자열 기반으로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository repository;
    private final ApiKeyCrypto crypto;

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
        return repository.findByProvider(normalizeProvider(provider)).stream()
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

    /**
     * provider 목록.
     * - 기본 provider 는 항상 노출
     * - DB에 저장된 사용자 정의 provider 도 추가 노출
     */
    public List<Map<String, Object>> listProviders() {
        Set<String> providerNames = new LinkedHashSet<>(defaultProviderCatalog().keySet());
        repository.findAll().stream()
                .map(ApiKeyEntry::getProvider)
                .filter(v -> v != null && !v.isBlank())
                .map(this::normalizeProvider)
                .forEach(providerNames::add);

        return providerNames.stream()
                .map(this::providerInfo)
                .toList();
    }

    // ── CUD ───────────────────────────────────────────────
    public Map<String, Object> create(String provider, String label,
                                      String description, String plainKey, boolean active) {
        validate(provider, label, plainKey);
        String normalizedProvider = normalizeProvider(provider);
        ApiKeyEntry entry = ApiKeyEntry.builder()
                .provider(normalizedProvider)
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
        existing.setProvider(provider != null ? normalizeProvider(provider) : existing.getProvider());
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

    public Optional<String> findActivePlainKey(String provider) {
        if (provider == null || provider.isBlank()) {
            return Optional.empty();
        }
        return repository.findActiveByProvider(normalizeProvider(provider))
                .map(e -> crypto.decrypt(e.getKeyValue()));
    }

    public boolean hasActiveKey(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        return repository.findActiveByProvider(normalizeProvider(provider)).isPresent();
    }

    /** 현재 런타임에 적용된 키 요약 */
    public Map<String, Object> runtimeStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String provider : listProviders().stream().map(v -> String.valueOf(v.get("provider"))).toList()) {
            Optional<ApiKeyEntry> active = repository.findActiveByProvider(provider);
            result.put(provider.toLowerCase(),
                    active.map(e -> "활성 (" + e.maskedKey() + ")").orElse("미설정"));
        }
        return result;
    }

    // ── 런타임 반영 ───────────────────────────────────────
    private void applyToRuntime(String provider, String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return;
        String normalizedProvider = normalizeProvider(provider);
        try {
            switch (normalizedProvider) {
                case "OPENAI" -> {
                    var field = openAiApi.getClass().getDeclaredField("apiKey");
                    field.setAccessible(true);
                    field.set(openAiApi, plainKey);
                    log.info("OpenAI API 키 런타임 적용 완료");
                }
                case "TAVILY" -> {
                    System.setProperty("TAVILY_API_KEY", plainKey);
                    log.info("Tavily API 키 시스템 프로퍼티 설정 완료 (재시작 시 반영)");
                }
                default -> log.info("런타임 반영 미지원 프로바이더: {}", normalizedProvider);
            }
        } catch (Exception e) {
            log.warn("API 키 런타임 반영 실패 (provider={}, reason={})", normalizedProvider, e.getMessage());
        }
    }

    // ── 내부 유틸 ─────────────────────────────────────────
    private Map<String, Object> toMasked(ApiKeyEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("provider", e.getProvider());
        m.put("label", e.getLabel());
        m.put("description", e.getDescription());
        m.put("maskedKey", e.maskedKey());
        m.put("active", e.isActive());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }

    private Map<String, Object> providerInfo(String providerName) {
        ProviderMeta meta = defaultProviderCatalog().get(normalizeProvider(providerName));
        String normalized = normalizeProvider(providerName);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", normalized);
        m.put("displayName", meta != null ? meta.displayName() : normalized);
        m.put("description", meta != null ? meta.description() : "사용자 정의 API 키 그룹");
        m.put("keyIssueUrl", meta != null ? meta.keyIssueUrl() : null);
        Optional<ApiKeyEntry> active = repository.findActiveByProvider(normalized);
        m.put("hasActiveKey", active.isPresent());
        m.put("maskedKey", active.map(ApiKeyEntry::maskedKey).orElse(null));
        return m;
    }

    private Map<String, ProviderMeta> defaultProviderCatalog() {
        Map<String, ProviderMeta> catalog = new LinkedHashMap<>();
        for (ApiKeyProvider provider : ApiKeyProvider.values()) {
            catalog.put(provider.name(), new ProviderMeta(provider.displayName, provider.description, provider.keyIssueUrl));
        }
        return catalog;
    }

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toUpperCase();
    }

    private void validate(String provider, String label, String plainKey) {
        if (provider == null || provider.isBlank())
            throw new IllegalArgumentException("프로바이더는 필수입니다.");
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("레이블은 필수입니다.");
        if (plainKey == null || plainKey.isBlank())
            throw new IllegalArgumentException("API 키 값은 필수입니다.");
    }

    private record ProviderMeta(String displayName, String description, String keyIssueUrl) {}
}
