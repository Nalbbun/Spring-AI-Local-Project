package ai.local.nalbbun.prompt.repository;

import ai.local.nalbbun.category.model.ChatCategory;
import ai.local.nalbbun.prompt.model.PromptEntry;

import java.util.List;
import java.util.Optional;

/**
 * 프롬프트 저장소 인터페이스.
 * JDBC / Redis 구현체가 조건부로 등록됩니다.
 */
public interface PromptRepository {

    /** 전체 프롬프트 목록 (active 여부 무관) */
    List<PromptEntry> findAll();

    /** 카테고리로 필터 (null = 전체) */
    List<PromptEntry> findByCategory(ChatCategory category);

    /** ID로 단건 조회 */
    Optional<PromptEntry> findById(String id);

    /** 카테고리 기본 프롬프트 조회 */
    Optional<PromptEntry> findDefault(ChatCategory category);

    /** 저장 (신규 생성) */
    PromptEntry save(PromptEntry entry);

    /** 수정 */
    PromptEntry update(PromptEntry entry);

    /** 삭제 */
    void delete(String id);

    /** 특정 카테고리의 기본 프롬프트 해제 */
    void clearDefault(ChatCategory category);
}
