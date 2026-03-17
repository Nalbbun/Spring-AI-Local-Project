package ai.local.nalbbun.model.common;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MemorySummary는 계층 간에 전달되는 입력 및 출력 데이터를 표현하는 모델이다.
 * <p>주요 기능: memory summary 관련 책임을 수행한다.</p>
 * <p>입력/출력: 호출부에서 전달된 값이나 상태를 받아 처리 결과, 조회 결과 또는 부수효과를 제공한다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorySummary {
    /** category 값을 보관한다. */
    private ChatCategory category;
    /** summary 값을 보관한다. */
    private String summary;
    /** updatedAt 값을 보관한다. */
    private LocalDateTime updatedAt;
}