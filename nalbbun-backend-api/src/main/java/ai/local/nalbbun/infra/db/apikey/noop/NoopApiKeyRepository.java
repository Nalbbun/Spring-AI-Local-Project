package ai.local.nalbbun.infra.db.apikey.noop;

import ai.local.nalbbun.domain.apikey.repository.ApiKeyRepository;
import ai.local.nalbbun.infra.security.apikey.model.ApiKeyEntry;

import java.util.List;
import java.util.Optional;

/**
 * DB가 없거나 JDBC API 키 저장소를 만들 수 없는 환경에서 사용하는 no-op 저장소다.
 * 조회는 빈 결과를 반환하고, 쓰기 작업은 명시적으로 실패시킨다.
 */
public class NoopApiKeyRepository implements ApiKeyRepository {

    @Override
    public List<ApiKeyEntry> findAll() {
        return List.of();
    }

    @Override
    public List<ApiKeyEntry> findByProvider(String provider) {
        return List.of();
    }

    @Override
    public Optional<ApiKeyEntry> findById(String id) {
        return Optional.empty();
    }

    @Override
    public Optional<ApiKeyEntry> findActiveByProvider(String provider) {
        return Optional.empty();
    }

    @Override
    public ApiKeyEntry save(ApiKeyEntry entry) {
        throw new IllegalStateException("DataSource가 없어 API 키 저장을 사용할 수 없습니다.");
    }

    @Override
    public ApiKeyEntry update(ApiKeyEntry entry) {
        throw new IllegalStateException("DataSource가 없어 API 키 수정을 사용할 수 없습니다.");
    }

    @Override
    public void delete(String id) {
        throw new IllegalStateException("DataSource가 없어 API 키 삭제를 사용할 수 없습니다.");
    }
}
