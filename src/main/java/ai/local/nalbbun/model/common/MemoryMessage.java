package ai.local.nalbbun.model.common;

import ai.local.nalbbun.model.category.ChatCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMessage {
    private String role;              // user / assistant / system
    private String content;
    private ChatCategory category;
    private LocalDateTime createdAt;
}