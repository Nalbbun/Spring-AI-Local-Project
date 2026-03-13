package ai.local.nalbbun.category.common.memory;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CategoryMemoryUpdate {
    private String summary;

    @Builder.Default
    private List<String> importantNotes = new ArrayList<>();
}