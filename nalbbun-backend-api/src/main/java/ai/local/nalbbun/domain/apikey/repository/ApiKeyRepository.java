package ai.local.nalbbun.domain.apikey.repository;

import java.util.List;
import java.util.Optional;

import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;

public interface ApiKeyRepository {
    List<ApiKeyEntry> findAll();
    List<ApiKeyEntry> findByProvider(String provider);
    Optional<ApiKeyEntry> findById(String id);
    Optional<ApiKeyEntry> findActiveByProvider(String provider);
    ApiKeyEntry save(ApiKeyEntry entry);
    ApiKeyEntry update(ApiKeyEntry entry);
    void delete(String id);
}
