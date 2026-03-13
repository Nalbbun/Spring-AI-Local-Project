package ai.local.nalbbun.model.common;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemorySummary {
    private ChatCategory category;
    private String summary;
    private LocalDateTime updatedAt;
}