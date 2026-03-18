package ai.local.nalbbun.prompt.model;

import ai.local.nalbbun.category.model.ChatCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 저장된 프롬프트 엔트리.
 * JDBC는 prompt_entry 테이블, Redis는 prompt:{id} 키로 관리합니다.
 *
 * 주의: boolean isDefault 는 Lombok @Data 사용 시 Jackson이 "default" 로 직렬화하는 문제가 있어
 * @Getter/@Setter 를 명시하고 isDefault 필드에 @JsonProperty("isDefault") 를 적용합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptEntry {

    /** 고유 ID */
    private String id;

    /** 프롬프트 이름 */
    private String name;

    /** 적용 대상 카테고리. null 이면 모든 카테고리 공통 */
    private ChatCategory category;

    /** 시스템 프롬프트 본문 */
    private String systemPrompt;

    /** 설명 (선택) */
    private String description;

    /**
     * 기본 프롬프트 여부.
     * boolean isXxx → Lombok 은 isXxx() getter, setXxx() setter 를 생성하므로
     * Jackson 이 "xxx" 로 직렬화합니다. @JsonProperty 로 "isDefault" 를 명시합니다.
     */
    @JsonProperty("isDefault")
    private boolean isDefault;

    /** 활성화 여부 */
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
