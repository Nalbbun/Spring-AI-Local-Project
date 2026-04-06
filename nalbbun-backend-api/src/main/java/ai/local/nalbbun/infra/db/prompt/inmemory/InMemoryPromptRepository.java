package ai.local.nalbbun.infra.db.prompt.inmemory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import ai.local.nalbbun.domain.category.model.ChatCategory;
import ai.local.nalbbun.domain.prompt.model.PromptEntry;
import ai.local.nalbbun.domain.prompt.repository.PromptRepository;

/**
 * DB/Redis가 준비되지 않은 환경에서도 화면 진입을 보장하기 위한 인메모리 프롬프트 저장소다.
 */
public class InMemoryPromptRepository implements PromptRepository {

    private final ConcurrentMap<String, PromptEntry> store = new ConcurrentHashMap<>();

    @Override
    public List<PromptEntry> findAll() {
        return store.values().stream()
                .sorted(Comparator
                        .comparing((PromptEntry e) -> e.getCategory() == null ? "ZZZ" : e.getCategory().name())
                        .thenComparing(PromptEntry::isDefault, Comparator.reverseOrder())
                        .thenComparing(PromptEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<PromptEntry> findByCategory(ChatCategory category) {
        if (category == null) {
            return findAll();
        }
        return store.values().stream()
                .filter(e -> e.getCategory() == null || e.getCategory() == category)
                .sorted(Comparator
                        .comparing(PromptEntry::isDefault, Comparator.reverseOrder())
                        .thenComparing(PromptEntry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<PromptEntry> findById(String id) {
        PromptEntry entry = store.get(id);
        return entry == null ? Optional.empty() : Optional.of(copy(entry));
    }

    @Override
    public Optional<PromptEntry> findDefault(ChatCategory category) {
        return store.values().stream()
                .filter(PromptEntry::isDefault)
                .filter(PromptEntry::isActive)
                .filter(e -> category == null ? e.getCategory() == null : e.getCategory() == category)
                .findFirst()
                .map(this::copy);
    }

    @Override
    public PromptEntry save(PromptEntry entry) {
        PromptEntry saved = copy(entry);
        if (saved.getId() == null || saved.getId().isBlank()) {
            saved.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        }
        if (entry.getVersionNo() <= 0) entry.setVersionNo(1);
        LocalDateTime now = LocalDateTime.now();
        saved.setCreatedAt(now);
        saved.setUpdatedAt(now);
        if (saved.isDefault()) {
            clearDefault(saved.getCategory());
        }
        store.put(saved.getId(), copy(saved));
        return saved;
    }

    @Override
    public PromptEntry update(PromptEntry entry) {
        PromptEntry updated = copy(entry);
        updated.setUpdatedAt(LocalDateTime.now());
        if (updated.isDefault()) {
            clearDefault(updated.getCategory());
        }
        store.put(updated.getId(), copy(updated));
        return updated;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public void clearDefault(ChatCategory category) {
        List<String> ids = new ArrayList<>();
        for (PromptEntry entry : store.values()) {
            boolean sameCategory = category == null ? entry.getCategory() == null : entry.getCategory() == category;
            if (sameCategory && entry.isDefault()) {
                PromptEntry copy = copy(entry);
                copy.setDefault(false);
                store.put(copy.getId(), copy);
                ids.add(copy.getId());
            }
        }
    }

    private PromptEntry copy(PromptEntry source) {
        PromptEntry copy = new PromptEntry();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setCategory(source.getCategory());
        copy.setSystemPrompt(source.getSystemPrompt());
        copy.setDescription(source.getDescription());
        copy.setDefault(source.isDefault());
        copy.setActive(source.isActive());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        return copy;
    }
}
