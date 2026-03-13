package ai.local.nalbbun.model.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResolution {
    private ChatCategory category;
    private int confidence;
    private String resolverMode;
    private String reason;
}